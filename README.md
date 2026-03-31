# Aegis ID

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uy-nguyen00_aegis-id&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uy-nguyen00_aegis-id)
[![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

This project implements JWT-based authentication using Spring Security, Spring Boot 4 and Java 25.

## Prerequisites

- Docker
- Java 25
- OpenSSL (for generating RSA keys)

## Setup

### 1. Configure Environment Variables

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

# JWT RSA Keys (Base64-encoded DER format)
JWT_PRIVATE_KEY=<your-base64-encoded-private-key>
JWT_PUBLIC_KEY=<your-base64-encoded-public-key>

# JWT token expiration (milliseconds)
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# JWT roles claim toggle (optional, default: false)
JWT_INCLUDE_ROLES_CLAIM=false

# Management server port (Actuator endpoints, default: 8081)
MANAGEMENT_PORT=8081
```

Adjust the values as needed for your environment.

### 2. Generate RSA Keys

Before running the application, you must generate the RSA key pair for JWT signing and verification and encode them as Base64:

```sh
# Generate the RSA private key
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048

# Derive the public key
openssl rsa -pubout -in private_key.pem -out public_key.pem

# Extract Base64-encoded DER values (strip PEM headers and newlines)
grep -v "^-" private_key.pem | tr -d '\n'   # → set as JWT_PRIVATE_KEY in your .env
grep -v "^-" public_key.pem | tr -d '\n'    # → set as JWT_PUBLIC_KEY in your .env
```

Copy the output of each command into your `.env` file as `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` respectively. Keep the PEM files out of the repository — they are only needed to generate the Base64 values.

For CI/CD, store the Base64 values as secrets (`JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`) in your GitHub repository settings and reference them in the workflow environment.

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

## Testing

Run the tests using the command line:

```sh
./mvnw test
```

## Management Endpoints

Actuator endpoints run on a **separate management port** (`8081` by default) and are not exposed through the main application port. Do not expose the management port publicly in production — secure it at the network level.

| Endpoint | Method | Description |
| ---------- | -------- | ------------- |
| `/actuator/health` | GET | Application health check |
| `/actuator/jwtconfig` | GET | Read current JWT configuration (e.g., `includeRolesClaim`) |
| `/actuator/jwtconfig` | POST | Update JWT configuration at runtime (body: `{"includeRolesClaim": true}`) |
