# rh-paie-backend

Initial Spring Boot backend foundation for an academic HR and payroll management application.

This first version intentionally contains only the authentication and user account structure. Payroll, attendance, payslips, AI chatbot, facial recognition, employee business modules, and document modules are not implemented yet.

## Tech Stack

- Java 26 installed locally, compiling with Java 21 bytecode for Spring Boot compatibility
- Spring Boot 3
- Maven
- PostgreSQL
- Spring Web
- Spring Data JPA
- Spring Security
- Jakarta Validation
- Lombok
- Flyway
- SpringDoc OpenAPI / Swagger

## Current Endpoints

- `GET /api/auth/health`
- `GET /api/users/health`

## Configuration

PostgreSQL is configured through environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

Defaults are provided in `src/main/resources/application.yml` for local development.

## PostgreSQL Docker

Run the project database container:

```bash
docker run -d \
  --name rh-paie-postgres \
  -e POSTGRES_DB=rh_paie_db \
  -e POSTGRES_USER=rhpaie_user \
  -e POSTGRES_PASSWORD=rhpaie_password \
  -p 5434:5432 \
  -v rh_paie_postgres_data:/var/lib/postgresql/data \
  postgres:15
```

Test the database connection:

```bash
docker exec -it rh-paie-postgres psql -U rhpaie_user -d rh_paie_db
```

## Run

```bash
mvn spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

To test protected endpoints in Swagger:

1. Call `POST /api/auth/register` or `POST /api/auth/login`.
2. Copy the `token` value from the login response.
3. Click `Authorize` in Swagger UI.
4. Enter the token as:

```text
Bearer <token>
```

5. Call protected endpoints such as `GET /api/auth/me`.

## Build

```bash
mvn clean install
```

## Next Step

The next backend step is implementing real authentication with JWT.
