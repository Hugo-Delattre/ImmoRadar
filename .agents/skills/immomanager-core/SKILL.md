# Skill: ImmoManager Core Rules

## Context
Project: ImmoManager (Real Estate Deal Finder & Tenant CRM)
Frontend: Angular v22 (Zoneless, Signals, Standalone Components, Signal Forms)
Backend: Spring Boot 4.1+, Java 25, Maven, GraalVM Native AOT, AWS Serverless Container

## Architectural Constraints

### 1. Frontend
- **Code Generation:** ALWAYS use the Angular CLI to generate components, services, directives, and pipes. Use Angular v22 and refers to angular-developer skills for best practices.
- **Build Check:** Once you finish generating code, run `ng build` to ensure there are no build errors. If there are errors, fix them before proceeding.
- **Styling:** Use Tailwind CSS. To initialize it, run `npx ng add tailwindcss`.
- **Reactivity & State:** Use `signal()`, `computed()`, and `effect()` for state management. Ensure change detection relies strictly on Angular Signals (`provideZonelessChangeDetection()`).
- **Forms:** Since this is a new application using v22, ALWAYS prefer signal forms.
- **Testing:** Use **Vitest** for unit testing components and services. Use **Playwright** for End-to-End (E2E) testing.

### 2. Backend (Spring Boot 4.1 & Java 25)
- **Architecture:** Target architecture is AWS Lambda (Fat Lambda). Keep memory footprint low.
- **Compilation:** All code must be compatible with GraalVM Native AOT compilation to ensure sub-100ms startup times.
- **Database:** Use Spring Data JPA with PostgreSQL (AWS RDS).
- **Database Migrations:** Use **Flyway** for schema migrations. JPA `ddl-auto` must be set to `validate` in production. Every schema change must have a corresponding Flyway SQL script.
- **Testing:** Use **JUnit 5** and **Mockito** for unit testing. Use **Testcontainers** for integration testing against a real PostgreSQL database. Do not use H2 for integration tests.
- **Security:** Use **AWS Cognito** for authentication and authorization (JWT validation via Spring Security).
- **AI Integration:** Expose core business logic using `@McpTool` and `@McpResource` annotations for Antigravity integration.

### 3. Infrastructure & Deployment
- **IaC (Infrastructure as Code):** Use **Terraform** to provision the infrastructure (Lambda, RDS, Cognito, API Gateway).

### 4. Business Domain
- **Deal Finder Module:** Scraping, cash-flow calculation, yield analysis.
- **CRM Module:** Tenant management, rent tracking, lease contracts documents.