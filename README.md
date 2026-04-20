# Aegis ID

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uy-nguyen00_aegis-id&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uy-nguyen00_aegis-id)
[![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

This project implements JWT-based authentication using Spring Security, Spring Boot 4 and Java 25.

## Prerequisites

- Docker
- Java 25
- OpenSSL (for generating RSA keys)

## Setup

### 1. Generate RSA Keys

Before running the application, generate an RSA key pair in PEM format:

```sh
# Generate the RSA private key
openssl genpkey -algorithm RSA -out secrets/jwt/private_key.pem -pkeyopt rsa_keygen_bits:2048

# Derive the public key
openssl rsa -pubout -in secrets/jwt/private_key.pem -out secrets/jwt/public_key.pem
```

For CI/CD, store the PEM files in your secret manager and provide file locations through environment variables (`JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION`).

### 2. Configure Environment Variables

Create a `.env` file in the project root with the following properties (for local use only):

```sh
# Project properties
APP_NAME=aegis_id
SPRING_PROFILES_ACTIVE=dev # or "prod"

# Database properties
DB_URL=localhost
DB_PORT=5432
DB_NAME=aegis_id
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT key files on Docker host (mounted as Docker secrets)
JWT_PRIVATE_KEY_FILE=./secrets/jwt/private_key.pem
JWT_PUBLIC_KEY_FILE=./secrets/jwt/public_key.pem

# JWT key locations inside container
JWT_PRIVATE_KEY_LOCATION=file:/run/secrets/jwt_private_key
JWT_PUBLIC_KEY_LOCATION=file:/run/secrets/jwt_public_key

# JWT token expiration (milliseconds)
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# JWT roles claim toggle (optional, default: false)
JWT_INCLUDE_ROLES_CLAIM=false

# Management server port (Actuator endpoints, default: 8081)
MANAGEMENT_PORT=8081
```

Adjust the values as needed for your environment.

### 3. Start Services

Use Docker Compose to build and start the full stack (PostgreSQL, app, and pgAdmin):

```sh
docker compose up -d --build
```

- **Application**: Port `8080`
- **Management (Actuator)**: Port `8081`
- **PostgreSQL**: Port `5432`
- **pgAdmin**: Port `5050` (Login: `admin@admin.com` / `root`)

## Running the Application

The application runs in Docker as the `app` service.

Useful commands:

```sh
docker compose logs -f app
docker compose down
```

By default, the application runs on `http://localhost:8080`.

To rotate keys without rebuilding the image, replace the PEM files referenced by `JWT_PRIVATE_KEY_FILE` / `JWT_PUBLIC_KEY_FILE` and restart the `app` service.

## Testing

Run the tests using the command line:

```sh
./mvnw test
```

This project uses Maven naming conventions in a single test source set:

- Unit tests: `*Test` / `*Tests` (run by Surefire in the `test` phase)
- Integration tests: `*IT` (run by Failsafe in the `integration-test`/`verify` phases)

Run unit + integration tests:

```sh
./mvnw verify
```

Run only integration tests:

```sh
./mvnw -DskipTests failsafe:integration-test failsafe:verify
```

Test sources are organized under:

- `src/test/java`: Java test classes
- `src/test/resources`: test configuration files (`application-dev.yml`, `application-prod.yml`)

## Management Endpoints

Actuator endpoints run on a **separate management port** (`8081` by default) and are not exposed through the main application port. Do not expose the management port publicly in production — secure it at the network level.

| Endpoint | Method | Description |
| ---------- | -------- | ------------- |
| `/actuator/health` | GET | Application health check |
| `/actuator/jwtconfig` | GET | Read current JWT configuration (e.g., `includeRolesClaim`) |
| `/actuator/jwtconfig` | POST | Update JWT configuration at runtime (body: `{"includeRolesClaim": true}`) |
