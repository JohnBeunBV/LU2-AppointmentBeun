# LU2-AppointmentBeun

OpenMRS Appointment Scheduling module, ingericht in een OTAP-omgeving met Docker en GitHub Actions.

---

## Omgevingen

Het project heeft vier gescheiden omgevingen. Elke omgeving heeft een eigen Docker Compose-bestand, eigen database en eigen configuratie.

| Omgeving | Compose-bestand | Poort | Database | Trigger |
|---|---|---|---|---|
| Development | `docker-compose.dev.yml` | 8080 (+ debug 5005) | `openmrs_dev` | Lokaal |
| Test | `docker-compose.test.yml` | 8081 | `openmrs_test` | Push naar `develop` |
| Acceptatie | `docker-compose.acceptance.yml` | 8082 | `openmrs_acceptance` | Push naar `release/*` |
| Productie | `docker-compose.prod.yml` | 8080 | `openmrs_prod` | Handmatig (`workflow_dispatch`) |

Alle omgevingen gebruiken dezelfde `Dockerfile` (`.docker/Dockerfile`). Het `BUILD_ENV` build-argument bepaalt de JVM-instellingen en het logniveau:

| BUILD_ENV | JVM-heap | Debug | Logniveau |
|---|---|---|---|
| `development` | 512 m / 256 m | JDWP :5005 | DEBUG |
| `test` | 1024 m / 512 m | — | DEBUG |
| `acceptance` | 1024 m / 512 m | — | INFO |
| `production` | 2048 m / 1024 m | — | ERROR |

### Branch → omgeving

```
feature/*   →  alleen build & test (geen deploy)
    ↓ PR
develop     →  automatisch deploy naar Test
    ↓ PR
release/*   →  automatisch deploy naar Acceptatie  (1 reviewer vereist)
    ↓ PR
main        →  handmatig deploy naar Productie     (2 reviewers + workflow_dispatch)
```

---

## Hoe wordt voorkomen dat testdata in productie terechtkomt?

Drie lagen van isolatie zorgen ervoor dat data nooit tussen omgevingen kan lekken:

**1. Gescheiden databases**
Elke omgeving heeft een eigen MySQL-instantie met een eigen database (`openmrs_dev`, `openmrs_test`, `openmrs_acceptance`, `openmrs_prod`). Er is geen gedeelde database en geen verbinding tussen de omgevingen.

**2. Gescheiden Docker-netwerken**
Elke compose-stack draait in een eigen geïsoleerd bridge-netwerk (`openmrs-network-dev`, `-test`, `-acceptance`, `-prod`). Containers kunnen niet over netwerken heen communiceren.

**3. Branch protection + handmatige goedkeuring voor productie**
Code bereikt productie alleen via een `workflow_dispatch` op `main`, na goedkeuring van 2 reviewers. Er is geen automatische deploy naar productie. De pipeline controleert bovendien het versieformaat (`v1.2.3`) vóór de deploy start.

---

## Aan de slag als nieuwe ontwikkelaar

### Vereisten

- Docker Desktop
- Java 8 (Eclipse Temurin) — voor lokaal bouwen buiten Docker
- Maven 3.x — voor lokaal bouwen buiten Docker

### 1. Repository clonen

```bash
git clone <repository-url>
cd LU2-AppointmentBeun
```

### 2. Lokale omgeving starten

De development-omgeving heeft standaard credentials ingebakken — geen `.env`-bestand nodig.

```bash
docker-compose -f docker-compose.dev.yml up -d
```

OpenMRS is beschikbaar op: `http://localhost:8080/openmrs`
Debug-poort (JDWP): `5005`

Standaard inloggegevens voor development:
- Gebruiker: `admin`
- Wachtwoord: `Admin123`

### 3. Module bouwen

```bash
cd openmrs-module-appointmentscheduling

# Bouwen zonder tests
mvn clean package -DskipTests

# Bouwen met tests
mvn clean package

# Enkele testklasse uitvoeren
mvn test -pl api -Dtest=AppointmentServiceTest
```

Het gebouwde artifact staat op: `omod/target/appointmentscheduling-*.omod`

De development compose-stack mount de modulemap als volume — wijzigingen in de code zijn na een rebuild direct zichtbaar zonder de container opnieuw te bouwen.

### 4. Werken met branches

Maak altijd een feature-branch aan vanaf `develop`:

```bash
git checkout develop
git pull
git checkout -b feature/mijn-feature
```

Push naar je feature-branch en open een PR naar `develop`. De CI-pipeline (build + tests + SAST + SCA) draait automatisch bij elke push.

### 5. CI/CD pipeline

De pipeline bestaat uit drie workflowbestanden:

**`pipeline.yml`** — hoofd-pipeline, draait bij elke push/PR:

1. **Gitleaks** — scant op hardcoded secrets (blokkeert bij fund)
2. **Build & Test** — Maven `clean verify` inclusief JaCoCo coverage gate (≥ 70%)
3. **SonarQube** — statische code-analyse (op `main` en PRs; quality gate blokkeert bij FAILED)
4. **Snyk** — SCA scan op CVEs in dependencies (blokkeert bij CVSS ≥ 7)
5. **Docker build + Trivy** — bouwt de image en scant op CRITICAL/HIGH CVEs
6. **Deploy** — alleen bij push op `develop`, `release/*` of handmatig op `main`

**`codeql.yml`** — SAST analyse, draait bij elke push/PR naar `main`, `develop`, `release/*`:

- CodeQL Java-analyse met `security-extended` queries
- Resultaten zichtbaar in GitHub Security tab

**`qodana_code_quality.yml`** — JetBrains codekwaliteit, draait bij PRs en push naar `main`:

- Qodana kwaliteitsgate via `qodana.cloud`

Benodigde GitHub Secrets voor de volledige pipeline:

| Secret | Voor |
|---|---|
| `SONAR_TOKEN` | SonarQube analyse |
| `SNYK_TOKEN` | Snyk dependency scan |
| `QODANA_TOKEN_281741559` | Qodana kwaliteitsanalyse |
| `TEST_SSH_KEY` + `TEST_SERVER_HOST` + `TEST_SERVER_USER` + `TEST_SSH_KNOWN_HOST` | Deploy naar test |
| `ACC_SSH_KEY` + `ACC_SERVER_HOST` + `ACC_SERVER_USER` + `ACC_SSH_KNOWN_HOST` | Deploy naar acceptatie |
| `PROD_SSH_KEY` + `PROD_SERVER_HOST` + `PROD_SERVER_USER` + `PROD_SSH_KNOWN_HOST` | Deploy naar productie |
| `SLACK_WEBHOOK` | Deployment notificaties |

GitHub repository variabelen (`vars.*`): `SONAR_PROJECT_KEY`, `SONAR_ORGANIZATION`, `SONAR_HOST_URL`, `DEPLOYMENTS_ENABLED`.

Zie `OTAP-SETUP.md` voor de volledige serveropzet en configuratie van GitHub Environments.

---

## Bekende issues

| Bestand | Issue | Status |
|---|---|---|
| `AppointmentServiceImpl.java:1426` | **PII-logging** — logt patiëntnaam, DOB, identifier en geslacht in plaintext (AVG-overtreding) | Open |
| `AppointmentUtils.java:29` | **Typfout** — `"View Provider Scedules"` moet `"View Provider Schedules"` zijn | Open |

Zie `docs/auditrapport/05-risicomatrix.md` voor het volledige risicooverzicht.
