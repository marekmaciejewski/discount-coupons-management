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

The application starts on [http://localhost:8080](http://localhost:8080) by default. Coupon country checks use the external [ipwho.is](https://ipwho.is) service to resolve the client IP address.

Useful local URLs:

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [H2 console](http://localhost:8080/h2-console)

## API

- `POST /coupons` creates a coupon.
- `GET /coupons/{code}` reads a coupon by code.
- `POST /coupon-redemptions` redeems a coupon for a user. The client IP is resolved from `X-Forwarded-For` or the remote address.

The application trusts forwarding headers when resolving the client IP for country checks. Run it behind trusted infrastructure that strips or controls headers such as `Forwarded`, `X-Forwarded-For`, and `X-Real-IP`; otherwise callers can spoof their apparent source IP.

The OpenAPI contract lives in [src/main/resources/openapi/discount-coupons-api.yaml](src/main/resources/openapi/discount-coupons-api.yaml). Generated API interfaces and DTOs are produced under `target/generated-sources/openapi`.
