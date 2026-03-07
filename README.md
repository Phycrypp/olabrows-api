# 🌸 Browed by Olá — REST API

> Production-grade Spring Boot REST API powering [olabrows.store](https://olabrows.store), deployed on AWS EC2 with MySQL RDS and JWT security.

🔗 **Live API:** [api.olabrows.store](https://api.olabrows.store)

## Tech Stack
- **Framework:** Spring Boot 3.3.0 / Java 21
- **Database:** MySQL 8.0 (AWS RDS)
- **Security:** Spring Security + JWT
- **Email:** AWS SES
- **Hosting:** AWS EC2 (Amazon Linux 2023)
- **Build:** Maven

## API Endpoints
- POST /api/auth/login — Get JWT token
- GET/POST/PUT/DELETE /api/products — Product catalogue
- GET/POST/PUT /api/orders — Order management
- GET/POST /api/subscribers — Email subscribers
- POST /api/subscribers/import — Bulk CSV import
- GET/POST /api/hr/employees — Employee records
- POST /api/email/broadcast — Email all subscribers

## Related Repositories
- [ola-brows](https://github.com/Phycrypp/ola-brows) — Frontend + CI/CD
- [ola-terraform](https://github.com/Phycrypp/ola-terraform) — Infrastructure as Code
