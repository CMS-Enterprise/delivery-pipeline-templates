# ─── Fargate pod execution role ───
#
# Used by the Fargate agent, not by the pods' own code: it registers the pod with
# the cluster and ships its logs. Images come from Artifactory with an
# imagePullSecret, so the ECR permissions in the managed policy go unused.

data "aws_iam_policy_document" "fargate_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks-fargate-pods.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["arn:${local.partition}:eks:${var.region}:${local.account_id}:fargateprofile/${local.cluster_name}/*"]
    }
  }
}

resource "aws_iam_role" "fargate_pod_execution" {
  name               = "${local.cluster_name}-fargate-pod-execution"
  assume_role_policy = data.aws_iam_policy_document.fargate_assume.json
}

resource "aws_iam_role_policy_attachment" "fargate_pod_execution" {
  role       = aws_iam_role.fargate_pod_execution.name
  policy_arn = "arn:${local.partition}:iam::aws:policy/AmazonEKSFargatePodExecutionRolePolicy"
}

data "aws_iam_policy_document" "fargate_logging" {
  statement {
    sid = "FargateLogRouter"
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents",
    ]
    resources = [
      aws_cloudwatch_log_group.fargate.arn,
      "${aws_cloudwatch_log_group.fargate.arn}:*",
    ]
  }
}

resource "aws_iam_role_policy" "fargate_logging" {
  name   = "fargate-logging"
  role   = aws_iam_role.fargate_pod_execution.id
  policy = data.aws_iam_policy_document.fargate_logging.json
}

# ─── Fargate profiles ───
#
# EKS creates or deletes one Fargate profile at a time per cluster, so apps
# depends on system to keep Terraform from issuing both calls at once.

resource "aws_eks_fargate_profile" "system" {
  cluster_name           = aws_eks_cluster.this.name
  fargate_profile_name   = "system"
  pod_execution_role_arn = aws_iam_role.fargate_pod_execution.arn
  subnet_ids             = var.private_subnet_ids

  # CoreDNS and the AWS Load Balancer Controller.
  selector {
    namespace = "kube-system"
  }

  # The Flux controllers.
  selector {
    namespace = "flux-system"
  }

  timeouts {
    create = "20m"
    delete = "20m"
  }

  depends_on = [
    aws_iam_role_policy_attachment.fargate_pod_execution,
    aws_iam_role_policy.fargate_logging,
  ]
}

resource "aws_eks_fargate_profile" "apps" {
  cluster_name           = aws_eks_cluster.this.name
  fargate_profile_name   = "apps"
  pod_execution_role_arn = aws_iam_role.fargate_pod_execution.arn
  subnet_ids             = var.private_subnet_ids

  dynamic "selector" {
    for_each = toset(var.app_namespaces)

    content {
      namespace = selector.value
    }
  }

  timeouts {
    create = "20m"
    delete = "20m"
  }

  depends_on = [aws_eks_fargate_profile.system]
}
