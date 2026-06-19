# 03 Asset-identificatie - OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-26: Asset-identificatie
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Norm:** NEN-7510:2024

---

## 1. Inleiding

Dit document beschrijft de kroonjuwelen ("assets") van de OpenMRS Appointment Scheduler module. Een asset is elke component, dataset of functionaliteit waarvan de vertrouwelijkheid, integriteit of beschikbaarheid van belang is voor de veilige werking van het systeem. De identificatie vormt de basis voor de risicomatrix en de security backlog.

---

## 2. Scope

De OpenMRS Appointment Scheduler biedt functionaliteit voor:

- Het aanmaken, wijzigen en annuleren van patiëntafspraken
- Beheer van beschikbaarheden van zorgverleners (providers)
- Inzage in afspraakhistorie per patiënt
- REST API-endpoints voor integratie met andere OpenMRS-modules

---

## 3. Asset-overzicht

### 3.1 Data-assets

| Asset ID | Asset                                                    | Type              | Gevoeligheid | CIA-classificatie                    | Broncode-referentie                                                                                                                    | Toelichting                                                                                           |
| -------- | -------------------------------------------------------- | ----------------- | ------------ | ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| DA-01    | Patiëntgegevens (naam, geboortedatum, patiënt-ID)        | Persoonsdata      | **Hoog**     | C: Hoog / I: Hoog / A: Hoog          | `api/src/main/java/org/openmrs/module/appointmentscheduling/Appointment.java` → veld `patient` (relatie naar OpenMRS `Patient`-object) | Bijzondere persoonsgegevens onder AVG artikel 9; zorgdata valt ook onder NEN-7510                     |
| DA-02    | Afspraakgegevens (datum, tijd, locatie, reden van komst) | Medische data     | **Hoog**     | C: Hoog / I: Hoog / A: Hoog          | `Appointment.java` → velden `timeSlot`, `appointmentType`, `reason`; DB-tabel `appointment_scheduling_appointment`                     | Koppeling patiënt ↔ zorgverlener is gevoelig; reden van komst kan diagnose-informatie bevatten        |
| DA-03    | Providergegevens (naam, specialisatie, beschikbaarheid)  | Bedrijfsdata      | **Midden**   | C: Midden / I: Hoog / A: Midden      | `AppointmentBlock.java` → veld `provider`; DB-tabel `appointment_scheduling_block`                                                     | Manipulatie van beschikbaarheden kan zorgverlening verstoren                                          |
| DA-04    | Audit logs (wie heeft welke afspraak ingezien/gewijzigd) | Operationele data | **Hoog**     | C: Hoog / I: Hoog / A: Midden        | OpenMRS core `BaseOpenmrsData` → velden `creator`, `dateCreated`, `changedBy`, `dateChanged` (geërfd door `Appointment.java`)          | Noodzakelijk voor NEN-7510 A.8.15 (logging & monitoring); onmisbaar bij incidentonderzoek             |
| DA-05    | Sessiedata en authenticatietokens                        | Technische data   | **Hoog**     | C: Hoog / I: Hoog / A: Midden        | OpenMRS core sessiebeheer; REST-authenticatie via `BasicAuthenticationFilter` in `openmrs-module-webservices.rest`                     | Diefstal van sessietokens leidt tot impersonatie van gebruiker                                        |
| DA-06    | Databaseconfiguratie (connection strings, credentials)   | Configuratiedata  | **Kritiek**  | C: Kritiek / I: Kritiek / A: Kritiek | `omod/src/main/resources/hibernate.cfg.xml`; runtime via `openmrs-runtime.properties`                                                  | Vastgelegd in `hibernate.cfg.xml` of omgevingsvariabelen; blootstelling geeft directe databasetoegang |

---

### 3.2 Systeemcomponenten

| Asset ID | Component                                                    | Type                  | Gevoeligheid | CIA-classificatie                    | Broncode-referentie                                                                       | Toelichting                                                                                                                              |
| -------- | ------------------------------------------------------------ | --------------------- | ------------ | ------------------------------------ | ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| SC-01    | REST API (`/ws/rest/v1/appointment*`)                        | Applicatielaag        | **Hoog**     | C: Hoog / I: Hoog / A: Hoog          | `omod/src/main/java/.../rest/resource/AppointmentResource.java`                           | Publiek toegankelijke endpoints; onvoldoende autorisatiechecks leiden tot IDOR of ongeautoriseerde mutaties                              |
| SC-02    | Autorisatielaag (Spring Security / OpenMRS privilege-checks) | Beveiligingscomponent | **Kritiek**  | C: Kritiek / I: Kritiek / A: Hoog    | `AppointmentUtils.java`; privilege-constanten in `AppointmentSchedulingConstants.java`    | Centrale poortwachter; fout hierin raakt alle andere assets                                                                              |
| SC-03    | Persistentielaag (Hibernate / MySQL-tabellen)                | Datalaag              | **Kritiek**  | C: Kritiek / I: Kritiek / A: Kritiek | `db/hibernate/HibernateAppointmentDAO.java`; `AppointmentSchedulingHibernateMappings.xml` | SQL-injectie of verkeerde ORM-configuratie kan volledige database blootstellen                                                           |
| SC-04    | Inputvalidatie en -sanitatie                                 | Beveiligingscomponent | **Hoog**     | C: Midden / I: Hoog / A: Midden      | `validator/AppointmentTypeValidator.java`                                                 | Ontbrekende validatie is de oorzaak van XSS, SQLi en path traversal                                                                      |
| SC-05    | Third-party libraries (Maven-dependencies)                   | Infrastructuur        | **Midden**   | C: Midden / I: Midden / A: Midden    | `pom.xml` projectroot; zie `sbom/sbom-{versie}-{datum}.json` (pipeline-artifact)                                           | Verouderde libraries met bekende CVE's vormen een indirect risico; zie SBOM (taak 1.4 / 2.4)                                             |
| SC-06    | Loggingmechanisme (Log4j / SLF4J)                            | Operationeel          | **Midden**   | C: Laag / I: Midden / A: Midden      | `org.apache.commons.logging.Log` in o.a. `AppointmentServiceImpl.java`                    | Log4Shell (CVE-2021-44228) toont aan dat loggers zelf aanvalsvlak kunnen zijn; ook: onvoldoende logging maakt incidenten ondetecteerbaar |

---

### 3.3 Procesassets

| Asset ID | Proces                                   | Type               | Gevoeligheid | Toelichting                                                                       |
| -------- | ---------------------------------------- | ------------------ | ------------ | --------------------------------------------------------------------------------- |
| PA-01    | Afspraak aanmaken / wijzigen / annuleren | Bedrijfsproces     | **Hoog**     | Verstoring leidt direct tot impact op zorgverlening                               |
| PA-02    | Authenticatie en sessiebeheer            | Beveiligingsproces | **Kritiek**  | Fundament voor alle toegangsbeveiliging                                           |
| PA-03    | Toegangscontrole op patiëntdossiers      | Beveiligingsproces | **Kritiek**  | Iedere gebruiker mag alleen eigen of toegewezen patiënten inzien (NEN-7510 A.8.3) |
| PA-04    | Auditlogging van gebruikersacties        | Complianceproces   | **Hoog**     | Vereist door NEN-7510 A.8.15; ontbreekt gedeeltelijk per gap-analyse sprint 1     |

---

## 4. Eigenaarschap en verantwoordelijkheden

| Asset ID     | Eigenaar (functioneel)                        | Beheerder (technisch) | Wetgeving / norm            |
| ------------ | --------------------------------------------- | --------------------- | --------------------------- |
| DA-01, DA-02 | Zorginstelling (verwerkingsverantwoordelijke) | OpenMRS-beheerder     | AVG art. 9, NEN-7510:2024-2 |
| DA-03        | HR / planning zorginstelling                  | OpenMRS-beheerder     | NEN-7510 A.8.3              |
| DA-04        | CISO / privacy officer                        | DevOps / beheerder    | NEN-7510 A.8.15             |
| DA-05, SC-02 | Applicatiebeheerder                           | Ontwikkelteam         | NEN-7510 A.8.5              |
| DA-06, SC-03 | DBA / systeembeheerder                        | DevOps                | NEN-7510 A.8.3              |
| SC-05        | Ontwikkelteam                                 | Ontwikkelteam         | CRA (Cyber Resilience Act)  |

---

## 5. Gevoeligheidsclassificatie — legenda

| Niveau      | Omschrijving                                                                                                                          |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **Kritiek** | Blootstelling of uitval heeft directe, ernstige gevolgen voor patiëntveiligheid of de continuïteit van zorgverlening                  |
| **Hoog**    | Significante schade voor patiënten, zorgverleners of de organisatie bij verlies van vertrouwelijkheid, integriteit of beschikbaarheid |
| **Midden**  | Beperkte directe schade, maar kan indirect leiden tot escalatie of reputatieschade                                                    |
| **Laag**    | Geringe impact; herstel is eenvoudig en de schade is beheersbaar                                                                      |

---

## 6. Relatie met NEN-7510 controls (preview)

| Control | Omschrijving                               | Betrokken assets                         |
| ------- | ------------------------------------------ | ---------------------------------------- |
| A.8.3   | Toegangsbeveiliging tot informatiesystemen | DA-01, DA-02, DA-06, SC-01, SC-02, PA-03 |
| A.8.5   | Veilige authenticatie                      | DA-05, SC-02, PA-02                      |
| A.8.15  | Logging en monitoring                      | DA-04, SC-06, PA-04                      |

> Volledige uitwerking per control: zie `docs/auditrapport/01-gap-analyse.md.

---

<!-- ## 7. Volgende stap

De assets in dit document vormen de input voor:

- **Taak 2.2** – Risicomatrix: per asset worden dreigingen (kans × impact) bepaald
- **Taak 2.3** – Bow-tie analyse: het hoogste risico wordt dieper uitgewerkt
- **Taak 2.5** – Security backlog: technische kwetsbaarheden worden gekoppeld aan assets -->
