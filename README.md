# Discount Coupons Management

Spring Boot service for creating discount coupons and registering coupon redemptions. Coupons are matched case-insensitively, have a maximum use count, and can be restricted to a country resolved from the client IP address.

## Requirements

- JDK 21
- Maven wrapper included in the repository

## Build And Test

On Windows:

```powershell
.\mvnw.cmd verify
```

On macOS/Linux:

```sh
./mvnw verify
```

`verify` compiles the application, generates OpenAPI interfaces and DTOs, runs tests, runs integration tests, and writes the JaCoCo report under `target/site/jacoco`.

## Run Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```sh
./mvnw spring-boot:run
```

The application starts on `8080` by default. Override the port or IP lookup service with environment variables:

```sh
PORT=8081 APP_GEO_IPWHOIS_BASE_URL=https://ipwho.is ./mvnw spring-boot:run// todo: mention the external service instead of this config
```

Useful local URLs: // todo: make them clickable

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/h2-console`

## API

- `POST /coupons` creates a coupon.
- `GET /coupons/{code}` reads a coupon by code.
- `POST /coupon-redemptions` redeems a coupon for a user. The client IP is resolved from `X-Forwarded-For` or the remote address.

The OpenAPI contract lives in `src/main/resources/openapi/discount-coupons-api.yaml`. Generated API interfaces and DTOs are produced under `target/generated-sources/openapi`. // todo: link to file?
