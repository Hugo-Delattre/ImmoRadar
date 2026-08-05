# --- S3 BUCKET POUR DOSSIERS BANCAIRES PDF ---
resource "aws_s3_bucket" "dossiers" {
  bucket        = "${var.app_name}-${var.environment}-dossiers-bancaires"
  force_destroy = false

  tags = {
    Name = "${var.app_name}-${var.environment}-dossiers"
  }
}

resource "aws_s3_bucket_public_access_block" "dossiers_block" {
  bucket = aws_s3_bucket.dossiers.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "dossiers_cors" {
  bucket = aws_s3_bucket.dossiers.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["*"]
    max_age_seconds = 3600
  }
}
