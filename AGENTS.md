# AGENTS.md

## Scope

These instructions apply to the whole repository.

## Project Context

This is a Java 21 Spring Boot 4 application for discount coupon creation and redemption. Persistence uses Spring JDBC with H2 and Liquibase. The HTTP contract is defined in `src/main/resources/openapi/discount-coupons-api.yaml`; generated API interfaces and DTOs are build output and should not be edited by hand.

## Commands

- `.\mvnw.cmd verify` on Windows, or `./mvnw verify` on macOS/Linux, is the main validation command.
- `.\mvnw.cmd spring-boot:run` starts the application locally on the configured `PORT`, defaulting to `8080`.
- Use `.\mvnw.cmd test` only for a faster unit-test pass; integration tests run as part of `verify`.

## Conventions

- Keep source code under the existing `pl.mm.discountcoupons` package structure.
- Keep Liquibase changes in `src/main/resources/db/changelogs` and include them from `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Keep WireMock mappings in the default `src/test/resources/mappings` directory unless a test has a strong reason to customize the file root.
- Reuse the test datasource from `src/test/resources/application.yaml`; avoid per-test in-memory database names unless isolation actually requires it.
