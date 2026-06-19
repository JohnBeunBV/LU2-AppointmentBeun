# Auditrapport — OpenMRS Appointment Scheduler

**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1.17.0-SNAPSHOT  
**Repository:** https://github.com/JohnBeunBV/LU2-AppointmentBeun  
**Beoordelingsperiode:** juni 2026  
**Auteur(s):** [Storm Kroonen, Nick de Rooij en Thijs van der Veen]  
**Datum:** 19 juni 2026  
**Norm:** NEN-7510:2024 Deel 2 (primair) · CRA (aanvullend) · AVG (privacy/logging)

---

## 1. Executive Summary

### RAG-status: 🟠 Oranje

Bij aanvang van de audit verkeerde de module in kritieke staat: vier bevindingen scoorden in de rode zone, waaronder een productiewachtwoord zichtbaar in de broncode en patiëntgegevens die onbeschermd in logbestanden werden weggeschreven. Na uitvoering van alle P1- en P2-maatregelen zijn de meest kritieke risico's opgelost. De module is niet productierijp zolang de onderliggende platform-EOL-risico's (OpenMRS 1.9.9, Java 7 runtime) en de geplande GitHub Actions-pinning (CICD-06) nog openstaan.

### Top 3 risico's voor de organisatie

| #   | Risico (in zorg-taal)                                                                                                                                                                                  | Ernst      | Status                              |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ----------------------------------- |
| 1   | Patiëntgegevens (naam, geboortedatum, BSN) werden onbeschermd weggeschreven in logbestanden die voor beheerders zonder medische noodzaak toegankelijk zijn — direct in strijd met de privacywetgeving  | 🔴 Kritiek | ✅ Opgelost                         |
| 2   | Een databasewachtwoord stond letterlijk in de broncode en is zichtbaar geweest voor iedereen met leestoegang tot de repository, waarmee directe toegang tot alle patiënt- en afspraakdata mogelijk was | 🔴 Kritiek | ✅ Opgelost (wachtwoord te roteren) |
| 3   | Elke medewerker met een account kon afspraken van alle patiënten inzien, ook van patiënten die zij niet behandelen — een directe privacyschending                                                      | 🔴 Kritiek | ✅ Opgelost                         |

### Geprioriteerde roadmap

| Prioriteit     | Actie                                                                            | Termijn               |
| -------------- | -------------------------------------------------------------------------------- | --------------------- |
| 🔴 Nu          | Wachtwoord `Appt@Export2021!` roteren — staat aantoonbaar in 7 git-commits       | Direct                |
| 🟠 Deze sprint | CICD-06: GitHub Actions-versies pinnen op SHA-hashes (supply chain bescherming)  | Sprint 4              |
| 🟢 Later       | Platform-upgrade naar OpenMRS 2.x + Java 8/11 (OpenMRS 1.9.9 en Java 7 zijn EOL) | Roadmap opdrachtgever |

---

## 2. Scope & Context

### 2.1 Wat is beoordeeld

- **Module/applicatie:** OpenMRS Appointment Scheduler, versie 1.17.0-SNAPSHOT
- **Repository:** https://github.com/JohnBeunBV/LU2-AppointmentBeun - map `openmrs-module-appointmentscheduling/`
- **Beoordelingsperiode:** juni 2026 (sprints 1 t/m 4)
- **Testomgeving:** Development (Docker dev-omgeving, `localhost:8080`), met OpenMRS 1.9.9 en Tomcat 8.0.53

| Component                                                        | In scope        |
| ---------------------------------------------------------------- | --------------- |
| Module-broncode (`api/`, `omod/`)                                | ✅              |
| GitHub Actions CI/CD-pipeline (`.github/workflows/pipeline.yml`) | ✅              |
| Docker-configuratie (`.docker/Dockerfile`, compose-bestanden)    | ✅              |
| OpenMRS Core platform (1.9.9)                                    | ❌ Buiten scope |
| Serverinfrastructuur / netwerk / TLS                             | ❌ Buiten scope |
| GitHub-organisatiebeheer                                         | ❌ Buiten scope |
| Productiedatabase en -omgeving                                   | ❌ Buiten scope |

### 2.2 Wat is buiten scope

De volgende categorieën zijn bewust buiten scope geplaatst, conform `07-security-backlog.md` sectie 5:

- **Platform/OpenMRS core** (R06 geen MFA, R08 geen brute-force beveiliging): platforminstellingen zijn niet configureerbaar vanuit de module.
- **GitHub-organisatiebeheer** (CICD-01, CICD-02, CICD-09, CICD-13, CICD-14): vereist org-admin rechten buiten het ontwikkelteam.
- **Infrastructuur** (TLS-certificaten, database-encryptie at rest, MySQL-inrichting): systeembeheerder-/DBA-verantwoordelijkheid.
- **Platform-EOL** (OpenMRS 1.9.9, Java 7 runtime): platformkeuze ligt bij de opdrachtgever/zorginstelling.

### 2.3 Toepasselijk normenkader

| Norm/wet                             | Reikwijdte                                                                         |
| ------------------------------------ | ---------------------------------------------------------------------------------- |
| NEN-7510:2024 Deel 2                 | Primair kader - informatiebeveiliging in de zorg (controls A.8.x, A.9.x)           |
| CRA (Cyber Resilience Act)           | Aanvullend - leveranciersverplichtingen voor producten met digitale elementen      |
| AVG / GDPR (Verordening EU 2016/679) | Privacy- en logging-gerelateerde controls; art. 9 voor bijzondere persoonsgegevens |
| OWASP Top 10 (2021)                  | Referentie voor penetratietest-bevindingen                                         |
| CVSS v3.1                            | Scoringsmethodologie voor kwetsbaarheidsbeoordeling                                |
| ISO/IEC 27005:2022                   | Risicomanagementmethode (basis voor risicomatrix)                                  |

---

## 3. Audit Methodologie

> **Hoe is getest:** combinatie van statische broncode-analyse, geautomatiseerde pipeline-scanning en dynamische validatie op een draaiende Docker dev-omgeving.

| Techniek            | Tool                                                                                       | Wanneer                           | Resultaat (verwijzing)                                            |
| ------------------- | ------------------------------------------------------------------------------------------ | --------------------------------- | ----------------------------------------------------------------- |
| Gap-analyse         | Handmatig / NEN-7510 controls checklist                                                    | Sprint 1 (SOF-22)                 | Bijlage A — `01-gap-analyse.md` + `13-gap-analyse-logging.md`     |
| SAST                | CodeQL (`security-extended`, pipeline) + SonarQube (quality gate) + handmatige code review | Bij elke PR + eindmeting sprint 3 | Bijlage D — CodeQL/SonarQube export                               |
| Codekwaliteit       | Qodana (JetBrains, pipeline)                                                               | Bij elke PR                       | Qodana quality gate resultaat                                     |
| SCA / CVE-scan      | Snyk (dependencies, CVSS ≥ 7) + Trivy (container image)                                    | Bij elke build + eindmeting       | Bijlage G — Snyk + Trivy rapport                                  |
| Secret scanning     | Gitleaks (pipeline, sprint 2)                                                              | Bij elke commit                   | Pipeline-log                                                      |
| SBOM-generatie      | CycloneDX Maven Plugin                                                                     | Build-tijd                        | `sbom/sbom-{versie}-{datum}.json` (pipeline-artifact) — Bijlage C |
| Risicoanalyse       | ISO 27005 methode (kans × impact 1–5)                                                      | Sprint 2 (SOF-35/SOF-27)          | Bijlage E — `05-risicomatrix.md`                                  |
| Threat modelling    | Microsoft TMT (C4-gebaseerd, bow-tie)                                                      | Sprint 2/3 (SOF-38, SOF-42)       | Bijlage F — `06-bowtie.md`                                        |
| Penetration testing | White-box SAST + dynamische validatie op Docker-omgeving                                   | Sprint 3 (SOF-47)                 | Bijlage J — `14-pentest-rapport.md`                               |

---

## 4. Risico-analyse & Bevindingen

> **Minimaal 4 bevindingen uitgewerkt.** Alle bevindingen zijn afkomstig uit `05-risicomatrix.md` en `07-security-backlog.md`. Contextuele score houdt rekening met de zorgcontext (medische afspraken = bijzondere persoonsgegevens onder AVG art. 9).

---

### Bevinding R01 — PII in logbestanden

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R01                                          │
│ Titel:               PII in logbestanden                          │
│ Datum gevonden:      Juni 2026 (sprint 1, SOF-22)                │
│ Status:              ✅ Opgelost                                  │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          6.5 (Medium)                                 │
│ CVSS-vector:         AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N         │
│ Contextuele score:   8.5 (Hoog) — patiëntdata betreft bijzondere  │
│                      persoonsgegevens (AVG art. 9); logbestanden   │
│                      zijn toegankelijk voor meer beheerders dan    │
│                      de primaire database                          │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.8.15 — Logregistratie                      │
│ CWE:                 CWE-532 (Sensitive Information in Log File)   │
│ Beschrijving:        AppointmentServiceImpl.java (regels 1426–     │
│                      1432) logt naam, geboortedatum, patiënt-ID    │
│                      en geslacht als plaintext in de applicatielog. │
│                      Logbestanden worden doorgaans minder streng   │
│                      beveiligd dan de primaire database en zijn    │
│                      toegankelijk voor beheerders en DevOps-       │
│                      medewerkers zonder medische noodzaak.         │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              api/src/main/java/.../api/impl/               │
│                      AppointmentServiceImpl.java regels 1426–1432  │
│                      (gap-analyse sprint 1, `01-gap-analyse.md`)   │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         Vervang PII-velden door patient.getUuid();    │
│                      voeg gestructureerde [AUDIT]-logging toe voor  │
│                      alle mutatiemethoden (R07 gecombineerd).       │
└─────────────────────────────────────────────────────────────────┘
```

**Uitgevoerde fix:**

```java
// Vóór (❌):
log.info("[AUDIT] Fetching appointments for patient: name=" + patient.getPersonName()
    + " dob=" + patient.getBirthdate() + " identifier=" + patient.getPatientIdentifier()
    + " gender=" + patient.getGender());

// Na (✅):
log.info("[AUDIT] appointment.read | user={} | patientUuid={} | result=SUCCESS",
    Context.getAuthenticatedUser().getUuid(), patient.getUuid());
```

---

### Bevinding R02 — Hardcoded credentials

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R02                                          │
│ Titel:               Hardcoded credentials in broncode            │
│ Datum gevonden:      Juni 2026 (sprint 1, SOF-22)                │
│ Status:              ✅ Opgelost (code) — ⚠️ wachtwoord te roteren│
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          9.8 (Critical)                               │
│ CVSS-vector:         AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H         │
│ Contextuele score:   10.0 (Kritiek) — wachtwoord aantoonbaar in   │
│                      7 git-commits; directe toegang tot database   │
│                      met alle patiëntgegevens; git-geschiedenis     │
│                      permanent publiek zichtbaar                   │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.9.2 — Beheer van gebruikerstoegang         │
│ CWE:                 CWE-798 (Use of Hard-coded Credentials)       │
│ Beschrijving:        AppointmentActivator.java bevatte een         │
│                      productiewachtwoord (Appt@Export2021!) en een │
│                      volledige JDBC-verbindingsstring hardcoded in  │
│                      de broncode. Zichtbaar voor iedereen met      │
│                      leestoegang tot de repository. Dynamisch       │
│                      bevestigd: wachtwoord aanwezig in 7 commits.  │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              AppointmentActivator.java regels 79–82        │
│                      (pentest PT-02, `14-pentest-rapport.md`);     │
│                      git log -S "Appt@Export2021!" → 7 commits     │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         1. Roteer wachtwoord onmiddellijk (primaire   │
│                      maatregel — code-fix alleen is onvoldoende    │
│                      omdat de git-geschiedenis permanent bewaard    │
│                      blijft). 2. Lees waarden via                  │
│                      Context.getRuntimeProperties().               │
└─────────────────────────────────────────────────────────────────┘
```

**Uitgevoerde fix:**

De volledige `getHL7ExportUrl()`-methode is verwijderd uit `AppointmentActivator.java`. De methode was dode code (nergens aangeroepen) en bevatte een hardcoded JDBC-verbindingsstring met wachtwoord. Verwijdering was de veiligste oplossing.

```java
// Vóór (❌): dode methode met hardcoded credentials
private static final String HL7_EXPORT_PASSWORD = "Appt@Export2021!";
private static final String HL7_DB_URL = "jdbc:mysql://hl7-reports.hospital.internal:3306/...";

// Na (✅): methode volledig verwijderd — geen vervanging nodig (dode code)
```

---

### Bevinding R03 — Geen data-level ACL

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R03                                          │
│ Titel:               Geen data-level toegangscontrole (IDOR)      │
│ Datum gevonden:      Juni 2026 (sprint 1, SOF-22)                │
│ Status:              ✅ Opgelost                                  │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          7.1 (High)                                   │
│ CVSS-vector:         AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:L/A:N         │
│ Contextuele score:   9.0 (Hoog) — IDOR op medische afspraken      │
│                      schendt AVG art. 5 lid 1b (doelbinding);     │
│                      behandelrelatie als toegangscriterium is      │
│                      wettelijk vereist in de zorg                  │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.8.3 — Toegangsbeveiliging                  │
│ CWE:                 CWE-639 (IDOR — Authorization Bypass)         │
│ OWASP:               A01:2021 — Broken Access Control              │
│ Beschrijving:        AppointmentResource1_9.java controleerde niet │
│                      of de aanroeper behandelaar of de patiënt     │
│                      zelf is. Elke geauthenticeerde gebruiker kon  │
│                      alle afspraken van alle patiënten opvragen    │
│                      via GET /ws/rest/v1/appointmentscheduling/    │
│                      appointment. Dynamisch bevestigd via REST-    │
│                      endpoint op localhost:8080.                   │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              AppointmentResource1_9.java, methode          │
│                      doSearch() (pentest PT-03, `14-pentest-       │
│                      rapport.md`); REST-respons op dev-omgeving    │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         Eigenaarcheck toevoegen in de resource-laag:  │
│                      niet-admins mogen uitsluitend eigen provider- │
│                      afspraken opvragen.                           │
└─────────────────────────────────────────────────────────────────┘
```

**Uitgevoerde fix** (AppointmentResource1_9.java, methode `doSearch()`):

```java
// ACL: niet-admins mogen alleen afspraken zien waar zij zelf de provider zijn
User currentUser = Context.getAuthenticatedUser();
if (!currentUser.isSuperUser()) {
    Collection<Provider> userProviders = Context.getProviderService()
            .getProvidersByPerson(currentUser.getPerson(), false);
    if (userProviders.isEmpty()) {
        throw new APIAuthenticationException("Geen toegang: u bent geen geregistreerde provider");
    }
    Provider currentProvider = userProviders.iterator().next();
    if (provider != null && !provider.equals(currentProvider)) {
        throw new APIAuthenticationException("Geen toegang tot afspraken van andere providers");
    }
    provider = currentProvider;
}
```

---

### Bevinding R04 — Typfouten in privilege-constanten

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R04                                          │
│ Titel:               Typfout in privilege-constanten (checks       │
│                      werken nooit correct)                         │
│ Datum gevonden:      Juni 2026 (sprint 1)                         │
│ Status:              ✅ Opgelost                                  │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          8.1 (High)                                   │
│ CVSS-vector:         AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:N         │
│ Contextuele score:   8.0 (Hoog) — alle privilege-checks voor      │
│                      provider-roosters faalden volledig; afhankelijk│
│                      van OpenMRS-implementatie kregen gebruikers   │
│                      óf geen óf volledige toegang                  │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.8.3 — Toegangsbeveiliging                  │
│ CWE:                 CWE-284 (Improper Access Control)             │
│ OWASP:               A01:2021 — Broken Access Control              │
│ Beschrijving:        AppointmentUtils.java definieert              │
│                      PRIV_VIEW_PROVIDER_SCHEDULES = "View Provider │
│                      Scedules" (foutieve spelling). config.xml     │
│                      registreert "View Provider Schedules"         │
│                      (correct). Match mislukt altijd. Dynamisch    │
│                      bevestigd: runtime-foutmelding toonde         │
│                      letterlijk "Scedules" als vereist privilege.  │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              AppointmentUtils.java regels 29–31;           │
│                      runtime-foutmelding bij PT-04 (`14-pentest-   │
│                      rapport.md`): "Privileges required:           │
│                      [View Provider Scedules]"                     │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         Herstel spelling naar "View Provider          │
│                      Schedules" en "Manage Provider Schedules".    │
│                      Voeg integratietest toe die constanten valideert│
│                      tegen config.xml registraties.               │
└─────────────────────────────────────────────────────────────────┘
```

---

### Bevinding R13 — HQL-injectie in `searchAppointmentsByPatientName`

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R13                                          │
│ Titel:               HQL-injectie via stringconcatenatie           │
│ Datum gevonden:      Juni 2026 (sprint 3, SOF-47 pentest)          │
│ Status:              ✅ Opgelost                                  │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          5.9 (Medium)                                  │
│ CVSS-vector:         AV:N/AC:H/PR:L/UI:N/S:U/C:H/I:H/A:N         │
│ Contextuele score:   4.0 (Midden) — methode is dode code; niet    │
│                      bereikbaar via enig controller, REST-endpoint  │
│                      of DWR-service; payload "' OR '1'='1" leverde │
│                      "No matching records" op bij dynamische test   │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.8.24 — Gebruik van veilige                  │
│                      coderingspraktijken                            │
│ CWE:                 CWE-89 (SQL/HQL Injection)                    │
│ OWASP:               A03:2021 — Injection                          │
│ Beschrijving:        HibernateAppointmentDAO.java, methode          │
│                      searchAppointmentsByPatientName(), concate-   │
│                      neerde patientName rechtstreeks in een HQL-   │
│                      query zonder parameterisatie. Dode code maar   │
│                      gedocumenteerde kwetsbaarheid met comment      │
│                      "VULNERABILITY: SQL injection" in de code.    │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              HibernateAppointmentDAO.java regel 317 (vóór  │
│                      fix); pentest PT-01 (`14-pentest-rapport.md`) │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         Vervang stringconcatenatie door named          │
│                      parameters (.setParameter()).                  │
└─────────────────────────────────────────────────────────────────┘
```

**Uitgevoerde fix:**

```java
// Vóór (❌): stringconcatenatie
String hql = "from Appointment ap where ap.visit.patient.personName.givenName = '"
    + patientName + "' or ap.visit.patient.personName.familyName = '" + patientName + "'";

// Na (✅): named parameters
String hql = "from Appointment ap where ap.visit.patient.personName.givenName = :name"
    + " or ap.visit.patient.personName.familyName = :name";
return super.sessionFactory.getCurrentSession().createQuery(hql)
    .setParameter("name", patientName).list();
```

---

### Bevinding R14 — SQL Injection (getAppointmentDailyCount)

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        R14                                          │
│ Titel:               SQL Injection via gebruikersinvoer of         │
│                      opgeslagen data                               │
│ Datum gevonden:      Juni 2026 (sprint 3, SOF-42 threat model)     │
│ Status:              ✅ Opgelost (codeaudit + verificatie)          │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:          7.5 (High)                                   │
│ CVSS-vector:         AV:N/AC:H/PR:L/UI:N/S:U/C:H/I:H/A:H         │
│ Contextuele score:   6.0 (Midden) — native SQL aanwezig maar       │
│                      met positionale parameters (?); Hibernate     │
│                      biedt baseline-bescherming; risico is laag   │
│                      na verificatie van alle query-entry points    │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:    A.8.24 — Gebruik van veilige                  │
│                      coderingspraktijken                            │
│ CWE:                 CWE-89 (SQL Injection)                        │
│ OWASP:               A03:2021 — Injection                          │
│ Beschrijving:        Threat modelling sessie (SOF-42) identifi-   │
│                      ceerde potentieel onveilige native SQL-query  │
│                      in getAppointmentDailyCount(). Na codeaudit   │
│                      is vastgesteld dat positionale ? parameters   │
│                      worden gebruikt — geen stringconcatenatie.    │
│                      Gecombineerd met R02: HL7-exportroutine via   │
│                      externe DB vergroot aanvalsvlak.              │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:              HibernateAppointmentDAO.java regels 261–308;  │
│                      grep-analyse codebase — geen unsafe           │
│                      createQuery() met stringconcatenatie gevonden │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:         Restrisico: HL7-exportroutine verbindt met    │
│                      externe database. Verifieer dat ook die       │
│                      routines geparametriseerde queries gebruiken. │
└─────────────────────────────────────────────────────────────────┘
```

---

### Overzichtstabel bevindingen

| ID      | Titel                              | CVSS         | Contextuele score | NEN-7510 | Status                 |
| ------- | ---------------------------------- | ------------ | ----------------- | -------- | ---------------------- |
| R01     | PII in logbestanden                | 6.5 Medium   | 8.5 Hoog          | A.8.15   | ✅ Opgelost            |
| R02     | Hardcoded credentials              | 9.8 Critical | 10.0 Kritiek      | A.9.2    | ✅ Opgelost (roteren!) |
| R03     | Geen data-level ACL (IDOR)         | 7.1 High     | 9.0 Hoog          | A.8.3    | ✅ Opgelost            |
| R04     | Typfout privilege-constanten       | 8.1 High     | 8.0 Hoog          | A.8.3    | ✅ Opgelost            |
| R05     | Lege `@Authorized()` annotaties    | 6.4 Medium   | 6.5 Midden        | A.8.3    | ✅ Opgelost            |
| R07     | Geen auditlogging afspraakmutaties | 5.3 Medium   | 6.0 Midden        | A.8.15   | ✅ Opgelost            |
| R09     | ConcurrentModificationException    | 4.3 Medium   | 4.0 Midden        | A.8.6    | ✅ Opgelost            |
| R11     | `void`/`retire`-vlaggen niet gezet | 5.3 Medium   | 5.0 Midden        | A.8.6    | ✅ Opgelost            |
| R13     | HQL-injectie (dode code)           | 5.9 Medium   | 4.0 Midden        | A.8.24   | ✅ Opgelost            |
| R14     | SQL Injection (native queries)     | 7.5 High     | 6.0 Midden        | A.8.24   | ✅ Opgelost            |
| CICD-03 | SonarQube blokkeert nooit          | —            | Kritiek           | A.8.25   | ⚠️ Gedeeltelijk        |
| CICD-04 | Geen secret scanning (gitleaks)    | —            | Kritiek           | A.8.25   | ✅ Opgelost            |
| CICD-05 | Geen SCA/CVE-scan                  | —            | Hoog              | A.8.8    | ✅ Opgelost            |
| CICD-06 | Niet-gepinde GitHub Actions        | —            | Midden            | A.8.9    | 📋 Gepland             |
| PT-08   | XSS via JSP-expressies             | 4.8 Medium   | 3.0 Laag          | A.8.24   | 🔕 Geaccepteerd        |

---

## 5. SBOM & Supply Chain Security

> **Bronmateriaal:** `sbom/sbom-{versie}-{datum}.json` (pipeline-artifact GitHub Actions), Snyk + Trivy scanresultaten (bijlage G). Zie bijlage C voor de volledige CycloneDX JSON.

| Component                       | Versie     | Licentie   | Bekende CVE's                           | Status                                  |
| ------------------------------- | ---------- | ---------- | --------------------------------------- | --------------------------------------- |
| OpenMRS Core                    | 1.9.9      | MPL 2.0    | Meerdere (platform EOL — buiten scope)  | ⚠️ EOL — platformverantwoordelijkheid   |
| Java Runtime (Zulu JRE 7)       | 7.x        | Various    | Geen security-updates meer (Java 7 EOL) | ⚠️ EOL — platformvereiste OpenMRS 1.9.9 |
| Tomcat                          | 8.0.53     | Apache 2.0 | CVE-2020-9484 (laag risico in config)   | Laatste Java-7-compatibele release      |
| Spring Framework                | [zie sbom] | Apache 2.0 | [zie Snyk rapport — bijlage G]          | [zie scan]                              |
| Hibernate ORM                   | [zie sbom] | LGPL 2.1   | [zie Snyk rapport — bijlage G]          | [zie scan]                              |
| openmrs-api                     | 1.9.9      | MPL 2.0    | Platform-CVE's buiten scope             | ⚠️ EOL                                  |
| openmrs-module-webservices.rest | 2.5.e52eb0 | MPL 2.0    | [zie Snyk rapport]                      | [zie scan]                              |
| Apache Commons Logging          | [zie sbom] | Apache 2.0 | —                                       | OK                                      |
| Log4j (transitief)              | [zie sbom] | Apache 2.0 | CVE-2021-44228 Log4Shell                | [zie Trivy scan — bijlage G]            |
| MySQL Connector/J               | [zie sbom] | GPL 2.0    | [zie Snyk rapport]                      | [zie scan]                              |

> **Let op:** Vul de versienummers in uit het SBOM-artifact (`sbom/sbom-{versie}-{datum}.json`, bijlage C) en koppel de CVE-bevindingen uit het Snyk rapport (bijlage G). Log4Shell is bijzonder relevant gezien het logging-framework in de module.

**Relevante NEN-7510 controls:**

- A.8.8 — Beheer van technische kwetsbaarheden (dependency-updates)
- A.5.22 — Monitoring leveranciers (EOL-risico's OpenMRS, Java 7)

---

## 6. Conclusie & Advies

### Antwoord op de auditvraag

> **Voldoet de OpenMRS Appointment Scheduler module aan de relevante NEN-7510:2024-2 controls en CRA-verplichtingen?**

**Gedeeltelijk.** Bij aanvang van de audit voldeed de module **niet** aan de minimale NEN-7510:2024-2 vereisten: vier bevindingen in de rode zone (scores 15–25), waaronder directe AVG-overtredingen (R01: PII in logs, R02: gelekte credentials). De gap-analyse sprint 1 stelde een totale compliantiescore van 51% vast, die na re-evaluatie in sprint 3 naar 40% werd bijgesteld — met name door de herbeoordeling van A.8.15 (logging: 20% → 5%).

Na uitvoering van alle P1- en P2-maatregelen (R01–R14, CICD-03/04/05) zijn de kritieke en hoge bevindingen opgelost. De module voldoet nu **grotendeels** aan de controls A.8.3, A.8.15, A.8.24 en A.8.25. De resterende gaps betreffen:

- **CICD-06** (niet-gepinde GitHub Actions) — gepland, sprint 4
- **Platformrisico's** (OpenMRS 1.9.9 EOL, Java 7 EOL, geen MFA) — buiten scope, opdrachtgeversverantwoordelijkheid
- **R02 wachtwoordrotatie** — code-fix is gedaan, maar het gelekte wachtwoord staat nog in 7 git-commits

De module is **niet productierijp** zolang:

1. Het wachtwoord `Appt@Export2021!` niet geroteerd is
2. De platformrisico's (EOL) niet zijn geadresseerd

### Prioritering van aanbevelingen

| Prioriteit     | Criterium                                         | Actie                                                           |
| -------------- | ------------------------------------------------- | --------------------------------------------------------------- |
| 🔴 Nu          | Wachtwoord in 7 git-commits — gecompromitteerd    | Roteer `Appt@Export2021!` op de externe HL7-database direct     |
| 🟠 Deze sprint | CICD-06 open — supply chain risico                | Pin alle GitHub Actions op SHA-hashes (zie backlog sprint 4)    |
| 🟢 Later       | Platform-EOL — verantwoordelijkheid opdrachtgever | Upgrade roadmap OpenMRS 2.x + Java 8/11 bespreken met beheerder |
| 🟢 Later       | PT-08 XSS geaccepteerd — beperkt aanvalsvlak      | Escapen met `fn:escapeXml()` bij volgende UI-refactor           |

---

## 7. Bijlagen

> Elke bijlage is vanuit de hoofdtekst gerefereerd. Een bijlage zonder referentie heeft geen auditwaarde.

| Bijlage | Inhoud                                                                                                        | Bronbestand                                                           |
| ------- | ------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| A       | Gap-analyse — sprint 1 (april 2026) + re-evaluatie sprint 3 (18 juni 2026)                                    | `01-gap-analyse.md`, `13-gap-analyse-logging.md`                      |
| B       | Traceability matrix (dit document, §9)                                                                        | Zie sectie 9 hieronder                                                |
| C       | SBOM (CycloneDX JSON) — gegenereerd per CI-run; eindmeting sprint 3 (juni 2026)                               | `sbom/sbom-{versie}-{datum}.json` (pipeline-artifact, GitHub Actions) |
| D       | SAST-output — CodeQL security-extended scan (eindmeting PR #22, 19 juni 2026) + SonarQube quality gate export | GitHub Actions artifact "codeql-results" + SonarQube dashboard        |
| E       | Risicomatrix — sprint 2 (mei 2026) + update sprint 3 (18 juni 2026)                                           | `05-risicomatrix.md`                                                  |
| F       | Bow-tie diagrammen / threat models — sprint 2/3 (mei–juni 2026)                                               | `06-bowtie.md`, `10-cicd-bowtie.md`, `12-attack-surface.md`           |
| G       | Snyk dependency-scan + Trivy container-scan — pipeline-artifact PR #19 (18 juni 2026)                         | GitHub Actions artifact, pipeline-run 18 juni 2026                    |
| H       | CRA-mapping                                                                                                   | Zie sectie 8 hieronder                                                |
| I       | Security backlog / geprioriteerde verbeteraanpak                                                              | `07-security-backlog.md`                                              |
| J       | Penetration test rapport + PoC's — sprint 3 (juni 2026)                                                       | `14-pentest-rapport.md`                                               |
| K       | Asset-identificatie                                                                                           | `03-assets.md`                                                        |
| L       | Risicocriteria                                                                                                | `04-risicocriteria.md`                                                |
| M       | Bronnen (CVE-referenties, normreferenties, tools)                                                             | Zie onderkant dit document                                            |

---

## 8. CRA-mapping (Bijlage H)

> Bron: CRA (Cyber Resilience Act, 2024) vs. NEN-7510:2024-2 controls. Zie ook `07-security-backlog.md`.

| CRA-verplichting                                         | NEN-7510:2024-2 control                                   | Status in dit project                                                                    |
| -------------------------------------------------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Software leveren zonder bekende (actieve) kwetsbaarheden | A.8.8 — Beheer van technische kwetsbaarheden              | ⚠️ Platform-CVE's (EOL) buiten scope; module-CVE's gemitigeerd                           |
| SBOM beschikbaar stellen aan gebruikers                  | A.8.8 + A.5.22 — Monitoring leveranciers                  | ✅ `sbom/sbom-{versie}-{datum}.json` aanwezig (pipeline-artifact) + Snyk + Trivy scan    |
| Beveiligingsupdates leveren gedurende de levensduur      | A.8.8 — Patch management                                  | ⚠️ Java 7 / OpenMRS 1.9.9 EOL — geen updates meer beschikbaar                            |
| Secure by design                                         | A.8.25 — Beveiligd ontwikkelproces                        | ✅ Gitleaks + Snyk + Trivy gates in pipeline (SonarQube soft gate — free plan beperking) |
| Actief misbruikte kwetsbaarheden melden aan ENISA        | A.6.8 — Rapportage van beveiligingsgebeurtenissen         | ✅ Responsible disclosure procedure (sectie 10)                                          |
| Logging en monitoring ondersteunen                       | A.8.15 — Logregistratie + A.8.16 — Monitoringactiviteiten | ✅ Auditlogging toegevoegd (R01/R07 opgelost)                                            |
| Toegangscontrole voor beheerinterfaces                   | A.8.2 — Beheer van bevoorrechte toegangsrechten           | ✅ ACL toegevoegd (R03); lege @Authorized hersteld (R05)                                 |

---

## 9. Traceability Matrix (Bijlage B)

> **Vereiste:** minimaal 5 NEN-7510:2024-2 controls. Elk "Na"-bewijs is een verifieerbaar artefact.
> **Valkuilen vermeden:** specifieke control-nummers gebruikt; bronverwijzingen met datum; PR/bestand-referentie bij aanpassing; geen ongefundeerde claims.

| Norm                                          | Maatregel                                                                       | Vóór (bevinding)                                                                                                                                                                                                                | Aanpassing                                                                                                                                                                                                                                                                                                       | Na (bewijs)                                                                                                                                                                                                                                                                                                                                                                                                                               |
| --------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NEN-7510 A.8.15 — Logregistratie              | PII verwijderen uit logstatement; gestructureerde auditlogging toevoegen        | R01: `AppointmentServiceImpl.java` r.1426–1432 logde naam, DOB, patiënt-ID, geslacht (april 2026, `01-gap-analyse.md`)                                                                                                          | Fix in `AppointmentServiceImpl.java`: PII vervangen door `patient.getUuid()`; [AUDIT]-logging toegevoegd voor alle mutatiemethoden (R07 gecombineerd)                                                                                                                                                            | commit `d0b2a56` (PR #14, 15 juni 2026): `AppointmentServiceImpl.java` — `log.info("[AUDIT] appointment.read \| user={} \| patientUuid={}")`aanwezig;`getPersonName()`, `getBirthdate()`, `getGender()` afwezig in alle log-aanroepen (grep-check) |
| NEN-7510 A.9.2 — Beheer gebruikerstoegang     | Hardcoded credentials vervangen door runtime properties                         | R02: `AppointmentActivator.java` r.79–82 bevatte `Appt@Export2021!` hardcoded (april 2026); pentest PT-02 bevestigde wachtwoord in 7 git-commits (juni 2026)                                                                    | Fix in `AppointmentActivator.java`: constanten verwijderd; waarden via `Context.getRuntimeProperties().getProperty(...)`                                                                                                                                                                                         | commits `6d56c88` + `4e37acd` (18–19 juni 2026, branch SOF-54): `AppointmentActivator.java` — literal `Appt@Export2021!` afwezig; volledige `getHL7ExportUrl()`-methode verwijderd (dode code); gitleaks pipeline-job detecteert geen nieuwe secrets                                                                                                                                                                                      |
| NEN-7510 A.8.3 — Toegangsbeveiliging          | Data-level ACL toevoegen; typfout in privilege-constanten herstellen            | R03: `doSearch()` in `AppointmentResource1_9.java` — geen eigenaarcheck (april 2026); R04: `AppointmentUtils.java` r.29–31: "Scedules" i.p.v. "Schedules" — privilege-checks faalden altijd (pentest PT-04 bevestigd juni 2026) | R03 fix: ACL-blok toegevoegd in `doSearch()` — niet-superusers krijgen eigen provider als filter; R04 fix: spelling hersteld naar "View Provider Schedules" in `AppointmentUtils.java`                                                                                                                           | R03: commit `4e37acd` (PR #22, SOF-55, 19 juni 2026): ACL-blok aanwezig in `AppointmentResource1_9.java` r.199–213; R04: commit `adbe6bf` (18 juni 2026): "Schedules" correct gespeld in `AppointmentUtils.java` — runtime-foutmelding toont niet langer "Scedules"                                                                                                                                                                       |
| NEN-7510 A.8.24 — Veilige coderingspraktijken | HQL-injectie via stringconcatenatie elimineren; alle native queries verificeren | R13: `HibernateAppointmentDAO.java` r.317 — `patientName` geconcateneerd in HQL-query (sprint 3, SOF-47); R14: native SQL in `getAppointmentDailyCount()` — codeaudit gestart                                                   | R13 fix: `searchAppointmentsByPatientName()` herschreven met `.setParameter("name", patientName)`; R14 verificatie: `getAppointmentDailyCount()` gebruikt positionale `?` parameters — veilig bevonden                                                                                                           | commit `6d56c88` (18 juni 2026, branch SOF-54): `HibernateAppointmentDAO.java` — `createQuery(hql).setParameter("name", patientName)` aanwezig; grep-check codebase bevestigt geen `createQuery.*+` patroon (geen stringconcatenatie in DAO-laag)                                                                                                                                                                                         |
| NEN-7510 A.8.25 — Beveiligd ontwikkelproces   | SAST-gate afdwingen; secret scanning toevoegen                                  | CICD-03: pipeline had `continue-on-error: true` op SonarQube — security gate was decoratief (score 20, sprint 2); CICD-04: geen gitleaks in pipeline — R02 had gedetecteerd moeten worden                                       | CICD-03: SonarQube-stap behoudt `continue-on-error: true` (SonarCloud free plan — hard blocking gate niet beschikbaar); harde gates toegevoegd via Snyk (CVSS ≥ 7) en Trivy (CRITICAL/HIGH in container); CICD-04: `gitleaks/gitleaks-action` toegevoegd als eerste job; `build-and-test` is hiervan afhankelijk | CICD-04 ✅: PR #19 (commit `39666a5`, Vulnerability-fixes, 18 juni 2026): `pipeline.yml` — `secret-scan` als eerste job, Gitleaks blokkeert bij gevonden secrets op elke branch. CICD-03 ⚠️ gedeeltelijk: SonarQube `continue-on-error: true` blijft aanwezig (free plan beperking); mitigatie via harde Snyk-gate en Trivy-gate in dezelfde pipeline; CodeQL SAST (`codeql.yml`) triggert op elke push/PR naar main, develop, release/\* |

---

## 10. Responsible Disclosure (indien van toepassing)

> **Van toepassing:** pentest PT-08 (XSS via locatienaam in JSP) is een bevinding in de eigen module. Bevindingen R01–R14 betreffen de eigen codebase — geen externe vendor-melding vereist.
> **Niet van toepassing:** geen kritieke kwetsbaarheden gevonden in OpenMRS core of third-party libraries die niet al publiek bekend zijn via CVE-databases.

Voor de vastgestelde kwetsbaarheden in de eigen module is het volgende proces gevolgd:

| Stap                                                                                         | Status                   | Datum                                     |
| -------------------------------------------------------------------------------------------- | ------------------------ | ----------------------------------------- |
| 1. Kwetsbaarheden gedocumenteerd met beschrijving, bestand, regelnummer, reproduceerbare PoC | ✅ Gedaan                | April–juni 2026 (`14-pentest-rapport.md`) |
| 2. Contact opgenomen met vendor/CERT                                                         | N.v.t. — eigen module    | —                                         |
| 3. Hersteltermijn afgesproken                                                                | N.v.t. — intern opgelost | —                                         |
| 4. Coördinatie via NCSC/CERT-CC                                                              | N.v.t.                   | —                                         |
| 5. Fixes intern doorgevoerd; bevindingen gedocumenteerd in dit rapport                       | ✅ Gedaan                | Juni 2026                                 |

**Relevante normen:** NEN-7510 A.6.8 (rapportage van informatiebeveiligingsgebeurtenissen), CRA Art. 14 (meldplicht ENISA binnen 24 uur bij actief misbruik van kritieke kwetsbaarheid).

---

## 11. Verantwoording AI-tooling

> **Verplicht onderdeel.** Doel: aantonen dat _jij_ de security-beslissingen hebt genomen, niet de AI.

```
┌─────────────────────────────────────────────────────────────────┐
│ AI-TOOLING VERANTWOORDING                                         │
│                                                                   │
│ Gebruikte tools: [vul in: bijv. Claude Code, GitHub Copilot]      │
│                                                                   │
│ Wat ik aan AI heb gevraagd:                                        │
│ • [bijv. uitleg van HQL-injectiepatronen]                          │
│ • [bijv. genereren van template voor bevindingen-format]           │
│ • [bijv. suggesties voor NEN-7510 control-nummers]                 │
│                                                                   │
│ Wat de AI heeft gegenereerd:                                        │
│ • [bijv. opzet van de traceability matrix structuur]               │
│ • [bijv. voorbeeldcode voor parameterisatie-fix]                   │
│                                                                   │
│ Wat ik zelf heb gecontroleerd:                                     │
│ • [bijv. alle CVSS-scores zelf berekend via FIRST-calculator]      │
│ • [bijv. bevindingen geverifieerd in de broncode]                  │
│ • [bijv. dynamische validatie op draaiende Docker-omgeving]        │
│ • [bijv. NEN-7510 controls geverifieerd in de norm zelf]           │
│                                                                   │
│ Beslissingen die ik zelf heb gemaakt:                              │
│ • [bijv. RAG-status bepalen op basis van bevindingen]              │
│ • [bijv. bevindingen prioriteren (P1/P2/P3)]                       │
│ • [bijv. PT-08 accepteren als restrisico met onderbouwing]         │
│ • [bijv. scope-afbakening: welke bevindingen buiten scope vallen]  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Checklist vóór oplevering

- [ ] Executive summary ≤ 400 woorden, geen jargon, RAG-status onderbouwd
- [x] Minimaal 4 volledig uitgewerkte bevindingen in sectie 4 (R01, R02, R03, R04, R13, R14)
- [x] Traceability matrix met minimaal 5 NEN-7510 controls, elk met verifieerbaar bewijs
- [x] Alle bijlagen gerefereerd vanuit de hoofdtekst
- [x] SBOM-sectie aanwezig (vul versienummers in uit `sbom/sbom-{versie}-{datum}.json` van de GitHub Actions pipeline)
- [x] CRA-mapping aanwezig
- [ ] AI-tooling verantwoording ingevuld (vul jouw eigen tekst in)
- [x] Geen bevindingen verzwegen — open/onopgeloste bevindingen opgenomen (CICD-06, PT-08, wachtwoordrotatie R02)
- [ ] Auteursnamen ingevuld (repository-URL is ingevuld)
- [ ] Document opgeslagen als artefact in de repository (PDF of Word)

---

## Bronnen (Bijlage M)

| Type               | Bron                                                                                                                                                                                                              |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Norm               | NEN-7510:2024 Deel 2 — Informatiebeveiliging in de zorg                                                                                                                                                           |
| Norm               | ISO/IEC 27005:2022 — Information security risk management                                                                                                                                                         |
| Wet                | AVG / GDPR — Verordening (EU) 2016/679                                                                                                                                                                            |
| Wet                | Cyber Resilience Act (CRA) — Verordening (EU) 2024/2847                                                                                                                                                           |
| CVE-database       | https://nvd.nist.gov/vuln                                                                                                                                                                                         |
| CVSS-calculator    | https://www.first.org/cvss/calculator/3.1                                                                                                                                                                         |
| CWE Top 25         | https://cwe.mitre.org/top25/                                                                                                                                                                                      |
| OWASP Top 10       | https://owasp.org/Top10/ (2021)                                                                                                                                                                                   |
| Interne documenten | `01-gap-analyse.md`, `03-assets.md`, `04-risicocriteria.md`, `05-risicomatrix.md`, `06-bowtie.md`, `07-security-backlog.md`, `11-risk-assessment-report.md`, `13-gap-analyse-logging.md`, `14-pentest-rapport.md` |
