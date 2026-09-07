# net2rent-backend

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-blue)
![Status](https://img.shields.io/badge/status-in%20development-yellow)

Backend API for the Incident Management App — registration, triage, execution, and closure of incidents reported on rental properties. Two entry points: guests identify with a lodging reference + 4-digit PIN and report issues themselves; staff (coordinators/admins) log incidents called in by phone and manage them through their full lifecycle.

**Frontend repository:** [net2rent-frontend](https://github.com/Net-2-Rent/net2rent-frontend)

## Project Status

Implemented: account/user/lodging management, strict multi-tenant isolation (every query scoped to the authenticated user's account), staff JWT auth, guest access via reference + PIN, guest incident registration, incident triage (classification, urgency), checklist, comments, and history.

Pending: the full incident lifecycle actions (assign, start, pause, resume, resolve, reject, close) are not implemented yet — this is the current priority. Track progress against `PRD-MVP-Incidencias.md`.

## Tech Stack

- Java 25
- Spring Boot 4.1.1 (Web, Data JPA, Security)
- Hibernate ORM
- PostgreSQL
- Maven (wrapper included, `./mvnw`)
- JWT authentication for staff; short-lived, independent guest tokens
- springdoc-openapi (Swagger UI)

## Project Structure
```
src/main/java/com/net2rent/net2rent_backend/
├── config/ # CORS, JWT & guest-token properties, time config
├── controller/ # REST endpoints (Auth, GuestAuth, GuestIncident, Incident, Lodging, User)
├── dto/ # Request/response payloads
├── exception/ # Global exception handling
├── model/ # JPA entities
├── repository/ # Spring Data repositories
├── security/ # Security config and JWT / guest-token filters
└── service/ # Business logic
```


## Prerequisites

- JDK 25
- PostgreSQL running locally
- Git

## Environment Variables

Copy `.env.example` to `.env` at the repo root (not versioned) and fill in your local values:

| Variable | Description |
| :--- | :--- |
| `DB_URL` | JDBC URL of your local PostgreSQL database, e.g. `jdbc:postgresql://localhost:5432/net2rent` |
| `DB_USERNAME` | Local PostgreSQL user |
| `DB_PASSWORD` | Local PostgreSQL password |
| `JWT_SECRET` | Random string used to sign staff JWTs |
| `GUEST_TOKEN_SECRET` | Random, Base64-encoded string used to sign guest tokens |

These are local to your machine — they don't need to match your teammates'. A token signed on one backend is not valid on another.

## Local Setup

1. Clone the repo and switch to your branch:
```bash
   git clone git@github.com:Net-2-Rent/net2rent-backend.git
   cd net2rent-backend
   git checkout <your-branch>
```
2. Create a database named `net2rent` in your local PostgreSQL.
3. Run the contents of `db/schema.sql` against that database. This creates all the tables — the backend does **not** generate the schema automatically (`spring.jpa.hibernate.ddl-auto=validate`). Any schema change on a branch (new columns, new tables) must be applied here manually; there is no migration tool yet.
4. Create your `.env` file (see Environment Variables above).
5. Start the backend:
```bash
   ./mvnw spring-boot:run
```
On every startup, Spring runs `src/main/resources/data.sql`, which seeds accounts, users, and sample data (`INSERT ... ON CONFLICT DO NOTHING`, safe to re-run). It must **never** contain schema changes — those go in `db/schema.sql`, applied manually.

## Scripts

| Command | Purpose |
| :--- | :--- |
| `./mvnw spring-boot:run` | Run the API locally |
| `./mvnw test` | Run the test suite |
| `./mvnw clean package` | Build a runnable JAR |

## API Documentation

With the app running locally, the interactive API docs are at `http://localhost:8080/swagger-ui/index.html` (raw OpenAPI spec at `/v3/api-docs`).

## Related Documentation

- [`PRD-MVP-Incidencias.md`](./PRD-MVP-Incidencias.md) — what needs to be built and the acceptance criteria.
- [`VOCABULARIO.md`](./VOCABULARIO.md) — domain naming conventions.

## Notes for the Team

- Any `ALTER TABLE` or other schema change must be announced to the team and applied manually on each local Postgres instance — there is no shared migration mechanism yet.