# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This repo wraps the `openmrs-module-appointmentscheduling` OpenMRS module (Java/Maven) in an OTAP (Ontwikkeling/Test/Acceptatie/Productie) deployment setup using Docker and GitHub Actions.

The module itself lives in `openmrs-module-appointmentscheduling/` as a Git submodule-style directory targeting **OpenMRS 1.9.9** and Java 6 source compatibility (compiled with Java 8 in CI).

## Build commands

All Maven commands run from inside `openmrs-module-appointmentscheduling/`:

```bash
# Build the .omod artifact (skipping tests)
mvn clean package -DskipTests

# Build + run all tests
mvn clean package

# Run tests only (no rebuild)
mvn test

# Run a single test class
mvn test -pl api -Dtest=AppointmentServiceTest

# Run a single test method
mvn test -pl api -Dtest=AppointmentServiceTest#testGetAllAppointmentTypes
```

The output artifact is: `omod/target/appointmentscheduling-*.omod`

**IntelliJ:** Open `openmrs-module-appointmentscheduling/pom.xml` as a project. Set Project SDK to Java 8 (Eclipse Temurin). Source/language level must be 8.

## Module architecture

Two Maven sub-modules:

| Module | Artifact | Role |
|--------|----------|------|
| `api/` | `appointmentscheduling-api.jar` | Domain model, service layer, DAO interfaces, Hibernate mappings |
| `omod/` | `appointmentscheduling-*.omod` | Spring MVC controllers, JSP views, DWR services, REST resources |

**Layer stack (api):**
- `AppointmentService` (interface) → `AppointmentServiceImpl` (Spring bean, `@Transactional`)
- DAO interfaces (e.g. `AppointmentDAO`) → Hibernate implementations (`HibernateAppointmentDAO`)
- Hibernate mappings: `api/src/main/resources/*.hbm.xml`
- Schema managed by Liquibase: `api/src/main/resources/liquibase.xml`

**Access pattern:** The service is always retrieved via `Context.getService(AppointmentService.class)`. Direct instantiation is wrong.

**Key domain objects:** `AppointmentType` → `AppointmentBlock` → `TimeSlot` → `Appointment`. A block holds slots; a slot holds one or more appointments up to its capacity. `AppointmentRequest` is a separate request-for-appointment flow.

**DWR:** `omod` exposes `DWRAppointmentService` for AJAX calls from JSPs. Registered in `config.xml`.

## OTAP environment

Single Dockerfile at `.docker/Dockerfile`. The `BUILD_ENV` build arg controls runtime behaviour:

| BUILD_ENV | JVM heap | Debug port |
|-----------|----------|------------|
| `development` | 512m/256m | 5005 (JDWP) |
| `test` / `acceptance` | 1024m/512m | — |
| `production` | 2048m/1024m | — |

Compose files: `docker-compose.{dev,test,acceptance,prod}.yml`

Dockerfile uses `azul/zulu-openjdk-debian:7` as runtime (OpenMRS 1.9.9 rejects JVM versions > 1.7) and Tomcat 8.0.53 (last Java-7-compatible release). Builder stage uses `eclipse-temurin:8-jdk-jammy`.

## Known issues / past fixes

- **BOM encoding:** Windows-saved `.java` files may contain a UTF-8 BOM (`\xef\xbb\xbf`) that the Java compiler rejects with `illegal character: '﻿'`. Strip with: `sed -i '1s/^\xef\xbb\xbf//' <file>`
- **Wrong method name:** `AppointmentServiceImpl` had `getAppointmentsForPatient` (non-existent); the correct method is `getAppointmentsOfPatient` (defined in `AppointmentService.java`).
- **PII logging vulnerability:** `getAppointmentsForPatientWithLogging` in `AppointmentServiceImpl` logs patient name, DOB, identifier, and gender — this is explicitly marked as a vulnerability in the code.

## CI/CD workflows

| Workflow | Trigger | Deploys to |
|----------|---------|-----------|
| `build-test.yml` | Push/PR on develop, main, release/* | — (build + test only) |
| `deploy-test.yml` | Push to `develop` | `test` environment |
| `deploy-acceptance.yml` | Push to `release/*` | `acceptance` environment |
| `deploy-prod.yml` | `workflow_dispatch` | `production` environment |

See `OTAP-SETUP.md` for full GitHub Environments configuration, required secrets, branch protection rules, and server-side setup steps.
