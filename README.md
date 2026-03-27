# JWT Spring Security

[![code style: prettier](https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat-square)](https://github.com/prettier/prettier)

This project implements JWT-based authentication using Spring Security, Spring Boot 4 and Java 25.

## Prerequisites

- Docker
- Java 25
- OpenSSL

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

# CORS & Redirect
CORS_ALLOWED_ORIGINS=http://localhost:3000
ALLOWED_REDIRECT_URIS=http://localhost:3000/callback
```

Adjust the values as needed for your environment.

### 2. Generate RSA Keys

Before running the application, you must generate the RSA key pair for JWT signing and verification. Run the following commands (for local use only):

```sh
cd src/main/resources/keys/local-only
```

To generate Private Key:

```sh
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048
```

To generate Public Key from Private Key:

```sh
openssl rsa -pubout -in private_key.pem -out public_key.pem
```

### 3. Start Infrastructure

Use Docker Compose to start the PostgreSQL database and pgAdmin:

```sh
docker compose up -d
```

- **PostgreSQL**: Port `5432`
- **pgAdmin**: Port `5050` (Login: `admin@admin.com` / `root`)

## Running the Application

Run the Spring Boot application using the Maven Wrapper:

```sh
./mvnw spring-boot:run
```

By default, the application runs on `http://localhost:8080`.

## Testing

Run the tests using the command line:

```sh
./mvnw test
```
