# 12 Attack Surface Mapping - OpenMRS Appointment Scheduler

**Sprint:** 3  
**Taak:** SOF-41: Attack Surface Mapping  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1  
**Datum:** Juni 2026  
**Norm:** NEN-7510:2024

---

## 1. Inleiding

Een attack surface omvat alle punten waarop een aanvaller interactie kan hebben met een systeem — bewust of onbewust, geauthenticeerd of niet. Dit document identificeert en classificeert alle ingangen van de OpenMRS Appointment Scheduler module, inclusief:

- De technische entry points (API, UI, database, logging)
- Wat het systeem impliciet vertrouwt (trust model)
- Welke ingangen het hoogste risico vormen
- Hoe de bevindingen de bestaande threat models bijwerken

De attack surface analyse is aanvullend op de risicomatrix (`05-risicomatrix.md`) en de bow-tie (`06-bowtie.md`). Waar die documenten vragen *wat er mis kan gaan*, beantwoordt dit document *via welke ingang* een aanvaller dat kan bereiken.

---

## 2. Overzicht attack surface

```
                        ┌────────────────────────────────────────────────────────┐
                        │           OpenMRS Appointment Scheduler                │
                        │                                                        │
  Browser / Client ────►│  [E1] REST API  /ws/rest/v1/appointment*              │
                        │                                                        │
  Browser / AJAX  ────►│  [E2] DWR-service  /module/appointmentscheduling/dwr  │
                        │                                                        │
  Browser / HTTP  ────►│  [E3] Spring MVC controllers  (JSP-views)             │
                        │           ↓                                            │
                        │  [E4] OpenMRS Context  (auth + privilege checks)      │
                        │           ↓                                            │
                        │  [E5] Service-laag  AppointmentServiceImpl            │
                        │           ↓                                            │
                        │  [E6] Hibernate DAO  →  MySQL database                │
                        │                                                        │
  Log-aggregator  ◄────│  [E7] Log framework  (Log4j / Commons Logging)        │
                        │                                                        │
  GitHub / CI     ────►│  [E8] CI/CD pipeline  (.github/workflows/)            │
                        │                                                        │
  Module startup  ────►│  [E9] ModuleActivator  AppointmentActivator            │
                        └────────────────────────────────────────────────────────┘
```

---

## 3. Entry points

### E1 — REST API

| Kenmerk       | Waarde                                                                 |
| ------------- | ---------------------------------------------------------------------- |
| Pad           | `/ws/rest/v1/appointment`, `/ws/rest/v1/appointmentblock`, etc.        |
| Bronbestand   | `omod/src/main/java/.../rest/resource/AppointmentResource.java`        |
| Authenticatie | HTTP Basic Auth via `BasicAuthenticationFilter` (OpenMRS core)         |
| Autorisatie   | `@Authorized` annotaties op service-methoden; geen row-level filtering |
| Blootstelling | Toegankelijk voor alle geauthenticeerde gebruikers over het netwerk    |
| Risico        | 🔴 **Hoog**                                                            |

**Kwetsbaarheden:**

- **IDOR (R03):** `getAppointmentsOfPatient(patient)` filtert niet op behandelaarrelatie. Elke ingelogde gebruiker kan afspraken van *elke* patiënt opvragen.
- **Lege autorisatie (R05):** `getAllProviderSchedules()` heeft `@Authorized()` zonder privilege — toegankelijk voor alle sessies.
- **Typfout privilege (R04):** `PRIV_VIEW_PROVIDER_SCHEDULES = "View Provider Scedules"` matcht nooit de database-waarde; privilege-check faalt altijd.

---

### E2 — DWR (Direct Web Remoting) AJAX-service

| Kenmerk       | Waarde                                                                 |
| ------------- | ---------------------------------------------------------------------- |
| Pad           | `/module/appointmentscheduling/dwr/*`                                  |
| Bronbestand   | `omod/src/main/java/.../web/DWRAppointmentService.java`                |
| Authenticatie | OpenMRS sessie (cookie-gebaseerd)                                      |
| Autorisatie   | Afhankelijk van DWR-configuratie in `config.xml`                       |
| Blootstelling | Toegankelijk vanuit de browser; oproepen zijn niet altijd zichtbaar    |
| Risico        | 🟠 **Midden**                                                          |

**Aandachtspunten:**

- DWR-methoden worden opgeroepen via gegenereerde JavaScript-stubs in de browser. De autorisatiechecks zijn minder zichtbaar dan bij REST en kunnen makkelijk over het hoofd worden gezien bij code-reviews.
- DWR-endpoints worden niet gedekt door de REST-resourcelaag en vallen buiten het normale `@Authorized`-patroon.

---

### E3 — Spring MVC Controllers (webinterface)

| Kenmerk       | Waarde                                                                       |
| ------------- | ---------------------------------------------------------------------------- |
| Pad           | `/module/appointmentscheduling/*.form`, `/appointmentBlockCalendar.htm`, etc. |
| Bronbestand   | `omod/src/main/java/.../web/controller/`                                     |
| Authenticatie | `Context.isAuthenticated()` check in controller                              |
| Autorisatie   | Handmatige checks in `AppointmentBlockCalendarController.java` r. 51         |
| Blootstelling | Toegankelijk via browser; sessie vereist                                      |
| Risico        | 🟠 **Midden**                                                                |

**Aandachtspunten:**

- Controllerlaag heeft minder consistente autorisatiechecks dan de service-laag met `@Authorized`.
- Formulierverwerking in JSP/Spring MVC vertrouwt op OpenMRS framework voor inputvalidatie; `AppointmentTypeValidator.java` biedt basis­validatie maar afdekking is niet volledig.

---

### E4 — OpenMRS Context (authenticatie- en autorisatielaag)

| Kenmerk       | Waarde                                                                        |
| ------------- | ----------------------------------------------------------------------------- |
| Bronbestand   | OpenMRS Core (`Context.java`); module gebruikt `Context.getService()` pattern |
| Authenticatie | Centraleciseerd in OpenMRS Core; module erft dit                              |
| Autorisatie   | `@Authorized(privilege)` annotaties op `AppointmentService` interface         |
| Blootstelling | Interne laag; niet direct van buitenaf bereikbaar                             |
| Risico        | 🟡 **Laag** (direct), 🔴 **Hoog** (bij misconfiguratie)                       |

**Aandachtspunten:**

- De module vertrouwt volledig op `Context.getAuthenticatedUser()` als de bron van identiteit. Als een sessie gecompromitteerd is, heeft de module geen verdere bescherming.
- Privilege-checks via `@Authorized` werken alleen als de privileges correct zijn geregistreerd in `config.xml` én de constanten in `AppointmentUtils.java` foutloos zijn gespeld (zie R04).

---

### E5 — Service-laag (AppointmentServiceImpl)

| Kenmerk       | Waarde                                                                  |
| ------------- | ----------------------------------------------------------------------- |
| Bronbestand   | `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`            |
| Toegang       | Altijd via `Context.getService(AppointmentService.class)`               |
| Blootstelling | Intern; niet rechtstreeks bereikbaar van buitenaf                       |
| Risico        | 🟠 **Midden** (kwetsbaarheden in de logica)                             |

**Kwetsbaarheden:**

- **PII-logging (R01):** `getAppointmentsForPatientWithLogging()` schrijft naam, geboortedatum, BSN en geslacht naar de applicatielog (r. 1424–1430).
- **Ontbrekende auditlogging (R07):** `saveAppointment`, `voidAppointment`, `cancelAppointment` loggen geen gestructureerde auditinformatie.
- **Void/retire-vlaggen niet gezet (R11):** `retireAppointmentType`, `voidAppointment`, `voidTimeSlot`, `voidAppointmentBlock` slaan op zonder de bijbehorende statusvlag te zetten.
- **ConcurrentModificationException (R09):** `cleanOpenAppointments` verwijdert elementen tijdens iteratie.

---

### E6 — Persistentielaag (Hibernate / MySQL)

| Kenmerk       | Waarde                                                                   |
| ------------- | ------------------------------------------------------------------------ |
| Bronbestand   | `api/src/main/java/.../db/hibernate/HibernateAppointmentDAO.java`        |
| Configuratie  | `AppointmentSchedulingHibernateMappings.xml`; `openmrs-runtime.properties` |
| Blootstelling | Intern; bereikbaar via Hibernate ORM                                     |
| Risico        | 🔴 **Kritiek** (door hardcoded credentials)                              |

**Kwetsbaarheden:**

- **Hardcoded credentials (R02):** `AppointmentActivator.java` r. 81–82 bevat het productiewachtwoord `Appt@Export2021!` en een volledige JDBC-URL. Dit wachtwoord staat in de git-geschiedenis en geeft directe toegang tot de HL7-rapportagedatabase.

---

### E7 — Log framework

| Kenmerk       | Waarde                                                             |
| ------------- | ------------------------------------------------------------------ |
| Implementatie | Apache Commons Logging (wrapper over Log4j of Logback)            |
| Bronbestand   | Gebruikt in o.a. `AppointmentServiceImpl.java`                     |
| Uitvoer       | Applicatielog op de Tomcat-server; toegankelijk voor beheerders    |
| Risico        | 🔴 **Hoog** (als PII-logging actief is)                            |

**Aandachtspunten:**

- Logbestanden zijn technisch minder streng beveiligd dan de patiëntdatabase maar bevatten door R01 dezelfde PII.
- Het log framework vormt zelf een aanvalsvector als verouderde Log4j-versies worden gebruikt (Log4Shell CVE-2021-44228); zie SC-05/SC-06.

---

### E8 — CI/CD Pipeline (GitHub Actions)

| Kenmerk       | Waarde                                                                   |
| ------------- | ------------------------------------------------------------------------ |
| Bronbestand   | `.github/workflows/pipeline.yml`                                         |
| Blootstelling | GitHub-hosted runners; secrets beschikbaar in pipeline-context           |
| Risico        | 🔴 **Hoog**                                                              |

**Aandachtspunten:**

- De pipeline heeft toegang tot SSH-sleutels voor alle drie OTAP-omgevingen (`TEST_SSH_KEY`, `ACC_SSH_KEY`, `PROD_SSH_KEY`).
- Een gecompromitteerde GitHub Actions-stap of een kwaadaardige pull request kan secrets exfiltreren.
- GitHub Actions zijn momenteel niet gepind op SHA-hashes (CICD-06), wat supply chain aanvallen mogelijk maakt.

---

### E9 — ModuleActivator (opstartroutine)

| Kenmerk       | Waarde                                                              |
| ------------- | ------------------------------------------------------------------- |
| Bronbestand   | `api/src/main/java/.../AppointmentActivator.java`                   |
| Trigger       | Automatisch bij het opstarten van OpenMRS                           |
| Blootstelling | Intern; wordt uitgevoerd met de rechten van de OpenMRS-applicatie   |
| Risico        | 🔴 **Kritiek** (door hardcoded credentials in dezelfde klasse)      |

**Aandachtspunten:**

- Credentials in `AppointmentActivator.java` worden meegeladen bij elke module-start.
- De `started()`-methode roept `AppointmentSchedulerSetup.setupCleanOpenAppointmentsTask()` aan op basis van een configuratiewaarde — als die waarde manipuleerbaar is, kan de scheduler ongewenst worden gestart.

---

## 4. Trust model

Het trust model beschrijft wat de module impliciet vertrouwt — zonder expliciete verificatie.

| Vertrouwde component                   | Vertrouwen niveau | Reden van vertrouwen                                                                                          | Risico bij breuk                                              |
| -------------------------------------- | :---------------: | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| **OpenMRS Core**                       | Volledig          | De module erft authenticatie, sessiebeheer en basisprivileges van de OpenMRS-kern zonder eigen validatie      | Kwetsbaarheden in OpenMRS Core (EOL 1.9.9) raken de module direct |
| **Context.getAuthenticatedUser()**     | Volledig          | De identiteit van de huidige gebruiker wordt als waarheid aangenomen; de module voert geen hervalidatie uit   | Gecompromitteerde sessie geeft volledige toegang tot servicelaag |
| **Hibernate ORM**                      | Volledig          | SQL wordt niet handmatig opgebouwd; de module vertrouwt erop dat Hibernate SQL-injectie voorkomt              | Bug of misconfiguratie in Hibernate leidt tot directe SQL-blootstelling |
| **Spring MVC / DWR framework**         | Volledig          | Input die via controllers binnenkomt, wordt niet nogmaals gesaniteerd op serviceniveau                        | XSS of parameter-tampering als het framework dit niet afvangt  |
| **Java JVM runtime**                   | Volledig          | Standaardveronderstelling; geen aanvullende sandboxing                                                        | JVM-kwetsbaarheden (CVE's op Java 7 — EOL)                    |
| **Maven-afhankelijkheden (SC-05)**      | Gedeeltelijk      | Snyk en Trivy controleren op bekende CVE's in de CI/CD-pipeline                                              | Verouderde bibliotheek met 0-day vóór detectie                 |
| **GitHub Actions-runners**             | Gedeeltelijk      | GitHub-hosted runners worden als veilig beschouwd, maar zijn gedeelde omgevingen                              | Runner-compromise geeft toegang tot secrets in pipeline-context |
| **`.gitleaks.toml`-suppression-lijst** | Gedeeltelijk      | Suppressions worden op twee-ogen-principe beoordeeld, maar zijn handmatig beheerd                             | Foutief gesuppresseerde secret wordt niet gedetecteerd         |

---

## 5. High risk ingangen

De onderstaande ingangen hebben de hoogste prioriteit op basis van blootstelling × aanwezige kwetsbaarheid:

| Rang | Entry point                    | Reden                                                                         | Gekoppelde risico's   |
| :--: | ------------------------------ | ----------------------------------------------------------------------------- | --------------------- |
|  1   | **E6 — Persistentielaag**      | Hardcoded productiepassword in broncode en git-geschiedenis (directe DB-toegang) | R02                   |
|  2   | **E1 — REST API**              | IDOR: elke gebruiker leest alle patiëntafspraken; lege privilege-checks       | R03, R04, R05         |
|  3   | **E5 — Service-laag (logging)**| PII van patiënten belandt in logbestanden; geen audit trail voor mutaties      | R01, R07              |
|  4   | **E8 — CI/CD pipeline**        | SSH-sleutels voor productie; niet-gepinde Actions; geen SHA-pinning            | CICD-04, CICD-05, CICD-06 |
|  5   | **E4 — OpenMRS Context**       | Module vertrouwt volledig op sessie; geen hervalidatie bij gevoelige operaties | R05, R06              |

---

## 6. Bijgewerkt threat model

Dit hoofdstuk koppelt de attack surface bevindingen terug aan de bestaande threat models en geeft aan waar die moeten worden bijgewerkt.

### 6.1 Bijstelling bow-tie (06-bowtie.md)

| Oorzaak in bow-tie | Entry point | Aanvullende informatie vanuit attack surface                                                   |
| ------------------ | ----------- | ---------------------------------------------------------------------------------------------- |
| O1 — PII in logs   | E5, E7      | Aanvalspad: authenticated user → REST API (E1) → `getAppointmentsForPatientWithLogging()` (E5) → logbestand (E7) → beheerder/log-aggregator leest PII |
| O2 — Geen data-level ACL | E1   | Aanvalspad: authenticated user → `GET /ws/rest/v1/appointment?patient=<uuid>` → geen eigenaarcheck → alle afspraken exposed |
| O3 — Lege @Authorized() | E1, E3 | Aanvalspad: elke sessie → `getAllProviderSchedules()` zonder privilege-vereiste → provider-roosterdata exposed |
| O4 — Geen auditlogging | E5    | Aanvalspad: mutaties via E1 of E3 → `saveAppointment/void/cancel` → geen auditrecord → incident ondetecteerbaar |
| O5 — Geen brute-force | E3, E4 | Aanvalspad: login-endpoint OpenMRS → ongelimiteerde pogingen → accountovername |

**Nieuw inzicht (niet in eerdere bow-tie):**

- **E6/R02 — Hardcoded credentials:** Dit is een aanvalspad dat niet via het top-event loopt maar direct via de persistentielaag. Een aanvaller met toegang tot de repository of git-geschiedenis kan de HL7-database aanspreken *zonder authenticatie via OpenMRS*. Dit vereist een aparte oorzaak-kolom in de bow-tie.
- **E8 — CI/CD als aanvalspad:** De pipeline vormt een aanvalsoppervlak richting alle OTAP-omgevingen. Dit is uitgewerkt in de CI/CD bow-tie (`10-cicd-bowtie.md`) maar ontbrak in de applicatie-bow-tie (`06-bowtie.md`).

### 6.2 Bijstelling risicomatrix (05-risicomatrix.md)

De risicomatrix bevat alle geïdentificeerde kwetsbaarheden. Vanuit de attack surface analyse zijn geen nieuwe risico's geïdentificeerd, maar de volgende paden zijn concreter geworden:

| Risico | Aanvalspad (nieuw inzicht)                                                                                  |
| ------ | ----------------------------------------------------------------------------------------------------------- |
| R02    | Niet alleen via broncode-toegang, maar ook via openbare git-clone van de repository (fork, mirror, leak)     |
| R03    | Aanvaller hoeft geen admin te zijn: elk geldig account volstaat voor IDOR via REST API                      |
| R04    | Privilege-typfout kan *beide* kanten op werken: nooit toegang (lockout) of altijd toegang (bypass), afhankelijk van de OpenMRS-versie |

### 6.3 Nieuw risico: E9 — ModuleActivator opstart

De `AppointmentActivator` bevat naast de hardcoded credentials ook een opstart­routine die een scheduled task aanmaakt op basis van een OpenMRS-globalProperty (`GP_CLEAN_OPEN_APPOINTMENTS`). Als een beheerder met beperkte rechten deze property kan manipuleren, kan de cleanup-taak onbedoeld worden geactiveerd of uitgeschakeld.

**Aanbeveling:** Voeg een privilege-check toe op het lezen en schrijven van `GP_CLEAN_OPEN_APPOINTMENTS` via de module-configuratie.

---

## 7. Samenvatting: Attack surface reductie aanbevelingen

| Prioriteit | Entry point | Maatregel                                                                   | Verwacht effect                              |
| ---------- | ----------- | --------------------------------------------------------------------------- | -------------------------------------------- |
| 🔴 P1      | E6          | Verwijder hardcoded credentials uit `AppointmentActivator.java` (R02)       | Sluit directe databasetoegang via git-lek    |
| 🔴 P1      | E1          | Voeg eigenaarcheck toe in `AppointmentResource.java` (R03)                  | Verhindert IDOR op patiëntafspraken          |
| 🔴 P1      | E5, E7      | Verwijder PII uit `getAppointmentsForPatientWithLogging()` (R01)            | Elimineert PII-lekpad via logbestanden       |
| 🔴 P1      | E1, E4      | Herstel privilege-spelling in `AppointmentUtils.java` (R04)                 | Herstelt werkende privilege-checks           |
| 🟠 P2      | E5          | Implementeer audit logging op mutatie-methoden (R07)                        | Maakt aanvalssporen detecteerbaar            |
| 🟠 P2      | E1, E3      | Vervang lege `@Authorized()` door expliciete privileges (R05)               | Verhindert ongeautoriseerde schema-toegang   |
| 🟠 P2      | E8          | Pin GitHub Actions op SHA-hashes (CICD-06)                                  | Mitigeert supply chain aanval via pipeline   |
| 🟡 P3      | E4          | Configureer brute-force beveiliging op OpenMRS-platform (R08)               | Vertraagt credential-stuffing aanvallen      |

---

## 8. Referenties

| Document                          | Relatie                                                             |
| --------------------------------- | ------------------------------------------------------------------- |
| `03-assets.md`                    | Asset-IDs (DA-xx, SC-xx, PA-xx) gebruikt in dit document           |
| `05-risicomatrix.md`              | Risico-IDs (R01–R12) gekoppeld aan entry points                     |
| `06-bowtie.md`                    | Oorzaken O1–O5 gekoppeld aan attack surface entry points            |
| `07-security-backlog.md`          | Acties per risico inclusief acceptatiecriteria                      |
| `08-cicd-risico-evaluatie.md`     | CI/CD-bevindingen (CICD-03 t/m CICD-06) gekoppeld aan E8           |
| `10-cicd-bowtie.md`               | Dreigingen T3–T5 corresponderen met E8 in dit document             |
