# ☁️ ImmoRadar — Infrastructure Cloud & Déploiement AWS (Terraform)

Ce dossier contient l'ensemble de l'**Infrastructure as Code (IaC)** permettant de déployer la solution **ImmoRadar** sur **Amazon Web Services (AWS)** en architecture 100% Serverless & Cloud-Native.

---

## 🏛️ Composants Déployés

| Service AWS | Rôle dans ImmoRadar |
| :--- | :--- |
| **AWS Lambda** | Exécution du binaire natif **Spring Boot 4.1 GraalVM** (démarrage en < 50ms, facturation à la milliseconde). |
| **Amazon API Gateway (v2)** | Point d'entrée HTTP public avec gestion de CORS et routage vers la Lambda. |
| **Amazon RDS (PostgreSQL 16)** | Base de données relationnelle managée, sécurisée dans des sous-réseaux privés VPC. |
| **Amazon S3 (Dossiers)** | Stockage sécurisé des dossiers bancaires PDF générés pour les utilisateurs. |
| **Amazon S3 (Frontend)** | Hébergement statique de l'application Angular 22 compilée. |
| **Amazon CloudFront** | CDN mondial assurant la terminaison HTTPS, la mise en cache et le routage SPA. |
| **Amazon VPC** | Isolation réseau avec sous-réseaux publics (NAT/GW) et privés (RDS & Lambda). |

---

## 🚀 Guide de Déploiement

### 1. Prérequis
- [Terraform](https://developer.hashicorp.com/terraform/downloads) >= 1.5.0 installé
- [AWS CLI](https://aws.amazon.com/cli/) configuré (`aws configure`) avec des identifiants valides
- Docker pour la compilation de l'image conteneur Spring Boot GraalVM

### 2. Initialiser Terraform
```bash
cd cloud/terraform
terraform init
```

### 3. Valider le Plan d'Exécution
```bash
terraform plan
```

### 4. Déployer l'Infrastructure
```bash
terraform apply -auto-approve
```

### 5. Récupérer les URLs de Production
À la fin du déploiement, Terraform affichera :
- `api_gateway_endpoint` : L'URL de l'API backend Spring Boot
- `cloudfront_domain_name` : L'URL publique de l'application Angular (HTTPS)
