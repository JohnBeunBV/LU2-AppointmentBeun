# 07 Security Backlog - OpenMRS Appointment Scheduler

**Sprint:** 3
**Taak:** SOF-42: Threat model uitbreiding + security backlog bijwerken
**Module:** OpenMRS Appointment Scheduler
**Versie:** 2
**Norm:** NEN-7510:2024

> **Wijzigingen t.o.v. versie 1 (SOF-38):**
>
> - R14 (SQL Injection) toegevoegd als nieuwe P2-bevinding vanuit threat modelling sessie SOF-42
> - CICD-03, CICD-04, CICD-05 bijgewerkt naar status ✅ Opgelost (sprint 2)
> - CICD-07, CICD-10 bijgewerkt naar ✅ Opgelost

---

## 1. Inleiding

Dit document is de centrale prioriteitenlijst voor alle beveiligingsbevindingen die zijn gevonden tijdens de audit van de OpenMRS Appointment Scheduler module en de bijbehorende CI/CD-pipeline. Per bevinding is vastgelegd:

- Of het **binnen of buiten scope** valt, inclusief onderbouwing
- De **prioriteit en actie** voor in-scope bevindingen
- De **geaccepteerde reden** voor buiten-scope bevindingen

De scope van dit project is de **OpenMRS Appointment Scheduling module** (`openmrs-module-appointmentscheduling/`) inclusief de GitHub Actions CI/CD-pipeline. Risico's die uitsluitend op platform-, infrastructuur- of organisatieniveau liggen (OpenMRS core, GitHub-organisatiebeheer, serverinrichting) vallen buiten de directe verantwoordelijkheid van het ontwikkelteam en zijn gedocumenteerd als buiten scope.

---

## 2. Statuslegenda

| Status            | Omschrijving                                    |
| ----------------- | ----------------------------------------------- |
| 🔴 Open           | Nog niet opgepakt                               |
| 🟡 In behandeling | Actief aan gewerkt in huidige sprint            |
| ✅ Opgelost       | Geïmplementeerd en gevalideerd                  |
| 📋 Gepland        | Ingepland voor volgende sprint                  |
| ⬜ Buiten scope   | Bewust uitgesloten — zie onderbouwing           |
| 🔕 Geaccepteerd   | Restrisico bewust geaccepteerd met onderbouwing |

---

## 3. In scope — Bevindingen die worden opgepakt

### 3.1 Module-code bevindingen

| ID  | Bevinding                                                    | Score | Zone      | Status  | Prioriteit   | NEN-7510 | Eigenaar |
| --- | ------------------------------------------------------------ | :---: | --------- | ------- | ------------ | -------- | -------- |
| R01 | PII in logbestanden                                          |  25   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.15   | Dev-team |
| R02 | Hardcoded credentials in broncode                            |  20   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.9.2    | Dev-team |
| R03 | Geen data-level ACL                                          |  16   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.3    | Dev-team |
| R04 | Typfouten in privilege-constanten                            |  15   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.3    | Dev-team |
| R05 | Lege `@Authorized()` op servicemethoden                      |  12   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.3    | Dev-team |
| R07 | Geen auditlogging voor afspraakmutaties                      |  12   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.15   | Dev-team |
| R11 | `retireAppointmentType` / `voidAppointment` zet vlag niet    |  12   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.6    | Dev-team |
| R09 | `ConcurrentModificationException` in `cleanOpenAppointments` |   9   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.6    | Dev-team |
| R13 | HQL-injectie in `searchAppointmentsByPatientName`            |  12   | 🟠 Oranje | ✅ Opgelost | P2 — Midden  | A.8.24   | Dev-team |
| **R14** | **SQL Injection via gebruikersinvoer of opgeslagen data**    | **15** | **🔴 Rood** | **🔴 Open** | **P2 — Hoog** | **A.8.24** | **Dev-team** |

> **Toelichting R14-prioriteit:** Hoewel de score (15) in de rode zone valt en gelijkstaat aan R04, is R14 als P2 geclassificeerd omdat Hibernate ORM een baseline-bescherming biedt via HQL en de kwetsbaarheid nog niet pinpoint gelokaliseerd is in een specifiek codepad. Verificatie en codeaudit zijn de eerste stap. Mocht de audit een concreet onbeveiligd codepad blootleggen, wordt de prioriteit bijgesteld naar P1.

### 3.2 CI/CD pipeline bevindingen

| ID      | Bevinding                                              | Score | Zone      | Status      | Prioriteit   | NEN-7510 | Eigenaar |
| ------- | ------------------------------------------------------ | :---: | --------- | ----------- | ------------ | -------- | -------- |
| CICD-03 | `continue-on-error` SonarQube — SAST blokkeert nooit   |  20   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.25   | Dev-team |
| CICD-04 | Geen geheimscanner (gitleaks) in pipeline              |  20   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.25   | Dev-team |
| CICD-05 | Geen SCA / CVE-scan op dependencies en container image |  16   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.8    | Dev-team |
| CICD-06 | Niet-gepinde GitHub Actions (`@v4`, `@v6`)             |  10   | 🟡 Geel   | 📋 Gepland  | P3 — Midden  | A.8.9    | Dev-team |

---

## 4. Gedetailleerde acties per in-scope bevinding

---

### R13 — HQL-injectie in `searchAppointmentsByPatientName`

**Bestand:** `api/src/main/java/.../api/db/hibernate/HibernateAppointmentDAO.java`, regel 317

**Probleem:**

```java
// VULNERABILITY: SQL injection - patientName is concatenated directly into query
String hql = "from Appointment ap where ap.visit.patient.personName.givenName = '"
    + patientName + "' or ap.visit.patient.personName.familyName = '" + patientName + "'";
return super.sessionFactory.getCurrentSession().createQuery(hql).list();
```

`patientName` wordt zonder parameterisatie in de HQL-query geconcateneerd. De methode is momenteel **dode code** (niet aangesloten op enig endpoint), maar de kwetsbaarheid is gedocumenteerd en moet worden opgelost.

**Actie:** Vervang string-concatenatie door `.setParameter()`:

```java
String hql = "from Appointment ap where ap.visit.patient.personName.givenName = :name"
           + " or ap.visit.patient.personName.familyName = :name";
return super.sessionFactory.getCurrentSession()
    .createQuery(hql)
    .setParameter("name", patientName)
    .list();
```

**Acceptatiecriterium:** Geen string-concatenatie meer in HQL/SQL-queries. Gevalideerd door code-review en grep-check in CI.

**Restrisico na fix:** Laag — methode is momenteel niet bereikbaar; na fix ook bij aansluiting veilig.

---

### R14 — SQL Injection via gebruikersinvoer of opgeslagen data

**Bron:** Threat modelling sessie SOF-42
**NEN-7510:** A.8.24 (Gebruik van veilige coderingspraktijken)
**OWASP:** A03:2021 — Injection

**Achtergrond:**

SQL injection omvat twee hoofdvarianten die beide relevant zijn voor de Appointment Scheduler:

- **Directe injectie:** Kwaadaardige code wordt ingevoegd in gebruikersinvoer die direct wordt doorgegeven aan een SQL-query.
- **Second-order injectie:** Kwaadaardige strings worden eerst opgeslagen als valide data en later pas uitgevoerd wanneer ze worden samengevoegd in een dynamische SQL-opdracht — bijvoorbeeld in een rapportage- of exportroutine.

De second-order variant is extra relevant in combinatie met R02: de HL7-exportroutine in `AppointmentActivator.java` maakt verbinding met een externe rapportagedatabase via hardcoded credentials. Als opgeslagen data ongesaniteerd wordt doorgegeven aan die exportquery, is de aanvalsoppervlakte groter dan de module zelf.

**Te auditen locaties (prioriteitsvolgorde):**

| Locatie                                         | Reden                                                              |
| ----------------------------------------------- | ------------------------------------------------------------------ |
| `HibernateAppointmentDAO.java`                  | Primaire DAO-laag; native queries mogelijk naast HQL               |
| `AppointmentActivator.java` (HL7-exportroutine) | Maakt verbinding met externe database; mogelijk dynamische queries |
| `DWRAppointmentService.java`                    | Ontvangt browser-input; minder zichtbaar dan REST                  |
| `AppointmentServiceImpl.java`                   | HQL-criteria; controleer op `createQuery()` met concatenatie       |
| Spring MVC controllers (`web/controller/`)      | Formulierinvoer die doorstroomt naar service- of DAO-laag          |

**Actie (gefaseerd):**

**Fase 1 — Codeaudit (direct):**

```bash
# Zoek naar potentieel onveilige queryopbouw in de codebase:
grep -rn "createNativeQuery\|createQuery.*+" --include="*.java" .
grep -rn "\"SELECT\|\"INSERT\|\"UPDATE\|\"DELETE" --include="*.java" .
grep -rn "\.append.*sql\|sql.*\+" --include="*.java" .
```

**Fase 2 — Fix onveilige queries:**

```java
// ❌ Onveilig — stringconcatenatie in query:
String hql = "FROM Appointment WHERE patientId = '" + patientId + "'";
Query query = session.createQuery(hql);

// ✅ Veilig — named parameter:
Query query = session.createQuery(
    "FROM Appointment WHERE patientId = :patientId");
query.setParameter("patientId", patientId);
```

```java
// ❌ Onveilig — native query met concatenatie:
session.createNativeQuery(
    "SELECT * FROM appointment WHERE notes LIKE '%" + userInput + "%'");

// ✅ Veilig — geparametriseerde native query:
session.createNativeQuery(
    "SELECT * FROM appointment WHERE notes LIKE :search")
    .setParameter("search", "%" + userInput + "%");
```

**Fase 3 — Invoervalidatie als extra verdedigingslaag:**

Voeg validatie toe in de controller- en service-laag, ongeacht het ORM-framework. OpenMRS biedt `ValidateUtil` en de `AppointmentTypeValidator` als basis; breid deze uit:

```java
// Voorbeeld: weiger invoer met SQL-metakarakters waar vrije tekst niet verwacht wordt
if (input != null && input.matches(".*[;'\"\\\\--].*")) {
    throw new APIException("Ongeldige invoer gedetecteerd");
}
```

**Fase 4 — Geautomatiseerde test:**

Voeg een integratietest toe die SQL-injectiepayloads als invoer verzendt:

```java
@Test
public void sqlInjectionPayloadShouldNotCauseException() {
    String[] payloads = {
        "' OR '1'='1",
        "'; DROP TABLE appointment; --",
        "1' UNION SELECT * FROM users --",
        "admin'--"
    };
    for (String payload : payloads) {
        // Verwacht: lege resultatenlijst of validatiefout — GEEN SQLException
        assertDoesNotThrow(() ->
            appointmentService.getAppointmentsByNote(payload));
    }
}
```

**Acceptatiecriteria:**

| Criterium                                                                                 | Verificatie                                         |
| ----------------------------------------------------------------------------------------- | --------------------------------------------------- |
| Geen `createQuery()` of `createNativeQuery()` met stringconcatenatie van gebruikersinvoer | grep-check in CI-pipeline                           |
| Alle DAO-methoden gebruiken `setParameter()` of typed criteria                            | Code review + Hibernate SQL-logging in testomgeving |
| SQL-injectie-integratietest slaagt zonder `SQLException`                                  | Automatische test in CI                             |
| HL7-exportroutine gebruikt geen dynamische queryopbouw                                    | Handmatige code review `AppointmentActivator.java`  |

**Restrisico na fix:** Laag — mits alle query-entry points geparametriseerd zijn. ORM-gebruik (Hibernate) biedt al een baseline; het restrisico zit in native queries en de HL7-exportroutine buiten de ORM-laag.

**Relatie met andere risico's:**

| Risico                      | Relatie                                                                                                     |
| --------------------------- | ----------------------------------------------------------------------------------------------------------- |
| R02 (Hardcoded credentials) | Een succesvolle SQL-injectie via de HL7-exportroutine heeft direct toegang tot de externe database door R02 |
| R03 (Geen data-level ACL)   | SQL-injectie kan de IDOR-bescherming volledig omzeilen door data-filtering in de query te manipuleren       |
| R01 (PII-logging)           | Een aanvaller die SQL-injectie combineert met de loggingkwetsbaarheid kan gericht PII exfiltreren           |

---

### R01 — PII in logbestanden

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`, regels 1426–1432

**Probleem:**

```java
log.info("[AUDIT] Fetching appointments for patient: "
    + "name=" + patient.getPersonName()           // ❌ PII
    + " dob=" + patient.getBirthdate()            // ❌ PII
    + " identifier=" + patient.getPatientIdentifier().getIdentifier()  // ❌ PII
    + " gender=" + patient.getGender());          // ❌ PII
```

**Actie:** Vervang de logstatement door uitsluitend een gepseudonimiseerde UUID:

```java
log.info("[AUDIT] Fetching appointments for patient UUID: {}", patient.getUuid());
```

**Acceptatiecriterium:** Geen van de velden `getPersonName()`, `getBirthdate()`, `getPatientIdentifier()`, `getGender()` mag voorkomen in een `log.*`-aanroep. Gevalideerd door unit test en grep-check in CI.

**Restrisico na fix:** Laag — UUID alleen geeft geen directe identificatie van de persoon.

---

### R02 — Hardcoded credentials

**Bestand:** `omod/src/main/java/.../AppointmentActivator.java`, regels 79–82

**Probleem:**

```java
private static final String HL7_EXPORT_PASSWORD = "Appt@Export2021!";
private static final String HL7_DB_URL = "jdbc:mysql://hl7-reports.hospital.internal:3306/appointments?user=appt_export_svc&password=Appt@Export2021!";
```

**Actie (twee stappen):**

1. **Onmiddellijk:** Roteer het productiewachtwoord `Appt@Export2021!` — het staat in de git-geschiedenis en moet als gecompromitteerd worden beschouwd.
2. **Code-fix:** Verwijder de constanten; lees de waarden uit omgevingsvariabelen of OpenMRS runtime properties:

```java
private String getHL7Password() {
    return Context.getRuntimeProperties().getProperty("hl7.export.password");
}
```

**Acceptatiecriterium:** `gitleaks` (CICD-04) detecteert geen secrets meer in de codebase. De credentials staan niet in broncode of git-geschiedenis (voor nieuwe commits).

**Restrisico na fix:** De git-geschiedenis vóór de fix bevat nog steeds het wachtwoord. Rotatie van het wachtwoord is hierdoor de primaire maatregel — code-fix alleen is niet voldoende.

---

### R03 — Geen data-level ACL

**Bestand:** `omod/src/main/java/.../rest/resource/AppointmentResource.java` (en `AppointmentService`)

**Probleem:** `getAppointmentsOfPatient(patient)` controleert niet of de aanroeper behandelaar of de patiënt zelf is. Elke geauthenticeerde gebruiker kan alle afspraken van elke patiënt opvragen.

**Actie:** Voeg een eigenaarcheck toe in de resource-laag:

```java
Patient requestedPatient = (Patient) object;
User currentUser = Context.getAuthenticatedUser();

if (!currentUser.hasRole("System Developer") &&
    !isProviderForPatient(currentUser, requestedPatient)) {
    throw new APIAuthenticationException(
        "Geen toegang tot afspraken van deze patiënt");
}
```

**Acceptatiecriterium:** Integratietest bewijst dat gebruiker A geen afspraken van patiënt B kan opvragen tenzij gebruiker A behandelaar is van patiënt B.

---

### R04 — Typfouten in privilege-constanten

**Bestand:** `api/src/main/java/.../AppointmentUtils.java`, regels 29–31

**Actie:** Herstel de spelling en voeg een test toe:

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Schedules";   // ✅
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Schedules"; // ✅
```

**Acceptatiecriterium:** Test slaagt; privilege-checks voor provider-roosters werken correct in integratie.

---

### R05 — Lege `@Authorized()` op servicemethoden

**Bestand:** `api/src/main/java/.../api/AppointmentService.java`

**Actie:**

```java
// Na:
@Authorized(AppointmentUtils.PRIV_VIEW_PROVIDER_SCHEDULES)
List<ProviderSchedule> getAllProviderSchedules();
```

**Acceptatiecriterium:** Een gebruiker zonder het `View Provider Schedules`-privilege krijgt een `APIAuthenticationException`.

---

### R07 — Geen auditlogging voor afspraakmutaties

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`

**Actie:** Voeg gestructureerd auditlog toe op mutatiemethoden — uitsluitend met UUID's, geen PII:

```java
log.info("[AUDIT] appointment.save | user={} | appointmentUuid={} | action={}",
    Context.getAuthenticatedUser().getUuid(),
    appointment.getUuid(),
    appointment.getId() == null ? "CREATE" : "UPDATE");
```

**Acceptatiecriterium:** Na elke mutatie-aanroep verschijnt een `[AUDIT]`-regel in de log zonder PII.

---

### R09 — ConcurrentModificationException in `cleanOpenAppointments`

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`, regels 961–988

**Actie:**

```java
// Na (optie A — iterator.remove):
Iterator<Appointment> it = appointmentsInStates.iterator();
while (it.hasNext()) {
    Appointment appointment = it.next();
    if (shouldRemove(appointment)) {
        it.remove(); // ✅
    }
}
```

**Acceptatiecriterium:** Unit test dekt het scenario waarbij een afspraak wordt opgeruimd zonder exception.

---

### R11 — `retireAppointmentType` / `voidAppointment` zet vlag niet

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`

**Actie:** Voeg de vlagzetting toe vóór elke `save`-aanroep:

| Methode                                | Toe te voegen actie                                               |
| -------------------------------------- | ----------------------------------------------------------------- |
| `retireAppointmentType(type, reason)`  | `type.setRetired(true); type.setRetireReason(reason);`            |
| `voidAppointment(appointment, reason)` | `appointment.setVoided(true); appointment.setVoidReason(reason);` |
| `voidTimeSlot(timeSlot, reason)`       | `timeSlot.setVoided(true); timeSlot.setVoidReason(reason);`       |
| `voidAppointmentBlock(block, reason)`  | `block.setVoided(true); block.setVoidReason(reason);`             |

**Acceptatiecriterium:** Unit tests bewijzen dat na aanroep van elke methode de vlag `true` is en de reden is opgeslagen.

---

### CICD-03 — SonarQube `continue-on-error` verwijderd

**Status: ✅ Opgelost in sprint 2**

Pipeline opgesplitst: `develop` gebruikt `continue-on-error: true`; `release/*` en `main` hebben een hard gate zonder `continue-on-error`.

---

### CICD-04 — Gitleaks secret scanning toegevoegd

**Status: ✅ Opgelost in sprint 2**

`secret-scan` job is de eerste job in de pipeline; `build-and-test` is hiervan afhankelijk.

---

### CICD-05 — SCA / CVE-scan toegevoegd (OWASP + Trivy)

**Status: ✅ Opgelost in sprint 2**

OWASP Dependency Check op Maven-dependencies (blokkeert bij CVSS ≥ 7); Trivy container image scan (blokkeert bij CRITICAL/HIGH op `release/*` en `main`).

---

### CICD-06 — Niet-gepinde GitHub Actions

**Status: 📋 Gepland (sprint 4)**

**Actie:** Pin alle externe Actions op volledige SHA-hashes:

| Huidige tag                        | Te pinnen SHA (voorbeeld)                                               |
| ---------------------------------- | ----------------------------------------------------------------------- |
| `actions/checkout@v4`              | `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683`             |
| `actions/setup-java@v4`            | `actions/setup-java@c5195efecf7bdfc987ee8bae7a71cb8b11521c00`           |
| `actions/upload-artifact@v4`       | `actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02`      |
| `docker/login-action@v3`           | `docker/login-action@74a5d142397b4f367a81961eba4e8cd7edddf772`          |
| `docker/build-push-action@v6`      | `docker/build-push-action@263435318d21b8e681c14492fe198d362a7d2c83`     |
| `slackapi/slack-github-action@v2`  | `slackapi/slack-github-action@37ebaef184d7626c5f204ab8d3baff4262dd30f0` |
| `gitleaks/gitleaks-action@v2`      | vast te stellen bij implementatie                                       |
| `aquasecurity/trivy-action@0.28.0` | vast te stellen bij implementatie                                       |
| `webfactory/ssh-agent@v0.9.0`      | vast te stellen bij implementatie                                       |

**Acceptatiecriterium:** Geen enkele externe Action in `pipeline.yml` gebruikt een floating tag; alle tags zijn SHA-hashes.

---

## 5. Buiten scope — Bewust uitgesloten bevindingen

| ID       | Bevinding                                           | Score  | Reden buiten scope                                                                                         | Aanbeveling                                                                                   |
| -------- | --------------------------------------------------- | :----: | ---------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| R06      | Geen MFA op OpenMRS-instantie                       |   12   | MFA is een OpenMRS-platforminstelling, niet configureerbaar vanuit de module.                              | Stel "Require 2FA" in via de OpenMRS-beheerinterface; gebruik OAuth2/SAML identity provider.  |
| R08      | Geen brute-force beveiliging                        |   12   | Account lockout en rate limiting zijn OpenMRS core-functionaliteit en reverse proxy-instellingen.          | Configureer lockout in OpenMRS runtime properties; voeg IP-rate limiting toe op nginx/Apache. |
| R10      | Gebruik van deprecated `Date`-API                   |   6    | Technische schuld met laag beveiligingsrisico. Betreft uitsluitend onderhoudbaarheid.                      | Migreer naar `java.time` bij een Java-versie-upgrade.                                         |
| R12      | Ongebruikte variabele `satisfyingConstraints`       |   2    | Score 2 — verwaarloosbaar risico. Betreft codekwaliteit, geen beveiligingsimpact.                          | Verwijder de variabele bij een volgende code-review.                                          |
| CICD-01  | GitHub beheerderaccount gecompromitteerd            |   15   | MFA-afdwinging op GitHub-organisatieniveau is een beheerderstaak buiten de module-scope.                   | Stel MFA verplicht via GitHub Org → Settings → Authentication.                                |
| CICD-02  | GitHub Secrets gelekt via logs                      |   15   | Secretbeheer en rotatieschema zijn een GitHub-organisatie- en DevOps-verantwoordelijkheid.                 | Stel een secrets-rotatieschema in; gebruik OIDC voor cloud-deployments.                       |
| CICD-07  | SSH private key tijdelijk op runner-schijf          |   12   | ✅ Gemitigeerd in sprint 2 via `webfactory/ssh-agent`.                                                     | —                                                                                             |
| CICD-08  | Geen geformaliseerde rollback procedure             |   15   | ✅ Deels gemitigeerd: automatische rollback bij health check failure. Geen volledige blue-green strategie. | Implementeer blue-green deployment bij beschikbaarheid tweede productieserver.                |
| CICD-09  | Geen verplichte reviewer voor productie-environment |   15   | GitHub Environment protection rules zijn een GitHub-beheerderstaak.                                        | Stel Required reviewers in via GitHub → Environments → production.                            |
| CICD-10  | SSH host key TOFU via `ssh-keyscan`                 |   8    | ✅ Gemitigeerd in sprint 2: `ssh-keyscan` vervangen door vaste host key secrets.                           | —                                                                                             |
| CICD-11  | Gedeelde Slack webhook                              |   4    | Score 4 — laag risico. Afsplitsen vereist aanpassingen in de Slack-workspace van de opdrachtgever.         | Maak separate Slack webhooks aan per omgeving bij volgende Slack workspace review.            |
| CICD-13  | Verlies toegang repository-eigenaar                 |   8    | GitHub repository-eigenaarsbeheer is een organisatie-governancekwestie.                                    | Zorg voor minimaal 2 owners op de GitHub-organisatie.                                         |
| CICD-14  | `main` push triggert productie-deploy               |   15   | Aanpassen vereist herinrichting van de branching-strategie en GitHub Environment protection rules.         | Verwijder de `main`-push trigger; laat productie uitsluitend via `workflow_dispatch` lopen.   |
| Platform | OpenMRS 1.9.9 EOL + bekende CVE's                   | n.v.t. | OpenMRS platformversie wordt bepaald door de opdrachtgever/zorginstelling.                                 | Upgrade naar OpenMRS 2.x LTS.                                                                 |
| Platform | Java 7 runtime (EOL) in Docker                      | n.v.t. | Java 7 is EOL maar technisch vereist door OpenMRS 1.9.9.                                                   | Upgrade platform naar OpenMRS 2.x (Java 8+ compatible) als eerste stap.                       |
| Platform | Geen versleuteling at rest database                 | n.v.t. | MySQL-encryptie-inrichting is een server- en DBA-verantwoordelijkheid.                                     | Activeer MySQL encryption-at-rest op de databaseserver.                                       |
| Platform | HTTP in plaats van HTTPS op test/acceptatie         | n.v.t. | TLS-certificaatbeheer voor test/acceptatieservers is een infrastructuurverantwoordelijkheid.               | Installeer een Let's Encrypt certificaat op test/acceptatieservers.                           |

---

## 6. Samenvatting

### In scope — te realiseren

| Prioriteit        | Bevindingen                                 | Aanpak                      |
| ----------------- | ------------------------------------------- | --------------------------- |
| P1 — Kritiek (🔴) | R01, R02, R03, R04                          | Oplossen in huidige sprint  |
| P2 — Hoog (🟠)    | R05, R07, R09, R11, **R14**                 | Oplossen in volgende sprint |
| P3 — Midden (🟡)  | CICD-06                                     | Oplossen binnen 2 sprints   |
| ✅ Opgelost       | CICD-03, CICD-04, CICD-05, CICD-07, CICD-10 | Afgerond in sprint 2        |

### Buiten scope — gedocumenteerd

| Reden                                                       | Bevindingen                                 |
| ----------------------------------------------------------- | ------------------------------------------- |
| Platform/OpenMRS core (niet aanpasbaar vanuit module)       | R06, R08, Platform-risico's                 |
| GitHub organisatiebeheer (niet aanpasbaar zonder org-admin) | CICD-01, CICD-02, CICD-09, CICD-13, CICD-14 |
| ✅ Reeds gemitigeerd in sprint 2                            | CICD-07, CICD-08 (deels), CICD-10           |
| Laag risico / onderhoudsissue                               | R10, R12, CICD-11                           |

### Restrisico na volledige uitvoering

Na implementatie van alle P1-P3 maatregelen — inclusief R14 (SQL Injection) — resteert een restrisico op:

- Platformniveau (OpenMRS EOL, Java 7, geen MFA) — verantwoordelijkheid opdrachtgever
- Organisatieniveau (GitHub governance, environment protection) — verantwoordelijkheid projectbeheerder
- Infrastructuurniveau (TLS, DB-encryptie) — verantwoordelijkheid systeembeheerder

> **Conform NEN-7510 A.5.1:** Dit restrisico is bewust geaccepteerd en gedocumenteerd.
