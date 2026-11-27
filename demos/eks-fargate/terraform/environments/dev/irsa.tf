# ─── IRSA ───
#
# EKS Pod Identity is not available to Fargate pods, so in-cluster controllers
# get AWS credentials through IAM Roles for Service Accounts. AWS validates the
# EKS OIDC issuer itself, so no thumbprint is pinned.

locals {
  oidc_issuer = trimprefix(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://")

  lb_controller_namespace       = "kube-system"
  lb_controller_service_account = "aws-load-balancer-controller"
}

resource "aws_iam_openid_connect_provider" "eks" {
  url            = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list = ["sts.amazonaws.com"]
}

# ─── AWS Load Balancer Controller ───
#
# The controller itself is installed through gitops (see the README); Terraform
# owns only its IAM role. policies/aws-load-balancer-controller.json is the
# upstream docs/install/iam_policy.json at v3.6.3 re-indented to 2 spaces
# (upstream file sha256 16f232c9d9f79366fe949c4550ad517a202380058a9e48d45a4e215044a20a6a).
# Replace it whenever the controller version changes: v3.7.0 adds
# ec2:ValidateSecurityGroupQuotasForInterface.

data "aws_iam_policy_document" "lb_controller_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.eks.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer}:sub"
      values   = ["system:serviceaccount:${local.lb_controller_namespace}:${local.lb_controller_service_account}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lb_controller" {
  name               = "${local.cluster_name}-aws-load-balancer-controller"
  assume_role_policy = data.aws_iam_policy_document.lb_controller_assume.json
}

#trivy:ignore:AVD-AWS-0057 Upstream controller policy; ELB/EC2 resources are created at run time, so most actions cannot be scoped to ARNs.
resource "aws_iam_policy" "lb_controller" {
  name        = "${local.cluster_name}-aws-load-balancer-controller"
  description = "AWS Load Balancer Controller v3.6.3 for ${local.cluster_name}"
  policy      = file("${path.module}/policies/aws-load-balancer-controller.json")
}

resource "aws_iam_role_policy_attachment" "lb_controller" {
  role       = aws_iam_role.lb_controller.name
  policy_arn = aws_iam_policy.lb_controller.arn
}
