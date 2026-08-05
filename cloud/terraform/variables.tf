variable "aws_region" {
  description = "Région AWS de déploiement"
  type        = string
  default     = "eu-west-3" # Paris
}

variable "environment" {
  description = "Nom de l'environnement (ex: dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "app_name" {
  description = "Nom de l'application"
  type        = string
  default     = "immoradar"
}

variable "db_username" {
  description = "Nom d'utilisateur administrateur de la base RDS PostgreSQL"
  type        = string
  default     = "immoradar_admin"
  sensitive   = true
}

variable "db_password" {
  description = "Mot de passe administrateur de la base RDS PostgreSQL"
  type        = string
  sensitive   = true
  default     = "ImmoRadarSecurePassword2026!"
}

variable "db_name" {
  description = "Nom de la base de données PostgreSQL"
  type        = string
  default     = "immoradar"
}
