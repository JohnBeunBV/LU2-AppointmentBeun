# 11 Risk Assessment Report - OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-38: Risk Assessment Report opstellen  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 3
**Norm:** NEN-7510:2024-2, AVG (GDPR), CRA

---

## 1. Managementsamenvatting

De OpenMRS Appointment Scheduler module verwerkt bijzondere persoonsgegevens (medische afspraakdata, patiëntidentificatoren) en valt daarmee onder de zwaarste categorie van de AVG en NEN-7510. Uit de security-audit van sprint 2 zijn **12 bevindingen** naar voren gekomen, waarvan **4 kritiek** (rode zone, score ≥ 15).

De meest urgente bevindingen zijn:

- **PII wordt in plaintext gelogd** (R01, score 25) — directe AVG-overtreding
- **Hardcoded productiecredentials in broncode** (R02, score 20) — volledige databasetoegang voor iedereen met leestoegang tot de repository
- **Geen data-level toegangscontrole** (R03, score 16) — elke gebruiker kan afspraken van alle patiënten inzien
- **CI/CD-pipeline blokkeert nooit op SAST-resultaten** (CICD-03, score 20) — security gate is decoratief

Geen van de preventieve beveiligingsbarrières is op dit moment geïmplementeerd. Zonder directe actie is het systeem niet geschikt voor productiegebruik in een zorgomgeving.

De totale geraamde kosten voor volledige mitigatie bedragen **€ 11.200 – € 17.600** (zie sectie 6).

---

## 2. Scope en context

### 2.1 Systeembeschrijving

De OpenMRS Appointment Scheduler is een module bovenop het OpenMRS Electronic Medical Record (EMR) platform. De module biedt:

- Aanmaken, wijzigen en annuleren van patiëntafspraken
- Beheer van beschikbaarheden van zorgverleners
- REST API-endpoints voor integratie met andere OpenMRS-modules
- Rapportagefunctionaliteit via het OpenMRS Reporting Framework

**Technische stack:**

- Java 8 / Maven
- OpenMRS Core 1.9.9
- Spring Framework (MVC, Security)
- Hibernate ORM / MySQL
- REST via openmrs-module-webservices.rest

### 2.2 Auditscope

| Component                         | In scope          |
| --------------------------------- | ----------------- |
| Module-broncode (`api/`, `omod/`) | ✅                |
| GitHub Actions CI/CD-pipeline     | ✅                |
| OpenMRS Core platform             | ❌ (buiten scope) |
| Serverinfrastructuur / netwerk    | ❌ (buiten scope) |
| GitHub-organisatiebeheer          | ❌ (buiten scope) |

---

## 3. Gevoelige gegevens

### 3.1 Verwerkte persoonsgegevens

De module verwerkt de volgende categorieën gevoelige gegevens, geclassificeerd conform AVG artikel 9 (bijzondere persoonsgegevens):

| Gegeven                             | Categorie                 | Broncode-referentie                                                      | AVG-grondslag                 |
| ----------------------------------- | ------------------------- | ------------------------------------------------------------------------ | ----------------------------- |
| Patiëntnaam                         | Bijzonder persoonsgegeven | `Appointment.java` → `patient.getPersonName()`                           | Art. 9 lid 2h (medische zorg) |
| Geboortedatum                       | Persoonsgegeven           | `AppointmentServiceImpl.java` r. 1426 → `patient.getBirthdate()`         | Art. 9 lid 2h                 |
| Patiënt-ID / BSN                    | Bijzonder persoonsgegeven | `AppointmentServiceImpl.java` r. 1428 → `patient.getPatientIdentifier()` | Art. 9 lid 2h                 |
| Geslacht                            | Persoonsgegeven           | `AppointmentServiceImpl.java` r. 1429 → `patient.getGender()`            | Art. 9 lid 2h                 |
| Reden van komst                     | Medische data             | `Appointment.java` → veld `reason`                                       | Art. 9 lid 2h                 |
| Afspraakhistorie                    | Medische data             | DB-tabel `appointment_scheduling_appointment`                            | Art. 9 lid 2h                 |
| Provider-roosters                   | Bedrijfsdata              | `AppointmentBlock.java` → veld `provider`                                | Art. 6 lid 1b                 |
| Audit trails (wie heeft wat gedaan) | Operationele data         | `BaseOpenmrsData` → `creator`, `changedBy`                               | NEN-7510 A.8.15               |

> **Bevinding:** PII uit de bovenstaande tabel wordt aantoonbaar weggeschreven naar logbestanden (`AppointmentServiceImpl.java` regels 1426–1432), wat een directe overtreding is van AVG artikel 9 en NEN-7510 A.8.15.

### 3.2 Datastromen

```
Zorgverlener/Patiënt
       │
       ▼
  Browser/Client
       │  HTTPS (ontbreekt op test/acc)
       ▼
  OpenMRS REST API (/ws/rest/v1/appointment*)
       │
       ▼
  AppointmentService (Spring)
       │          │
       ▼          ▼
  MySQL DB    Logbestanden  ← PII lekt hier naartoe (R01)
```

---

## 4. Bevindingen en mitigaties

### 4.1 Kritieke bevindingen (🔴 Rood, score ≥ 15)

#### R01 — PII in logbestanden (Score 25)

**Locatie:** `AppointmentServiceImpl.java` regels 1426–1432  
**CWE:** CWE-532 (Insertion of Sensitive Information into Log File)  
**NEN-7510:** A.8.15 — Monitoring

**Beschrijving:** Naam, geboortedatum, patiënt-ID en geslacht worden als plaintext gelogd. Logbestanden zijn doorgaans minder streng beveiligd dan de primaire database en zijn toegankelijk voor beheerders zonder medische noodzaak.

**Risico bij niet oplossen:** AVG-meldplicht (art. 33), boete tot € 20 miljoen of 4% jaaromzet, strafrechtelijke aansprakelijkheid bij herhaalde overtredingen.

**Mitigatie:**

```java
// Voor (❌):
log.info("[AUDIT] Fetching appointments for patient: name=" + patient.getPersonName() + ...);

// Na (✅):
log.info("[AUDIT] Fetching appointments for patient UUID: {}", patient.getUuid());
```

**NEN-7510 maatregel:** A.8.15 — Implementeer gestructureerde auditlogging zonder PII; gebruik uitsluitend gepseudonimiseerde UUID's in logbestanden.

---

#### R02 — Hardcoded credentials (Score 20)

**Locatie:** `AppointmentActivator.java` regels 79–82  
**CWE:** CWE-798 (Use of Hard-coded Credentials)  
**NEN-7510:** A.9.2 — Beheer van gebruikerstoegang

**Beschrijving:** Productiewachtwoord en volledige JDBC-verbindingsstring staan hardcoded in de broncode en zijn zichtbaar in de volledige git-geschiedenis.

**Risico bij niet oplossen:** Directe databasetoegang voor iedereen met leestoegang tot de repository; volledige patiëntdataset uitleesbaar en aanpasbaar.

**Mitigatie (twee stappen):**

1. Roteer het wachtwoord `Appt@Export2021!` onmiddellijk — het is gecompromitteerd
2. Lees credentials uit omgevingsvariabelen: `Context.getRuntimeProperties().getProperty("hl7.export.password")`

**NEN-7510 maatregel:** A.9.2 — Sla credentials op in een secrets manager of omgevingsvariabelen; nooit in broncode of versiebeheer.

---

#### R03 — Geen data-level ACL (Score 16)

**Locatie:** `AppointmentResource.java` — `getAppointmentsOfPatient()`  
**CWE:** CWE-639 (Authorization Bypass Through User-Controlled Key / IDOR)  
**NEN-7510:** A.8.3 — Toegangsbeveiliging

**Beschrijving:** Elke geauthenticeerde gebruiker kan alle afspraken van alle patiënten opvragen. Er is geen controle of de aanroepende gebruiker behandelaar is van de betreffende patiënt.

**Risico bij niet oplossen:** Privacyschending voor alle patiënten; AVG art. 5 lid 1b (doelbinding) geschonden.

**Mitigatie:** Voeg eigenaarcheck toe in `AppointmentResource.java`:

```java
if (!isProviderForPatient(currentUser, requestedPatient)) {
    throw new APIAuthenticationException("Geen toegang");
}
```

**NEN-7510 maatregel:** A.8.3 — Implementeer toegangsbeveiliging op dataniveau; toegang uitsluitend op basis van behandelrelatie.

---

#### R04 — Typfouten in privilege-constanten (Score 15)

**Locatie:** `AppointmentUtils.java` regels 29–31  
**CWE:** CWE-284 (Improper Access Control)  
**NEN-7510:** A.8.3 — Toegangsbeveiliging

**Beschrijving:** `"View Provider Scedules"` (foutieve spelling) vs. `"View Provider Schedules"` (database). Alle privilege-checks voor provider-roosters falen hierdoor.

**Mitigatie:** Herstel spelling + voeg test toe die constanten valideert tegen `config.xml`.

**NEN-7510 maatregel:** A.8.3 — Autorisatiemechanismen moeten getest en geverifieerd worden tegen de geregistreerde privilege-definities.

---

#### CICD-03 — SonarQube blokkeert nooit (Score 20)

**Locatie:** `.github/workflows/pipeline.yml` — `continue-on-error: true`  
**NEN-7510:** A.8.25 — Beveiligd ontwikkelproces

**Beschrijving:** De SAST-stap heeft `continue-on-error: true`, waardoor een gefaald quality gate de pipeline niet stopt. Security-bevindingen worden gerapporteerd maar leiden nooit tot een geblokkeerde build.

**Mitigatie:** Verwijder `continue-on-error` op `release/*` en `main`; behoud het op `develop` als waarschuwing.

**NEN-7510 maatregel:** A.8.25 — Het ontwikkelproces moet geautomatiseerde security-gates bevatten die deployment blokkeren bij kritieke bevindingen.

---

#### CICD-04 — Geen geheimscanner (Score 20)

**Locatie:** `.github/workflows/pipeline.yml` — ontbreekt  
**NEN-7510:** A.8.25 — Beveiligd ontwikkelproces

**Beschrijving:** Er is geen geheimscanner (zoals gitleaks) actief in de pipeline. R02 (hardcoded credentials) had hiermee gedetecteerd en geblokkeerd kunnen worden.

**Mitigatie:** Voeg `gitleaks/gitleaks-action` toe als eerste job; maak `build-and-test` afhankelijk hiervan.

**NEN-7510 maatregel:** A.8.25 — Automatische detectie van secrets in broncode als verplichte pipeline-stap.

---

### 4.2 Hoge bevindingen (🟠 Oranje, score 8–14)

| ID      | Bevinding                          | Score | CWE     | NEN-7510 | Mitigatie (samenvatting)                                                       |
| ------- | ---------------------------------- | ----- | ------- | -------- | ------------------------------------------------------------------------------ | --- |
| R05     | Lege `@Authorized()` annotaties    | 12    | CWE-284 | A.8.3    | Voeg expliciete privilege-constanten toe aan alle servicemethoden              |
| R07     | Geen auditlogging afspraakmutaties | 12    | CWE-778 | A.8.15   | Log `saveAppointment`, `voidAppointment`, `cancelAppointment` met UUID + actie |
| R08     | Geen brute-force beveiliging       | 12    | CWE-307 | A.8.5    | Account lockout na N pogingen; rate limiting op reverse proxy                  |
| R09     | ConcurrentModificationException    | 9     | CWE-362 | A.8.6    | Gebruik `iterator.remove()` in `cleanOpenAppointments`                         |
| R11     | Void/retire-vlaggen niet gezet     | 12    | CWE-665 | A.8.6    | Stel `setVoided(true)` / `setRetired(true)` in vóór `save`-aanroep             |
| CICD-05 | Geen SCA/CVE-scan                  | 16    | —       | A.8.8    | Snyk Open Source toevoegen aan pipeline via Maven                              |     |
| CICD-06 | Niet-gepinde GitHub Actions        | 10    | CWE-829 | A.8.9    | Pin alle Actions op SHA-hash                                                   |

---

### 4.3 Lage bevindingen (🟢 Groen, score ≤ 7)

| ID  | Bevinding             | Score | Actie                                                     |
| --- | --------------------- | ----- | --------------------------------------------------------- |
| R10 | Deprecated `Date`-API | 6     | Migreer naar `java.time` bij volgende Java-versie-upgrade |
| R12 | Ongebruikte variabele | 2     | Verwijderen bij volgende code-review                      |

---

## 5. Testresultaten

### 5.1 Unit tests (Maven Surefire)

Uitgedraaid op: 8 juni 2026 | Branch: `develop`

| Module     | Testklassen | Tests   | Geslaagd | Gefaald | Overgeslagen |
| ---------- | ----------- | ------- | -------- | ------- | ------------ |
| `api`      | 24          | 165     | 165      | 0       | 0            |
| `omod`     | 16          | 113     | 113      | 0       | 0            |
| **Totaal** | **40**      | **278** | **278**  | **0**   | **0**        |

Alle bestaande tests slagen. Dit bevestigt dat de huidige codebase functioneel correct is. Het geeft echter **geen zekerheid over security** — de bestaande tests dekken geen van de bevindingen uit dit rapport:

- Geen test verifieert dat PII niet in logs verschijnt (R01)
- Geen test valideert privilege-constanten tegen `config.xml` (R04)
- Geen test controleert dat `retireAppointmentType` de vlag zet (R11)
- Geen test dekt het `ConcurrentModificationException`-scenario (R09)

> **Conclusie:** De testdekking is functioneel adequaat maar security-blind. Aanvullende security-gerichte tests zijn vereist (zie security backlog `09-security-backlog.md`).

### 5.2 SBOM (Software Bill of Materials)

Gegenereerd op: 19 juni 2026 | Tool: CycloneDX Maven Plugin v2.8.0 | Formaat: CycloneDX 1.5 (JSON + XML)

#### Scope

De SBOM dekt de volledige Maven-dependency-tree van de module, gegenereerd via `makeAggregateBom` over de scopes `compile`, `provided`, `runtime` en `system`. Bestanden beschikbaar als `docs/sbom/bom.json` en `docs/sbom/bom.xml`.

#### Componentoverzicht

| Gegeven                 | Waarde                                  |
| ----------------------- | --------------------------------------- |
| Rootcomponent           | `appointmentscheduling:1.17.0-SNAPSHOT` |
| Totaal componenten      | 110 libraries                           |
| Met PURL                | 110/110 (100%)                          |
| Met hashverificatie     | 108/110 (98%)                           |
| Met licentie-informatie | 66/110 (60%)                            |
| Lifecycle-fase          | `build`                                 |

#### Verdeling naar componentgroep

| Groep                 | Aantal |
| --------------------- | ------ |
| Overig (diverse libs) | 49     |
| Apache / Commons      | 21     |
| `org.openmrs`         | 18     |
| `org.springframework` | 13     |
| Hibernate             | 4      |
| Jackson               | 2      |
| Log4j                 | 2      |
| MySQL Connector       | 1      |

#### Licentieverdeling (top 5)

| Licentie         | Componenten |
| ---------------- | ----------- |
| Apache-2.0       | 35          |
| LGPL (varianten) | 7           |
| MIT              | 3           |
| Public Domain    | 3           |
| BSD-3-Clause     | 2           |

> **Aandachtspunt licenties:** 44 van de 110 componenten hebben geen licentie-metadata in de SBOM. Dit is een bekende beperking van de CycloneDX Maven Plugin wanneer licentie-informatie ontbreekt in de upstream POM. Vervolgactie: handmatige verificatie of uitbreiding met een licentie-enrichment tool (bijv. License Finder).

> **Aandachtspunt hashes:** 2 componenten missen hashverificatie. Dit beperkt de integriteitscontrole voor die artefacten. Actie: controleer of deze componenten beschikbaar zijn via Maven Central met verificeerbare checksums.

> **Relatie met SCA:** De SBOM vormt de inputbasis voor de Snyk SCA-scan (zie §5.3). De 110 geïnventariseerde componenten komen overeen met de 103 + 59 gescande dependencies in `api/pom.xml` resp. `omod/pom.xml` (overlap door gedeelde transitive dependencies).

### 5.3 SCA / Dependency-scan (Snyk via Maven)

Uitgevoerd op: 19 juni 2026 | Tool: Snyk Open Source v1.1305.1 | Branch: `develop`

#### Scanresultaten per submodule

| Submodule                      | Doelbestand    | Geanalyseerde dependencies | Directe kwetsbaarheden | Genegeerde kwetsbaarheden |
| ------------------------------ | -------------- | -------------------------- | ---------------------- | ------------------------- |
| `appointmentscheduling` (root) | `pom.xml`      | 0                          | 0                      | 0                         |
| `appointmentscheduling-api`    | `api/pom.xml`  | 103                        | 0                      | 60                        |
| `appointmentscheduling-omod`   | `omod/pom.xml` | 59                         | 0                      | 26                        |

**Geen kwetsbaarheden in de module zelf aangetroffen.** Alle 86 gevonden kwetsbare dependency-paden zijn via de `.snyk` policy genegeerd, omdat het uitsluitend transitieve afhankelijkheden betreft die binnenkomen via `openmrs-api:1.9.9` (provided scope). De OpenMRS Core-versie valt buiten de projectscope en kan binnen dit project niet worden aangepast.

#### Genegeerde kwetsbaarheden — ernst-overzicht

| Ernst   | Unieke CVE's | Voornaamste getroffen libraries                                                                                     |
| ------- | ------------ | ------------------------------------------------------------------------------------------------------------------- |
| Kritiek | 4            | `log4j:1.2.15`, `spring-beans:3.0.5`, `xstream:1.4.3`, `jackson-mapper-asl:1.5.0`                                   |
| Hoog    | 56           | `xstream:1.4.3` (19 paden), `spring-web:3.0.5` (5 paden), `log4j:1.2.15` (4 paden), `spring-webmvc:3.0.5` (4 paden) |

Alle negaties zijn gedocumenteerd in `.snyk` met reden "Transitive via openmrs-api:1.9.9 (provided). Platform locked — outside project scope." en verlopen op 18 juni 2027.

> **Opmerking restrisico:** De kritieke kwetsbaarheden in `log4j 1.2.15`, `Spring 3.0.5` en `XStream 1.4.3` zijn reëel, maar niet oplosbaar zonder een upgrade van OpenMRS Core. Dit risico is bewust geaccepteerd en gedocumenteerd als platform-restrisico (zie sectie 8). De Snyk-scan is geconfigureerd als permanente pipeline-stap (CICD-05).

## 6. Kosteninschatting

De onderstaande raming is gebaseerd op een gemiddeld junior/medior developerstarief van **€ 55–€ 85/uur** en schatting van benodigde uren per bevinding. Dit is een indicatieve raming — werkelijke kosten kunnen afwijken.

### 6.1 Kosten per bevinding

| ID         | Bevinding                                   | Uren (min) | Uren (max) | Kosten min  | Kosten max  | Prioriteit |
| ---------- | ------------------------------------------- | ---------- | ---------- | ----------- | ----------- | ---------- |
| R01        | PII uit logstatements verwijderen           | 2          | 4          | € 110       | € 340       | P1         |
| R02        | Hardcoded credentials verwijderen + rotatie | 4          | 8          | € 220       | € 680       | P1         |
| R03        | Data-level ACL implementeren                | 8          | 16         | € 440       | € 1.360     | P1         |
| R04        | Typfouten herstellen + test toevoegen       | 2          | 4          | € 110       | € 340       | P1         |
| R05        | Lege `@Authorized()` herstellen             | 2          | 4          | € 110       | € 340       | P2         |
| R07        | Auditlogging implementeren                  | 4          | 8          | € 220       | € 680       | P2         |
| R08        | Brute-force beveiliging (infra)             | 4          | 8          | € 220       | € 680       | P2         |
| R09        | ConcurrentModificationException fixen       | 1          | 2          | € 55        | € 170       | P2         |
| R11        | Void/retire-vlaggen fixen (4 methoden)      | 2          | 4          | € 110       | € 340       | P2         |
| CICD-03    | `continue-on-error` verwijderen             | 1          | 2          | € 55        | € 170       | P1         |
| CICD-04    | Gitleaks toevoegen aan pipeline             | 2          | 4          | € 110       | € 340       | P1         |
| CICD-05    | Snyk toevoegen aan pipeline                 | 4          | 8          | € 220       | € 680       | P2         |
| CICD-06    | Actions pinnen op SHA                       | 2          | 4          | € 110       | € 340       | P3         |
| R10        | Deprecated Date-API migreren                | 4          | 8          | € 220       | € 680       | P3         |
| R12        | Ongebruikte variabele verwijderen           | 0.5        | 1          | € 28        | € 85        | P3         |
| **Totaal** |                                             | **42,5**   | **85**     | **€ 2.338** | **€ 7.225** |            |

### 6.2 Aanvullende kosten

| Post                                     | Eenmalig / Structureel | Schatting                        |
| ---------------------------------------- | ---------------------- | -------------------------------- |
| Security code review door externe partij | Eenmalig               | € 2.000 – € 4.000                |
| Penetration test (sprint 3)              | Eenmalig               | € 3.000 – € 5.000                |
| SonarCloud licentie (team)               | Structureel / jaar     | € 0 (open source) – € 960/jaar   |
| Snyk licentie                            | Structureel / jaar     | € 0 (gratis tier) – € 2.400/jaar |
| AVG-boete bij datalek (R01/R02)          | Risico                 | € 0 – € 20.000.000               |

### 6.3 Totaaloverzicht

| Categorie                            | Min         | Max          |
| ------------------------------------ | ----------- | ------------ |
| Ontwikkelkosten (fixes)              | € 2.338     | € 7.225      |
| Aanvullende kosten (pentest, review) | € 5.000     | € 9.000      |
| Toolingkosten (structureel/jaar)     | € 0         | € 3.360      |
| **Totaal (eenmalig + eerste jaar)**  | **€ 7.338** | **€ 19.585** |

> **Risico van niet handelen:** Een AVG-datalek door R01 of R02 kan leiden tot boetes van € 20 miljoen of 4% jaaromzet — vele malen hoger dan de mitigatiekosten.

---

## 7. Aanbevelingen en prioritering

### Onmiddellijk (deze sprint)

1. **R02** — Roteer het gelekte wachtwoord `Appt@Export2021!` vandaag nog
2. **R01** — Verwijder PII uit logstatements
3. **CICD-04** — Voeg gitleaks toe aan pipeline om herhaling te voorkomen
4. **CICD-03** — Verwijder `continue-on-error` op `release/*` en `main`

### Volgende sprint

5. **R03** — Implementeer data-level ACL
6. **R04** — Herstel privilege-constanten + test
7. **R05, R07, R11** — Autorisatie en auditlogging compleet maken
8. **CICD-05** — Snyk toevoegen als vaste pipeline-stap

### Binnen twee sprints

9. **R08** — Brute-force beveiliging (infrastructuurniveau)
10. **CICD-06** — Actions pinnen
11. **R09** — ConcurrentModificationException
12. **R10, R12** — Technische schuld opruimen

---

## 8. Restrisico na mitigatie

Na volledige uitvoering van alle bovenstaande maatregelen resteert een restrisico op:

| Categorie                    | Restrisico                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Eigenaar                     |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- |
| OpenMRS platform EOL (1.9.9) | 86 kwetsbare dependency-paden (4 kritiek, 56 hoog) aangetroffen via Snyk SCA, volledig transitief via `openmrs-api:1.9.9` (provided). Kritieke gevallen: `log4j:1.2.15` (CVE-2019-17571, CVE-2022-23302/05/07), `spring-beans:3.0.5` (CVE-2022-22965 — Spring4Shell), `xstream:1.4.3` (19 CVE's waaronder RCE), `jackson-mapper-asl:1.5.0` (CVE-2019-10202). Niet oplosbaar zonder OpenMRS Core-upgrade. Gedocumenteerd in `.snyk` policy, negaties verlopen 18 juni 2027. | Opdrachtgever/zorginstelling |
| Java 7/8 runtime EOL         | Geen security-updates meer beschikbaar                                                                                                                                                                                                                                                                                                                                                                                                                                     | Opdrachtgever                |
| Geen MFA (R06)               | Authenticatie via OpenMRS core, niet configureerbaar vanuit module                                                                                                                                                                                                                                                                                                                                                                                                         | Systeembeheerder             |
| Geen TLS op test/acceptatie  | Infrastructuurverantwoordelijkheid                                                                                                                                                                                                                                                                                                                                                                                                                                         | DevOps/systeembeheerder      |
| GitHub organisatiebeheer     | Environment protection, verplichte reviewers                                                                                                                                                                                                                                                                                                                                                                                                                               | Projectbeheerder             |

> Conform NEN-7510 A.5.1 is dit restrisico bewust geaccepteerd en gedocumenteerd.

## 9. Referenties

| Document            | Locatie                                                         |
| ------------------- | --------------------------------------------------------------- |
| Asset-identificatie | `docs/auditrapport/03-assets.md`                                |
| Risicocriteria      | `docs/auditrapport/04-risicocriteria.md`                        |
| Risicomatrix        | `docs/auditrapport/05-risicomatrix.md`                          |
| Bow-tie analyse     | `docs/auditrapport/06-bowtie.md`                                |
| Security backlog    | `docs/auditrapport/07-security-backlog.md`                      |
| SBOM                | `docs/sbom/bom.json`, `docs/sbom/bom.xml`                       |
| Testrapporten       | `api/target/surefire-reports/`, `omod/target/surefire-reports/` |
| NEN-7510:2024-2     | Nederlandse norm voor informatiebeveiliging in de zorg          |
| AVG / GDPR          | Verordening (EU) 2016/679                                       |
| CWE Top 25          | https://cwe.mitre.org/top25/                                    |
| CVSS v3.1           | https://www.first.org/cvss/calculator/3.1                       |
