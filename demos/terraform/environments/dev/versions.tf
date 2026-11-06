terraform {
  required_version = "~> 1.4.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "delivery-pipeline-demos"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
