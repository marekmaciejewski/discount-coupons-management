# Discount Coupons Management

| [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=coverage)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management) | [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=bugs)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management)<br>[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=marekmaciejewski_discount-coupons-management&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=marekmaciejewski_discount-coupons-management) |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Spring Boot service for creating discount coupons and registering coupon redemptions. Coupons are matched case-insensitively, have a maximum use count, and can be restricted to a country resolved from the client IP address.

## Live Demo

- [Frontend UI](https://marekmaciejewski.github.io/discount-coupons-management/)
- [Swagger UI](https://discount-coupons-management.onrender.com/swagger-ui/index.html)
- API base URL: `https://discount-coupons-management.onrender.com`

> [!IMPORTANT]
> The first request after inactivity may take about a minute. The backend runs on Render Free and may need to wake up
> before the demo responds.

The hosted backend uses ephemeral in-memory H2 storage. Restart, redeploy, or idle spin-down starts with an empty
database, so demo coupons need to be created before they can be redeemed.

## Requirements

- JDK 21
- Maven wrapper included in the repository
- Node.js 24 for the optional frontend UI

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

Frontend UI:

```powershell
cd frontend
npm ci
npm run build
```

The frontend build generates TypeScript API types from `src/main/resources/openapi/discount-coupons-api.yaml` and writes the static Vite output under `frontend/dist`.

In GitHub Actions, the [Coverage and SonarQube](https://github.com/marekmaciejewski/discount-coupons-management/actions/workflows/coverage.yml)
workflow shows a coverage table in the job summary, uploads the full HTML report as the `jacoco-coverage-report`
artifact, and publishes analysis to the
[SonarQube Cloud report](https://sonarcloud.io/summary/overall?id=marekmaciejewski_discount-coupons-management&branch=master).
The analysis includes backend sources, frontend TypeScript/React sources, and the frontend Pages workflow. Frontend
generated API types, build output, dependencies, and frontend coverage are excluded until frontend tests are added.

The [Frontend Pages](https://github.com/marekmaciejewski/discount-coupons-management/actions/workflows/frontend-pages.yml)
workflow builds the Vite app from `frontend` and deploys it to GitHub Pages.

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

Run the frontend locally against the local backend:

```powershell
cd frontend
npm run dev
```

Run the frontend locally against the hosted Render backend:

```powershell
cd frontend
npm run dev:render
```

## API

- `POST /coupons` creates a coupon.
- `GET /coupons/{code}` reads a coupon by code.
- `POST /coupon-redemptions` redeems a coupon for a user. The client IP is resolved from `X-Forwarded-For` or the remote address.

The application trusts forwarding headers when resolving the client IP for country checks. Run it behind trusted infrastructure that strips or controls headers such as `Forwarded`, `X-Forwarded-For`, and `X-Real-IP`; otherwise callers can spoof their apparent source IP.

The OpenAPI contract lives in [src/main/resources/openapi/discount-coupons-api.yaml](src/main/resources/openapi/discount-coupons-api.yaml). Generated API interfaces and DTOs are produced under `target/generated-sources/openapi`.

Quick smoke test against the hosted backend:

```powershell
curl.exe https://discount-coupons-management.onrender.com/v3/api-docs
```

## Deployment Notes

The checked-in `Dockerfile` builds the Maven project with JDK 21 and runs the packaged jar on a JRE image. For Render,
create a Web Service from this repository, select the Free instance type, and use Docker as the runtime.

Deployment-relevant environment variables:

| variable                    | example                                                    | purpose                                               |
|-----------------------------|------------------------------------------------------------|-------------------------------------------------------|
| `PORT`                      | provided by Render                                         | host-provided server port, defaults to `8080` locally |
| `SPRING_H2_CONSOLE_ENABLED` | `false`                                                    | disables the public H2 console in the hosted service  |
| `JAVA_TOOL_OPTIONS`         | `-XX:MaxRAMPercentage=75`                                  | caps JVM heap relative to container memory            |
| `APP_CORS_ALLOWED_ORIGINS`  | `https://marekmaciejewski.github.io,http://localhost:5173` | comma-separated frontend origins allowed by CORS      |
