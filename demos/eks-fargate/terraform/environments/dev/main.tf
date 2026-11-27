data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

locals {
  cluster_name = "${var.name_prefix}-${var.environment}"
  account_id   = data.aws_caller_identity.current.account_id
  partition    = data.aws_partition.current.partition

  # Log groups are named ahead of the cluster so the KMS key policy can scope
  # the CloudWatch Logs grant to them without a dependency cycle.
  cluster_log_group = "/aws/eks/${local.cluster_name}/cluster"
  fargate_log_group = "/aws/eks/${local.cluster_name}/fargate"
}

# ─── Encryption ───
#
# One key encrypts Kubernetes Secrets (envelope encryption) and both log groups.
# CloudWatch Logs needs a key-policy grant of its own, so the policy is written
# out rather than left at the default.

data "aws_iam_policy_document" "kms" {
  statement {
    sid       = "AccountAdministration"
    actions   = ["kms:*"]
    resources = ["*"]

    principals {
      type        = "AWS"
      identifiers = ["arn:${local.partition}:iam::${local.account_id}:root"]
    }
  }

  statement {
    sid = "CloudWatchLogs"
    actions = [
      "kms:Encrypt*",
      "kms:Decrypt*",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:Describe*",
    ]
    resources = ["*"]

    principals {
      type        = "Service"
      identifiers = ["logs.${var.region}.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "kms:EncryptionContext:aws:logs:arn"
      values   = ["arn:${local.partition}:logs:${var.region}:${local.account_id}:log-group:/aws/eks/${local.cluster_name}/*"]
    }
  }
}

resource "aws_kms_key" "eks" {
  description             = "Encrypts ${local.cluster_name} Kubernetes Secrets and logs"
  deletion_window_in_days = 7
  enable_key_rotation     = true
  policy                  = data.aws_iam_policy_document.kms.json
}

resource "aws_kms_alias" "eks" {
  name          = "alias/${local.cluster_name}-eks"
  target_key_id = aws_kms_key.eks.key_id
}

# ─── Logging ───
#
# The control plane log group must exist before the cluster, or EKS creates it
# itself without retention or encryption.

resource "aws_cloudwatch_log_group" "cluster" {
  name              = local.cluster_log_group
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.eks.arn
}

# Fargate pod stdout/stderr, shipped by the Fargate log router as configured in
# manifests/clusters/dev/aws-observability.yaml.
resource "aws_cloudwatch_log_group" "fargate" {
  name              = local.fargate_log_group
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.eks.arn
}

# ─── Cluster IAM role ───

data "aws_iam_policy_document" "cluster_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster" {
  name               = "${local.cluster_name}-cluster"
  assume_role_policy = data.aws_iam_policy_document.cluster_assume.json
}

resource "aws_iam_role_policy_attachment" "cluster" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:${local.partition}:iam::aws:policy/AmazonEKSClusterPolicy"
}

# ─── Cluster ───

resource "aws_eks_cluster" "this" {
  name     = local.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

  # Access entries only (no aws-auth ConfigMap). The principal running
  # terraform apply, i.e. the role awsAssumeRole assumed, becomes cluster admin.
  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = true
  }

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = var.endpoint_public_access
    public_access_cidrs     = var.endpoint_public_access ? var.public_access_cidrs : null
  }

  encryption_config {
    resources = ["secrets"]

    provider {
      key_arn = aws_kms_key.eks.arn
    }
  }

  # Standard support only: extended support bills per cluster-hour and hides
  # a version that has fallen out of date.
  upgrade_policy {
    support_type = "STANDARD"
  }

  timeouts {
    create = "30m"
    update = "60m"
    delete = "15m"
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster,
    aws_cloudwatch_log_group.cluster,
  ]
}

# ─── Cluster access ───

resource "aws_eks_access_entry" "admin" {
  for_each = toset(var.admin_principal_arns)

  cluster_name  = aws_eks_cluster.this.name
  principal_arn = each.value
}

resource "aws_eks_access_policy_association" "admin" {
  for_each = toset(var.admin_principal_arns)

  cluster_name  = aws_eks_cluster.this.name
  principal_arn = aws_eks_access_entry.admin[each.value].principal_arn
  policy_arn    = "arn:${local.partition}:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}

# ─── CoreDNS on Fargate ───
#
# The self-managed CoreDNS that EKS bootstraps is pinned to EC2 nodes, so on a
# Fargate-only cluster it never schedules. The managed add-on replaces it and,
# with computeType Fargate, runs in the kube-system Fargate profile. vpc-cni and
# kube-proxy are not needed: Fargate provides pod networking and service
# routing itself, and their DaemonSets schedule no pods here.

resource "aws_eks_addon" "coredns" {
  cluster_name  = aws_eks_cluster.this.name
  addon_name    = "coredns"
  addon_version = var.coredns_addon_version

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  configuration_values = jsonencode({
    computeType = "Fargate"
    resources = {
      requests = { cpu = "0.25", memory = "256M" }
      limits   = { cpu = "0.25", memory = "256M" }
    }
  })

  timeouts {
    create = "20m"
    update = "20m"
    delete = "10m"
  }

  # The add-on only reports ACTIVE once its pods run, which needs the profile.
  depends_on = [aws_eks_fargate_profile.system]
}

# ─── ALB subnet discovery ───

resource "aws_ec2_tag" "internal_elb" {
  for_each = var.tag_subnets_for_internal_alb ? toset(var.private_subnet_ids) : toset([])

  resource_id = each.value
  key         = "kubernetes.io/role/internal-elb"
  value       = "1"
}
