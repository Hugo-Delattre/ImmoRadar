# --- RÔLE IAM POUR LA LAMBDA ---
resource "aws_iam_role" "lambda_exec" {
  name = "${var.app_name}-${var.environment}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_vpc" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy" "lambda_s3_access" {
  name = "${var.app_name}-${var.environment}-lambda-s3"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.dossiers.arn}/*"
      }
    ]
  })
}

# --- LAMBDA CONTAINER IMAGE (Spring Boot 4.1 Native GraalVM) ---
resource "aws_lambda_function" "api" {
  function_name = "${var.app_name}-${var.environment}-api"
  role          = aws_iam_role.lambda_exec.arn
  package_type  = "Image"
  image_uri     = "123456789012.dkr.ecr.${var.aws_region}.amazonaws.com/${var.app_name}:latest" # À remplacer par le tag ECR réel
  timeout       = 30
  memory_size   = 512

  vpc_config {
    subnet_ids         = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE              = var.environment
      SPRING_DATASOURCE_URL               = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${var.db_name}"
      SPRING_DATASOURCE_USERNAME          = var.db_username
      SPRING_DATASOURCE_PASSWORD          = var.db_password
      SPRING_DATASOURCE_DRIVER_CLASS_NAME = "org.postgresql.Driver"
      SPRING_JPA_DATABASE_PLATFORM        = "org.hibernate.dialect.PostgreSQLDialect"
      SPRING_JPA_HIBERNATE_DDL_AUTO       = "update"
      S3_DOSSIERS_BUCKET                  = aws_s3_bucket.dossiers.id
    }
  }

  tags = {
    Name = "${var.app_name}-${var.environment}-api"
  }
}
