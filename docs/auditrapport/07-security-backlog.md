# 09 Security Backlog - OpenMRS Appointment Scheduler

**Sprint:** 2
**Taak:** SOF-38: Security backlog
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Datum:** Juni 2026
**Norm:** NEN-7510:2024

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
| R01 | PII in logbestanden                                          |  25   | 🔴 Rood   | 🔴 Open | P1 — Kritiek | A.8.15   | Dev-team |
| R02 | Hardcoded credentials in broncode                            |  20   | 🔴 Rood   | 🔴 Open | P1 — Kritiek | A.9.2    | Dev-team |
| R03 | Geen data-level ACL                                          |  16   | 🔴 Rood   | 🔴 Open | P1 — Kritiek | A.8.3    | Dev-team |
| R04 | Typfouten in privilege-constanten                            |  15   | 🔴 Rood   | ✅ Opgelost | P1 — Kritiek | A.8.3    | Dev-team |
| R05 | Lege `@Authorized()` op servicemethoden                      |  12   | 🟠 Oranje | ✅ Opgelost | P2 — Hoog    | A.8.3    | Dev-team |
| R07 | Geen auditlogging voor afspraakmutaties                      |  12   | 🟠 Oranje | 🔴 Open | P2 — Hoog    | A.8.15   | Dev-team |
| R11 | `retireAppointmentType` / `voidAppointment` zet vlag niet    |  12   | 🟠 Oranje | 🔴 Open | P2 — Hoog    | A.8.6    | Dev-team |
| R09 | `ConcurrentModificationException` in `cleanOpenAppointments` |   9   | 🟠 Oranje | 🔴 Open | P2 — Hoog    | A.8.6    | Dev-team |

### 3.2 CI/CD pipeline bevindingen

| ID      | Bevinding                                              | Score | Zone      | Status            | Prioriteit   | NEN-7510 | Eigenaar |
| ------- | ------------------------------------------------------ | :---: | --------- | ----------------- | ------------ | -------- | -------- |
| CICD-03 | `continue-on-error` SonarQube — SAST blokkeert nooit   |  20   | 🔴 Rood   | 🟡 In behandeling | P1 — Kritiek | A.8.25   | Dev-team |
| CICD-04 | Geen geheimscanner (gitleaks) in pipeline              |  20   | 🔴 Rood   | 🟡 In behandeling | P1 — Kritiek | A.8.25   | Dev-team |
| CICD-05 | Geen SCA / CVE-scan op dependencies en container image |  16   | 🟠 Oranje | 🟡 In behandeling | P2 — Hoog    | A.8.8    | Dev-team |
| CICD-06 | Niet-gepinde GitHub Actions (`@v4`, `@v6`)             |  10   | 🟡 Geel   | 📋 Gepland        | P3 — Midden  | A.8.9    | Dev-team |

---

## 4. Gedetailleerde acties per in-scope bevinding

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

**Restrisico na fix:** Midden — roldefinitie en "behandelaarrelatie" zijn afhankelijk van correcte OpenMRS-gebruikersbeheerinrichting buiten de module.

---

### R04 — Typfouten in privilege-constanten

**Bestand:** `api/src/main/java/.../AppointmentUtils.java`, regels 29–31

**Probleem:**

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Scedules";   // ❌
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Scedules"; // ❌
```

**Actie:** Herstel de spelling en voeg een test toe:

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Schedules";   // ✅
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Schedules"; // ✅
```

Voeg ook een test toe die verifieert dat elke privilege-constante in `AppointmentUtils` overeenkomt met een `<privilege>`-registratie in `config.xml`:

```java
@Test
public void allPrivilegeConstantsShouldBeRegisteredInConfig() {
    // laad config.xml, vergelijk alle PRIV_* constanten
}
```

**Acceptatiecriterium:** Test slaagt; privilege-checks voor provider-roosters werken correct in integratie.

---

### R05 — Lege `@Authorized()` op servicemethoden

**Bestand:** `api/src/main/java/.../api/AppointmentService.java`

**Probleem:** `getAllProviderSchedules()` en `getProviderScheduleByUuid()` hebben `@Authorized()` zonder privilege-argument, waardoor elke gebruiker ze kan aanroepen.

**Actie:**

```java
// Voor:
@Authorized()
List<ProviderSchedule> getAllProviderSchedules();

// Na:
@Authorized(AppointmentUtils.PRIV_VIEW_PROVIDER_SCHEDULES)
List<ProviderSchedule> getAllProviderSchedules();
```

**Acceptatiecriterium:** Een gebruiker zonder het `View Provider Schedules`-privilege krijgt een `APIAuthenticationException` bij het aanroepen van deze methoden.

---

### R07 — Geen auditlogging voor afspraakmutaties

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`

**Probleem:** `saveAppointment()`, `voidAppointment()`, `cancelAppointment()` en `changeMatchingAppointment()` loggen geen gestructureerde auditinformatie.

**Actie:** Voeg een gestructureerd auditlog toe op service-methode niveau. Gebruik uitsluitend UUID's — geen PII:

```java
// Voorbeeld voor saveAppointment:
log.info("[AUDIT] appointment.save | user={} | appointmentUuid={} | action={}",
    Context.getAuthenticatedUser().getUuid(),
    appointment.getUuid(),
    appointment.getId() == null ? "CREATE" : "UPDATE");
```

**Minimum te loggen velden per mutatie:**

| Veld           | Voorbeeld                                     |
| -------------- | --------------------------------------------- |
| Tijdstip       | automatisch via logging framework             |
| Gebruiker UUID | `Context.getAuthenticatedUser().getUuid()`    |
| Actie          | CREATE / UPDATE / VOID / CANCEL / RETIRE      |
| Resource type  | `Appointment`, `AppointmentBlock`, `TimeSlot` |
| Resource UUID  | `object.getUuid()`                            |

**Acceptatiecriterium:** Na elke mutatie-aanroep verschijnt een `[AUDIT]`-regel in de log zonder PII. Gevalideerd door unit test.

---

### R09 — ConcurrentModificationException in `cleanOpenAppointments`

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`, regels 961–988

**Probleem:** Elementen worden verwijderd via `list.remove(item)` terwijl een `Iterator` actief is over dezelfde lijst.

**Actie:** Vervang de directe verwijdering door `iterator.remove()`:

```java
// Voor:
for (Appointment appointment : appointmentsInStates) {
    if (shouldRemove(appointment)) {
        appointmentsInStates.remove(appointment); // ❌ ConcurrentModificationException
    }
}

// Na (optie A — iterator.remove):
Iterator<Appointment> it = appointmentsInStates.iterator();
while (it.hasNext()) {
    Appointment appointment = it.next();
    if (shouldRemove(appointment)) {
        it.remove(); // ✅
    }
}

// Na (optie B — collect-then-remove):
List<Appointment> toRemove = appointmentsInStates.stream()
    .filter(this::shouldRemove)
    .collect(Collectors.toList());
appointmentsInStates.removeAll(toRemove); // ✅
```

**Acceptatiecriterium:** Unit test dekt het scenario waarbij een afspraak wordt opgeruimd zonder exception.

---

### R11 — `retireAppointmentType` / `voidAppointment` zet vlag niet

**Bestand:** `api/src/main/java/.../api/impl/AppointmentServiceImpl.java`

**Probleem:** Vier methoden slaan op zonder de bijbehorende status-vlag te zetten:

| Methode                                | Ontbrekende actie                                                 |
| -------------------------------------- | ----------------------------------------------------------------- |
| `retireAppointmentType(type, reason)`  | `type.setRetired(true); type.setRetireReason(reason);`            |
| `voidAppointment(appointment, reason)` | `appointment.setVoided(true); appointment.setVoidReason(reason);` |
| `voidTimeSlot(timeSlot, reason)`       | `timeSlot.setVoided(true); timeSlot.setVoidReason(reason);`       |
| `voidAppointmentBlock(block, reason)`  | `block.setVoided(true); block.setVoidReason(reason);`             |

**Actie:** Voeg de vlagzetting toe vóór elke `save`-aanroep in elk van de vier methoden.

**Acceptatiecriterium:** Unit tests bewijzen dat na aanroep van elke methode de vlag `true` is en de reden is opgeslagen.

---

### CICD-03 — SonarQube `continue-on-error` verwijderd

**Bestand:** `.github/workflows/pipeline.yml`

**Probleem:** `continue-on-error: true` op de SonarQube-stap maakt de SAST-controle decoratief.

**Actie:** ✅ Reeds geïmplementeerd in sprint 2 — pipeline is opgesplitst:

- `develop` branch: `continue-on-error: true` (waarschuwing)
- `release/*` en `main`: geen `continue-on-error` (hard gate)

**Acceptatiecriterium:** Een mislukt quality gate op `release/*` blokkeert de build en voorkomt deploy naar acceptatie.

---

### CICD-04 — Gitleaks secret scanning toegevoegd

**Bestand:** `.github/workflows/pipeline.yml`

**Actie:** ✅ Reeds geïmplementeerd in sprint 2 — `secret-scan` job toegevoegd als eerste job; `build-and-test` is afhankelijk van `secret-scan`.

**Acceptatiecriterium:** Een commit met een hardcoded wachtwoord blokkeert de pipeline bij de `secret-scan`-stap voordat `build-and-test` start.

**Suppression-proces:** Zie `.gitleaks.toml` en `docs/auditrapport/10-cicd-risico-evaluatie.md` §5.

---

### CICD-05 — SCA / CVE-scan toegevoegd (OWASP + Trivy)

**Bestand:** `.github/workflows/pipeline.yml`

**Actie:** ✅ Reeds geïmplementeerd in sprint 2:

- OWASP Dependency Check op Maven-dependencies (`mvn dependency-check:check`, blokkeert bij CVSS ≥ 7)
- Trivy container image scan (blokkeert bij CRITICAL/HIGH op `release/*` en `main`)

**Suppression-proces:** Zie `owasp/suppressions.xml`, `.trivyignore` en `docs/auditrapport/10-cicd-risico-evaluatie.md` §5.

---

### CICD-06 — Niet-gepinde GitHub Actions

**Bestand:** `.github/workflows/pipeline.yml`

**Actie (gepland, volgende sprint):** Pin alle externe Actions op volledige SHA-hashes in plaats van floating tags:

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

De onderstaande bevindingen zijn geïdentificeerd maar vallen buiten de scope van dit project. Ze zijn gedocumenteerd zodat een toekomstige eigenaar of opdrachtgever ze kan oppakken.

| ID       | Bevinding                                           | Score  | Reden buiten scope                                                                                                                                                                                                                 | Aanbeveling                                                                                                       |
| -------- | --------------------------------------------------- | :----: | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| R06      | Geen MFA op OpenMRS-instantie                       |   12   | MFA is een OpenMRS-platforminstelling, niet configureerbaar vanuit de module. Verantwoordelijkheid van de systeembeheerder van de zorginstelling.                                                                                  | Stel in de OpenMRS-beheerinterface "Require 2FA" in; gebruik een OAuth2/SAML identity provider.                   |
| R08      | Geen brute-force beveiliging                        |   12   | Account lockout en rate limiting zijn OpenMRS core-functionaliteit en reverse proxy-instellingen. Buiten de scope van de module-code.                                                                                              | Configureer account lockout in OpenMRS runtime properties; voeg IP-rate limiting toe op nginx/Apache.             |
| R10      | Gebruik van deprecated `Date`-API                   |   6    | Technische schuld met laag beveiligingsrisico (score 6). Betreft uitsluitend onderhoudbaarheid. Geen directe patiëntdatablootstelling.                                                                                             | Migreer naar `java.time` bij een Java-versie-upgrade; opnemen in technische-schuld-backlog.                       |
| R12      | Ongebruikte variabele `satisfyingConstraints`       |   2    | Score 2 — verwaarloosbaar risico. Betreft codekwaliteit, geen beveiligingsimpact.                                                                                                                                                  | Verwijder de variabele bij een volgende code-review.                                                              |
| CICD-01  | GitHub beheerderaccount gecompromitteerd            |   15   | MFA-afdwinging op GitHub-organisatieniveau is een beheerderstaak op GitHub-organisatieniveau, buiten de module-scope. Het team kan dit niet zelf afdwingen zonder organisatiebeheerderrechten.                                     | Stel MFA verplicht via GitHub Org → Settings → Authentication → Require 2FA voor alle members.                    |
| CICD-02  | GitHub Secrets gelekt via logs                      |   15   | Secretbeheer en rotatieschema zijn een GitHub-organisatie- en DevOps-verantwoordelijkheid. De pipeline-code zelf is aangepast (SSH-agent i.p.v. echo naar schijf).                                                                 | Stel een secrets-rotatieschema in; gebruik OIDC voor cloud-deployments als alternatief voor SSH keys.             |
| CICD-07  | SSH private key tijdelijk op runner-schijf          |   12   | ✅ Reeds gemitigeerd in sprint 2 via `webfactory/ssh-agent` — key staat niet meer op schijf. Restrisico is geaccepteerd.                                                                                                           | —                                                                                                                 |
| CICD-08  | Geen geformaliseerde rollback procedure             |   15   | ✅ Gedeeltelijk gemitigeerd in sprint 2: automatische rollback toegevoegd aan `deploy-prod` bij health check failure. Geen volledige blue-green strategie — buiten scope van huidige sprint door serverinfrastructuur-beperkingen. | Implementeer blue-green deployment wanneer een tweede productieserver beschikbaar is.                             |
| CICD-09  | Geen verplichte reviewer voor productie-environment |   15   | GitHub Environment protection rules zijn een GitHub-beheerderstaak. Het team kan environment-settings niet afdwingen vanuit code.                                                                                                  | Stel in GitHub → Environments → production → Required reviewers in met minimaal 1 extra teamlid.                  |
| CICD-10  | SSH host key TOFU via `ssh-keyscan`                 |   8    | ✅ Gemitigeerd in sprint 2: `ssh-keyscan` vervangen door vaste host key secrets (`*_SSH_KNOWN_HOST`).                                                                                                                              | —                                                                                                                 |
| CICD-11  | Gedeelde Slack webhook                              |   4    | Score 4 — laag risico. Afsplitsen van webhooks vereist aanpassingen in de Slack-workspace van de opdrachtgever, buiten de module-scope.                                                                                            | Maak separate Slack webhooks aan per omgeving bij volgende Slack workspace review.                                |
| CICD-13  | Verlies toegang repository-eigenaar                 |   8    | GitHub repository-eigenaarsbeheer is een organisatie-governancekwestie. De module-code kan hier geen invloed op uitoefenen.                                                                                                        | Zorg voor minimaal 2 owners op de GitHub-organisatie; gebruik backup-codes voor MFA.                              |
| CICD-14  | `main` push triggert productie-deploy               |   15   | In de huidige OTAP-inrichting is `main`-push bedoeld als hotfix-pad. Aanpassen vereist herinrichting van de branching-strategie en GitHub Environment protection rules — buiten scope huidige sprint.                              | Verwijder de `main`-push trigger voor `deploy-prod`; laat productie uitsluitend via `workflow_dispatch` verlopen. |
| Platform | OpenMRS 1.9.9 EOL + bekende CVE's                   | n.v.t. | OpenMRS platformversie wordt bepaald door de opdrachtgever/zorginstelling. Module-upgrades zijn afhankelijk van platform-compatibiliteit. Buiten directe projectscope.                                                             | Upgrade naar OpenMRS 2.x LTS; neem platformvulnerabilities op in een apart platform-risicoregister.               |
| Platform | Java 7 runtime (EOL) in Docker                      | n.v.t. | Java 7 is EOL maar technisch vereist door OpenMRS 1.9.9. Upgraden vereist eerst het platform te upgraden. Kip-en-ei-probleem buiten module-scope.                                                                                  | Upgrade platform naar OpenMRS 2.x (Java 8+ compatible) als eerste stap.                                           |
| Platform | Geen versleuteling at rest database                 | n.v.t. | MySQL-encryptie-inrichting is een server- en DBA-verantwoordelijkheid, niet configureerbaar vanuit de module.                                                                                                                      | Activeer MySQL encryption-at-rest (InnoDB tablespace encryption) op de databaseserver.                            |
| Platform | HTTP in plaats van HTTPS op test/acceptatie         | n.v.t. | TLS-certificaatbeheer voor test/acceptatieservers is een infrastructuurverantwoordelijkheid. Module-code heeft hier geen invloed op.                                                                                               | Installeer een Let's Encrypt certificaat op test/acceptatieservers; gebruik HTTPS ook voor health checks.         |

---

## 6. Samenvatting

### In scope — te realiseren

| Prioriteit        | Bevindingen                          | Aanpak                      |
| ----------------- | ------------------------------------ | --------------------------- |
| P1 — Kritiek (🔴) | R01, R02, R03, R04, CICD-03, CICD-04 | Oplossen in huidige sprint  |
| P2 — Hoog (🟠)    | R05, R07, R09, R11, CICD-05          | Oplossen in volgende sprint |
| P3 — Midden (🟡)  | CICD-06                              | Oplossen binnen 2 sprints   |

### Buiten scope — gedocumenteerd

| Reden                                                       | Bevindingen                                 |
| ----------------------------------------------------------- | ------------------------------------------- |
| Platform/OpenMRS core (niet aanpasbaar vanuit module)       | R06, R08, Platform-risico's                 |
| GitHub organisatiebeheer (niet aanpasbaar zonder org-admin) | CICD-01, CICD-02, CICD-09, CICD-13, CICD-14 |
| ✅ Reeds gemitigeerd in sprint 2                            | CICD-07, CICD-08 (deels), CICD-10           |
| Laag risico / onderhoudsissue                               | R10, R12, CICD-11                           |

### Restrisico na volledige uitvoering

Na implementatie van alle P1-P3 maatregelen resteert een restrisico op:

- Platformniveau (OpenMRS EOL, Java 7, geen MFA) — verantwoordelijkheid opdrachtgever
- Organisatieniveau (GitHub governance, environment protection) — verantwoordelijkheid projectbeheerder
- Infrastructuurniveau (TLS, DB-encryptie) — verantwoordelijkheid systeembeheerder

> **Conform NEN-7510 A.5.1:** Dit restrisico is bewust geaccepteerd en gedocumenteerd.
