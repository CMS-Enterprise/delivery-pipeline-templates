output "bucket_name" {
  description = "Name of the demo artifact bucket."
  value       = aws_s3_bucket.artifacts.bucket
}

output "kms_key_arn" {
  description = "ARN of the KMS key encrypting the bucket."
  value       = aws_kms_key.artifacts.arn
}
