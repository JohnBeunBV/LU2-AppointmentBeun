# C1 / C2 Architectuurmodel — OpenMRS Appointment Scheduling

---

## C1 — Systeemcontextdiagram

Het C1-diagram toont het systeem als één geheel, omgeven door de externe actoren en systemen waarmee het interacteert.

![C1 — Systeemcontextdiagram](../images/c1afbeelding.png)

### Toelichting C1

| Actor / Systeem                    | Rol                                                           |
| ---------------------------------- | ------------------------------------------------------------- |
| **Zorgverlener / Beheerder**       | Primaire gebruiker; plant en bekijkt afspraken via de browser |
| **OpenMRS gebruiker**              | Raadpleegt afspraken via de browser                           |
| **OpenMRS Appointment Scheduling** | Het centrale systeem — scope van dit diagram                  |
| **GitHub repository**              | Broncode-opslag en CI/CD-workflows                            |
| **Deployment servers**             | OTAP-omgevingen; hosten de applicatie via Docker Compose      |

---

## C2 — Containerdiagram

Het C2-diagram toont de interne technische bouwstenen (containers) van het systeem en hoe ze met elkaar communiceren.

![C2 — Containerdiagram](../images/c2afbeelding.png)

### Toelichting C2

| Container                      | Technologie             | Verantwoordelijkheid                                                    |
| ------------------------------ | ----------------------- | ----------------------------------------------------------------------- |
| **OpenMRS WebApp**             | Java 7, Tomcat 8.0.53   | Serveert de UI; laadt de `.omod` module bij opstart                     |
| **Appointment Scheduling API** | Java, Spring, Hibernate | Domeinlogica en persistentie (ingebed in de WebApp)                     |
| **MySQL Database**             | MySQL 8.0               | Relationele opslag; alleen bereikbaar binnen het interne Docker-netwerk |
| **GitHub Actions + GHCR**      | (extern)                | Bouwt de Docker image en deployt deze via SSH naar de doelserver        |

### OTAP-omgevingen

Dezelfde containers draaien in elke omgeving, met omgevingsspecifieke configuratie via `BUILD_ENV`:

| Omgeving    | Compose-bestand                 | Poort | JVM-heap        | Debug      |
| ----------- | ------------------------------- | ----- | --------------- | ---------- |
| Development | `docker-compose.dev.yml`        | 8080  | 512 m / 256 m   | JDWP :5005 |
| Test        | `docker-compose.test.yml`       | 8081  | 1024 m / 512 m  | —          |
| Acceptance  | `docker-compose.acceptance.yml` | 8082  | 1024 m / 512 m  | —          |
| Production  | `docker-compose.prod.yml`       | 8080  | 2048 m / 1024 m | —          |

---

## Threat modellen (afgeleid van de C4-modellen)

De onderstaande threat modellen zijn opgesteld op basis van het C1- en C2-diagram. Per C4-niveau zijn de vertrouwensgrenzen, datastromen en componenten gebruikt als startpunt om te identificeren wat er mis kan gaan. De bevindingen zijn geclassificeerd volgens STRIDE en verwijzen naar de risico-IDs in het risk assessment rapport (`docs/auditrapport/11-risk-assessment-report.md`).

---

### Threat model C1 — Systeemcontext

Op C1-niveau kijken we naar de vertrouwensgrenzen tussen externe actoren en het systeem als geheel.

![Threat Model Niveau 1](../images/Threat%20Model%20Niveau%201.png)

| #   | Actor / grens                          | Dreiging (STRIDE)          | Beschrijving                                                                                                                        | Risico-ID                                          |
| --- | -------------------------------------- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| T1  | Zorgverlener → systeem                 | **Spoofing**               | Aanvaller misbruikt gestolen sessie-cookie om als zorgverlener afspraken in te zien of te muteren                                   | R08 (geen brute-force beveiliging), R06 (geen MFA) |
| T2  | OpenMRS gebruiker → systeem            | **Elevation of Privilege** | Elke geauthenticeerde gebruiker kan via de REST API afspraken van _alle_ patiënten inzien — geen behandelaarcheck                   | R03 (IDOR)                                         |
| T3  | GitHub repository → deployment servers | **Tampering**              | Kwaadaardige code in een pull request of via een gecompromitteerde GitHub Action bereikt de productieomgeving                       | CICD-03, CICD-06                                   |
| T4  | GitHub repository → deployment servers | **Information Disclosure** | SSH-sleutels voor alle OTAP-omgevingen zijn opgeslagen als GitHub Secrets; een gecompromitteerde pipeline-stap kan deze exfiltreren | CICD-04                                            |
| T5  | Systeem → logbestanden / monitoring    | **Information Disclosure** | PII (naam, geboortedatum, patiënt-ID, geslacht) wordt als plaintext gelogd en is zichtbaar voor beheerders zonder medische noodzaak | R01                                                |

---

### Threat model C2 — Containers

Op C2-niveau kijken we naar de grenzen en datastromen _tussen_ de containers.

![Threat Model Niveau 2](../images/Threat%20Model%20Niveau%202.png)

| #   | Container / grens                         | Dreiging (STRIDE)          | Beschrijving                                                                                                                                                                            | Risico-ID  |
| --- | ----------------------------------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| T6  | Browser → OpenMRS WebApp                  | **Tampering**              | DOM XSS in `appointments.jsp:18` — serverdata wordt als HTML gerenderd; aanvaller injecteert scripts via gemanipuleerde serverinvoer                                                    | CodeQL #17 |
| T7  | OpenMRS WebApp → Appointment API          | **Elevation of Privilege** | `getAllProviderSchedules()` heeft een lege `@Authorized()` annotatie — elke sessie heeft toegang, ongeacht rol                                                                          | R05        |
| T8  | OpenMRS WebApp → Appointment API          | **Elevation of Privilege** | Privilege-constanten in `AppointmentUtils.java` bevatten een typfout (`Scedules`); checks falen altijd, waardoor toegangscontrole effectief uitgeschakeld is                            | R04        |
| T9  | Appointment API → MySQL Database          | **Information Disclosure** | Productiecredentials (`Appt@Export2021!`) staan hardcoded in `AppointmentActivator.java` en zijn zichtbaar in de git-geschiedenis; directe databasetoegang zonder OpenMRS-authenticatie | R02        |
| T10 | Appointment API → logbestanden            | **Information Disclosure** | `getAppointmentsForPatientWithLogging()` schrijft naam, geboortedatum, BSN en geslacht als plaintext naar de applicatielog; logbestanden zijn minder beveiligd dan de primaire database | R01        |
| T11 | Appointment API → Appointment API         | **Denial of Service**      | `cleanOpenAppointments()` itereert over een collectie en verwijdert elementen via directe lijstoperatie; veroorzaakt `ConcurrentModificationException` bij gelijktijdige aanroepen      | R09        |
| T12 | GitHub Actions → GHCR → Deployment server | **Tampering**              | GitHub Actions zijn niet gepind op SHA-hashes; een gecompromitteerde upstream action of een package-substitutie-aanval kan kwaadaardige code in de Docker image injecteren              | CICD-06    |
| T13 | MySQL Database                            | **Repudiation**            | Mutaties via `saveAppointment`, `voidAppointment` en `cancelAppointment` laten geen auditspoor achter; bij een incident is niet traceerbaar wie wat heeft gewijzigd                     | R07        |

---

### Vertrouwensgrenzen samengevat

De C4-modellen laten twee kritieke vertrouwensgrenzen zien waarop de meeste dreigingen clusteren:

| Grens                                      | Dreigingen  | Oordeel                                                        |
| ------------------------------------------ | ----------- | -------------------------------------------------------------- |
| **Browser → WebApp** (extern netwerk)      | T1, T2, T6  | Onvoldoende beveiligd: geen MFA, IDOR, XSS                     |
| **WebApp/API → Database** (intern netwerk) | T9          | Kritiek: hardcoded credentials in broncode en git-geschiedenis |
| **GitHub → Deployment** (CI/CD keten)      | T3, T4, T12 | Hoog risico: pipeline is de schakel naar productie             |
| **API → Logbestanden** (interne uitvoer)   | T5, T10     | AVG-overtreding: PII in logbestanden                           |

Voor de volledige onderbouwing per dreiging, bijbehorende mitigaties en prioritering, zie:

- `docs/auditrapport/11-risk-assessment-report.md` — risicoscores en kosteninschatting
- `docs/auditrapport/12-attack-surface.md` — entry points (E1–E9) per C2-container
- `docs/auditrapport/06-bowtie.md` — oorzaak-gevolg-analyse per top-event
