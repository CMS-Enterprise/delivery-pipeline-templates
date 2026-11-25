output "bucket_name" {
  description = "Name of the demo artifact bucket."
  value       = aws_s3_bucket.artifacts.bucket
}

output "kms_key_arn" {
  description = "ARN of the KMS key encrypting the bucket."
  value       = aws_kms_key.artifacts.arn
}

output "access_log_bucket_name" {
  description = "Name of the bucket receiving the artifact bucket's access logs."
  value       = aws_s3_bucket.access_logs.bucket
}
