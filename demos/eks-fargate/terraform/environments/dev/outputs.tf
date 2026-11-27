output "cluster_name" {
  description = "Name of the EKS cluster."
  value       = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  description = "Kubernetes API endpoint."
  value       = aws_eks_cluster.this.endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64 CA bundle for the Kubernetes API, for kubeconfig."
  value       = aws_eks_cluster.this.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "EKS-managed security group attached to the control plane and every Fargate pod."
  value       = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

output "vpc_id" {
  description = "VPC of the cluster; the load balancer controller needs it as vpcId because Fargate pods have no instance metadata."
  value       = var.vpc_id
}

output "oidc_provider_arn" {
  description = "IAM OIDC provider for IRSA."
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "lb_controller_role_arn" {
  description = "IRSA role for the aws-load-balancer-controller service account in kube-system."
  value       = aws_iam_role.lb_controller.arn
}

output "fargate_pod_execution_role_arn" {
  description = "Pod execution role shared by the Fargate profiles."
  value       = aws_iam_role.fargate_pod_execution.arn
}

output "fargate_log_group_name" {
  description = "CloudWatch log group for pod logs; must match manifests/clusters/dev/aws-observability.yaml."
  value       = aws_cloudwatch_log_group.fargate.name
}

output "kms_key_arn" {
  description = "ARN of the KMS key encrypting Secrets and logs."
  value       = aws_kms_key.eks.arn
}
