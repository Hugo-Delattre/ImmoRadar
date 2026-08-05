output "api_gateway_endpoint" {
  description = "URL publique de l'API Gateway (Backend Spring Boot)"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "cloudfront_domain_name" {
  description = "URL publique CloudFront du frontend Angular"
  value       = "https://${aws_cloudfront_distribution.frontend_cdn.domain_name}"
}

output "rds_endpoint" {
  description = "Point de terminaison de la base PostgreSQL"
  value       = aws_db_instance.postgres.endpoint
}

output "dossiers_bucket_name" {
  description = "Nom du bucket S3 des dossiers bancaires"
  value       = aws_s3_bucket.dossiers.id
}
