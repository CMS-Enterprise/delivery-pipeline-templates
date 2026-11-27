variable "region" {
  description = "AWS region for the cluster."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name, used in resource names and tags."
  type        = string
}

variable "name_prefix" {
  description = "Prefix for resource names; the cluster is named <name_prefix>-<environment>."
  type        = string
  default     = "robocare-demo"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes minor version, e.g. \"1.35\". Upgrades are one minor version at a time."
  type        = string
  default     = "1.35"
}

variable "vpc_id" {
  description = "ID of the existing VPC the cluster runs in."
  type        = string

  validation {
    condition     = can(regex("^vpc-[0-9a-f]+$", var.vpc_id))
    error_message = "vpc_id must be a VPC ID (vpc-...)."
  }
}

variable "private_subnet_ids" {
  description = "Existing private subnets, in at least two AZs, for the control plane ENIs, Fargate pods and internal ALBs. They need a route to artifactory.cloud.cms.gov for image pulls."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2 && alltrue([for s in var.private_subnet_ids : can(regex("^subnet-[0-9a-f]+$", s))])
    error_message = "private_subnet_ids must list at least two subnet IDs (subnet-...)."
  }
}

variable "tag_subnets_for_internal_alb" {
  description = "Tag private_subnet_ids with kubernetes.io/role/internal-elb=1 so the load balancer controller discovers them. Leave false if the VPC owner already tags them: destroy removes tags Terraform added."
  type        = bool
  default     = false
}

variable "endpoint_public_access" {
  description = "Expose the Kubernetes API on a public endpoint as well as the private one."
  type        = bool
  default     = false
}

variable "public_access_cidrs" {
  description = "CIDRs allowed to reach the public API endpoint; only used when endpoint_public_access is true."
  type        = list(string)
  default     = []
}

variable "admin_principal_arns" {
  description = "IAM role/user ARNs granted cluster admin through EKS access entries, in addition to the principal that creates the cluster."
  type        = list(string)
  default     = []
}

variable "app_namespaces" {
  description = "Namespaces the demo workloads run in; each gets a Fargate profile selector. At most 5."
  type        = list(string)
  default     = ["delivery-demos-dev"]

  validation {
    condition     = length(var.app_namespaces) >= 1 && length(var.app_namespaces) <= 5
    error_message = "A Fargate profile takes between 1 and 5 selectors."
  }
}

variable "coredns_addon_version" {
  description = "Exact CoreDNS add-on version (aws eks describe-addon-versions --addon-name coredns). null takes the default for kubernetes_version at create time."
  type        = string
  default     = null
}

variable "log_retention_days" {
  description = "Days to keep control plane and Fargate pod logs in CloudWatch."
  type        = number
  default     = 90
}
