variable "region" {
  description = "AWS region for the demo resources."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name, used in resource names and tags."
  type        = string
}

variable "name_prefix" {
  description = "Prefix for resource names; must be globally unique for S3."
  type        = string
  default     = "robocare-demo"
}

variable "noncurrent_version_days" {
  description = "Days to keep superseded object versions before expiry."
  type        = number
  default     = 30
}
