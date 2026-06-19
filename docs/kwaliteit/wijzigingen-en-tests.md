# Wijzigingen en gemaakte tests

## Overzicht

Dit document beschrijft alle wijzigingen die zijn doorgevoerd in de codebase als onderdeel van de kwaliteits- en securityverbetering. De wijzigingen omvatten: consolidatie van CI/CD workflows, toevoeging van JaCoCo coverage meting, en aanvullende tests voor security-kritieke code.

---

## 1. CI/CD Pipeline

### Gewijzigd bestand: `.github/workflows/pipeline.yml`

De vijf afzonderlijke workflowbestanden zijn samengevoegd tot één enkele workflow.

**Verwijderde bestanden:**
- `build-test.yml`
- `build-and-sbom.yml`
- `deploy-test.yml`
- `deploy-acceptance.yml`
- `deploy-prod.yml`

**Opgebouwde job-structuur:**

```
pipeline.yml:
secret-scan
    └── build-and-test (build, tests, JaCoCo, SonarQube, Snyk, SBOM)
            └── docker-build (Trivy scan)
                    ├── deploy-test       (alleen develop)
                    ├── deploy-acceptance (alleen release/*)
                    └── deploy-prod       (alleen workflow_dispatch)

codeql.yml (parallel):
analyze (CodeQL Java SAST — security-extended)

qodana_code_quality.yml (parallel):
qodana (JetBrains kwaliteitsanalyse)
```

**Opgeloste bugs ten opzichte van de oude workflows:**

| Bug | Oplossing |
|-----|-----------|
| `logs -f` blokkeerde SSH-sessie in deploy-test | `-f` flag verwijderd |
| Integratietest draaide op de runner i.p.v. de server | Vervangen door health check met `curl` naar remote host |
| `submodules: recursive` ontbrak in acceptance en SBOM workflows | Toegevoegd aan alle checkout-stappen die broncode nodig hebben |
| Surefire reports pad onjuist voor multi-module project | Gewijzigd naar `**/surefire-reports/**` |
| Race condition: twee workflows bouwden tegelijk dezelfde Docker image tag | Verdwenen door één job-keten |
| `deploy-test` en `build-test` draaiden gelijktijdig, image nog niet klaar | `needs: docker-build` garandeert volgorde |

**Toegevoegde beveiligingsmaatregelen in de pipeline:**

- **Gitleaks** secret scan als eerste job — blokkeert bij gevonden secrets
- **SonarQube** SAST analyse (alle branches `continue-on-error: true` — SonarCloud free plan beperking; harde pipeline-gates via Gitleaks en Snyk)
- **Snyk** dependency vulnerability scan (CVSS ≥ 7)
- **Trivy** container image scan (CRITICAL/HIGH CVE's)
- SSH-sleutels via `webfactory/ssh-agent` (sleutel nooit naar schijf geschreven)
- SBOM gegenereerd per run en opgeslagen als artifact

---

## 2. JaCoCo coverage meting

### Gewijzigd bestand: `openmrs-module-appointmentscheduling/pom.xml`

JaCoCo 0.8.11 is toegevoegd aan de parent POM. Het plugin draait automatisch tijdens `mvn clean verify` in alle submodules.

**Toegevoegde executions:**

| Execution | Fase | Effect |
|-----------|------|--------|
| `jacoco-prepare-agent` | `test` | Instrumenteert bytecode voor meting |
| `jacoco-report` | `verify` | Genereert HTML/XML rapport in `target/site/jacoco/` |
| `jacoco-check` | `verify` | Faalt de build als coverage onder de drempel zakt |

**Drempel:** 70% instruction coverage (doel: 80% — zie [02-coverage-onderbouwing.md](02-coverage-onderbouwing.md))

### Gewijzigd bestand: `openmrs-module-appointmentscheduling/omod/pom.xml`

De `jacoco-check` execution is uitgeschakeld voor de `omod` module via `<phase>none</phase>`. De `omod` bevat Spring MVC controllers, DWR services en REST resources die een draaiende Tomcat vereisen en structureel niet door unit tests gedekt kunnen worden. Het rapport wordt wel gegenereerd.

### Gewijzigd bestand: `.github/workflows/pipeline.yml`

Coverage rapport wordt na elke run geüpload als GitHub Actions artifact:

```
Artifacts → jacoco-coverage-<versie>-<datum>/
  ├── api/target/site/jacoco/index.html
  └── omod/target/site/jacoco/index.html
```

---

## 3. Nieuwe tests

### 3.1 AppointmentValidatorTest

**Bestand:** `api/src/test/java/org/openmrs/module/appointmentscheduling/validator/AppointmentValidatorTest.java`

**Type:** Pure unit test — geen database, geen OpenMRS context vereist.

**Dekt:** `AppointmentValidator` — was volledig ongedekt voor deze toevoeging.

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `supports_shouldSupportAppointmentClass` | `supports(Appointment.class)` | `true` |
| `supports_shouldNotSupportArbitraryClass` | `supports(Object.class)` | `false` |
| `validate_nullAppointment_shouldRejectWithGeneralError` | `validate(null, errors)` | Error met code `error.general` |
| `validate_appointmentWithNullTimeSlot_shouldRejectTimeSlotField` | Afspraak zonder tijdslot | FieldError op `timeSlot` |
| `validate_appointmentWithNullPatient_shouldRejectPatientField` | Afspraak zonder patiënt | FieldError op `patient` |
| `validate_appointmentWithNullType_shouldRejectAppointmentTypeField` | Afspraak zonder type | FieldError op `appointmentType` |
| `validate_appointmentTypeNotSupportedByBlock_shouldRejectWithNotSupportedTypeCode` | Type staat niet in het blok | Error code `notSupportedType` |
| `validate_fullyValidAppointment_shouldProduceNoErrors` | Volledig geldige afspraak | Geen errors |

---

### 3.2 AppointmentServiceSecurityTest

**Bestand:** `api/src/test/java/org/openmrs/module/appointmentscheduling/api/AppointmentServiceSecurityTest.java`

**Type:** Integratietest — extends `BaseModuleContextSensitiveTest`, laadt `standardAppointmentTestDataset.xml`.

**Dekt:** Security-kritieke methoden in `AppointmentServiceImpl`.

#### PII-logging vulnerability

| Testmethode | Scenario | Doel |
|-------------|----------|------|
| `getAppointmentsForPatientWithLogging_shouldReturnSameResultAsGetAppointmentsOfPatient` | Patiënt 1 | Documenteert vulnerability; bewaakt returnwaarde op regressie |
| `getAppointmentsForPatientWithLogging_shouldNotReturnNullForKnownPatient` | Patiënt 2 | Null-check op resultaat |

> **Toelichting:** `getAppointmentsForPatientWithLogging` logt patiëntnaam, geboortedatum, BSN en geslacht naar de applicatielog. Dit is een bekende PII-vulnerability (gedocumenteerd in CLAUDE.md). De tests bewaken dat de methode correct delegeert naar `getAppointmentsOfPatient` zodat functionele regressie direct zichtbaar is.

#### bookAppointment guards

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `bookAppointment_onAlreadyPersistedAppointment_shouldThrowAPIException` | Afspraak met bestaand id | `APIException` |
| `bookAppointment_whenSlotFullAndOverbookFalse_shouldThrowTimeSlotFullException` | Slot 1 vol (6 min over, type 1 vereist 54 min), overbook=false | `TimeSlotFullException` |
| `bookAppointment_whenSlotFullAndOverbookTrue_shouldSucceed` | Zelfde slot, overbook=true | Boeking geslaagd, id toegewezen |
| `bookAppointment_withNullStatus_shouldDefaultToScheduled` | Nieuwe afspraak zonder status, ruim slot 8 | Status = `SCHEDULED` |

#### changeAppointmentStatus

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `changeAppointmentStatus_shouldUpdateStatusAndCreateHistoryEntry` | Afspraak 1 van SCHEDULED naar COMPLETED | Status bijgewerkt, history record aanwezig |
| `changeAppointmentStatus_withNullAppointment_shouldNotThrow` | null meegeven | Geen NPE (expliciete null-check in implementatie) |

---

## 4. Testdekking na wijzigingen

| Module | Voor | Na (verwacht) |
|--------|------|----------------|
| `api` | 70% | ~73–75% |
| `omod` | 32% | 32% (check uitgeschakeld, rapport beschikbaar) |

De `api` module stijgt door het toevoegen van 15 nieuwe testmethoden die voorheen volledig ongedekte code raken (`AppointmentValidator`, `bookAppointment`, `changeAppointmentStatus`, `getAppointmentsForPatientWithLogging`).
