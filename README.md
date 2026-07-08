# 🏢 ImmoManager — Smart Real Estate Deal Finder & Investment Simulator

[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular 22](https://img.shields.io/badge/Angular-22.0.0-red.svg)](https://angular.dev/)
[![TailwindCSS 4](https://img.shields.io/badge/TailwindCSS-v4.1-blue.svg)](https://tailwindcss.com/)
[![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://docs.docker.com/compose/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **ImmoManager** est une plateforme SaaS d'analyse d'opportunités et d'optimisation d'investissements locatifs. Elle permet aux investisseurs particuliers, chasseurs immobiliers et conseillers en gestion de patrimoine de filtrer des opportunités réelles à haut rendement, de comparer les prix du marché avec les données officielles de l'État (**API DVF**), de simuler l'impact fiscal (LMNP Réel, Micro-BIC, SCI IS, Nu), et de générer un **dossier bancaire PDF complet** prêt à présenter aux établissements de crédit.

---

## 🏛️ Architecture & Conception Système

```mermaid
flowchart TD
    %% Custom Styling
    classDef front fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#065f46;
    classDef security fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#581c87;
    classDef service fill:#e0f2fe,stroke:#0284c7,stroke-width:2px,color:#0369a1;
    classDef database fill:#d1fae5,stroke:#059669,stroke-width:2px,color:#065f46;
    classDef external fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#92400e;
    classDef asyncQueue fill:#ffedd5,stroke:#ea580c,stroke-width:2px,color:#9a3412;
    classDef storage fill:#ede9fe,stroke:#7c3aed,stroke-width:2px,color:#5b21b6;

    %% Nodes Definitions
    Frontend(("Angular 22 SPA\n(Signals & Resource API)")):::front
    AuthService["Spring Security / OAuth2\nJWT Token Provider"]:::security
    ApiGateway["REST Controller Layer\n(RFC 7807 ProblemDetails)"]:::service
    DealService["Deal & Simulator Engine\n(Spring Data JPA Specs)"]:::service
    DvfService["DVF Market Data Service\n(Data Gouv Integration)"]:::service
    PdfService["Banking Dossier Generator\n(OpenPDF / HTML to PDF)"]:::service

    DataGov[("API DVF (data.gouv.fr)\nTransactions Réelles")]:::external
    Postgres[("PostgreSQL\n(Deals & Simulations)")]:::database
    RedisCache[("Redis Cache\n(Prix m² & Rate Limiting)")]:::database
    AsyncTasks(("Task Queue / @Async\n(Dossier Bancaire Engine)")):::asyncQueue
    S3Storage[("AWS S3 Bucket\n(PDF Dossiers Téléchargeables)")]:::storage

    %% Connections
    Frontend -->|"Authentification & Refresh"| AuthService
    Frontend -->|"Requêtes REST + Bearer JWT"| ApiGateway
    ApiGateway -.->|"Validation JWT"| AuthService
    ApiGateway -->|"Filtres & Simulations"| DealService
    ApiGateway -->|"Génération Dossier PDF"| PdfService

    DealService -->|"Persistance & Filtres SQL"| Postgres
    DealService -->|"Cache prix & quotas"| RedisCache
    DealService -->|"Comparatif prix marché"| DvfService
    DvfService -->|"Historique ventes 5 ans"| DataGov

    PdfService -->|"Job asynchrone"| AsyncTasks
    AsyncTasks -->|"Upload PDF généré"| S3Storage
    AsyncTasks -.->|"Lien de téléchargement sécurisé"| Frontend
```

> 📌 *Une version statique haute résolution du schéma est également disponible dans le dossier de documentation :*  
> `docs/immo_schema.png`

---

## ⚡ Démarrage Rapide (1 seule commande avec Docker)

Le projet est entièrement conteneurisé. Pour démarrer la base PostgreSQL, l'API Spring Boot et l'application frontend Angular en une seule commande :

```bash
docker compose up --build
```

Une fois les conteneurs démarrés :
- 🌐 **Frontend (Angular)** : [http://localhost:4200](http://localhost:4200)
- ⚙️ **Backend API (Spring Boot)** : [http://localhost:8080/api/deals](http://localhost:8080/api/deals)
- 🗄️ **PostgreSQL** : `localhost:5432` (Base : `immomanager`, User : `immomanager`, Mot de passe : `immomanager_secret`)

Pour arrêter l'ensemble :
```bash
docker compose down
```

---

## 🛠️ Stack Technique

### Backend
- **Langage & Runtime** : Java 25 & Spring Boot 4.1.0
- **Persistance** : Spring Data JPA, PostgreSQL (production/docker) & SQLite (dev local autonome)
- **Architecture** : Clean Architecture, DTOs sous forme de `record` Java 25, RFC 7807 `ProblemDetail`
- **Cloud Ready** : Plugin GraalVM Native Image pour un démarrage ultra-rapide (< 50ms) et AWS Serverless Container

### Frontend
- **Framework** : Angular 22 (Standalone components, Signals, Signal Forms, Resource API)
- **Styling** : Tailwind CSS v4 & SCSS
- **Qualité & Tests** : Vitest (unitaires) & Playwright (E2E)

---

## 💻 Développement Local Sans Docker (Optionnel)

### 1. Démarrer le Backend
```bash
cd backend
./mvnw spring-boot:run
```
*(Utilise par défaut SQLite en local, aucun prérequis de base de données à installer).*

### 2. Démarrer le Frontend
```bash
cd frontend
npm install
npm start
```
L'application se lance sur `http://localhost:4200` avec proxy automatique des appels `/api` vers le backend.

---

## 📑 Spécifications Produit & Roadmap

Retrouvez le cahier des charges complet, l'analyse de valeur SaaS et la grille d'évaluation pour recruteurs dans le document dédié :

👉 **[Consulter le PRD (Product Requirements Document)](docs/PRD.md)**

Le PRD inclut des **cases à cocher dynamiques** pour suivre l'avancement des 3 phases du projet :
- [ ] **Phase 1** : Rigueur Architecturale, Moteur Fiscal Réel & API DVF de l'État
- [ ] **Phase 2** : Générateur de Dossier Bancaire PDF & Data Visualisation interactive
- [ ] **Phase 3** : Sécurité JWT, Multi-tenancy, Infrastructure as Code (AWS/Terraform) & CI/CD
