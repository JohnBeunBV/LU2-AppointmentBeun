# 05 — Risicomatrix: OpenMRS Appointment Scheduling Module

**Sprint:** 2 | **Taak:** SOF-27: Risicomatrix | **Datum:** Juni 2026 | **Norm:** NEN-7510:2024

---

## Kleurcodering

| Kleur | Score | Beoordeling | Actie |
|-------|-------|-------------|-------|
| 🔴 Rood | ≥ 15 | Onacceptabel risico | Onmiddellijk aanpakken |
| 🟠 Oranje | 8 – 14 | Verhoogd risico | Mitigatieplan met deadline opstellen |
| 🟢 Groen | ≤ 7 | Acceptabel | Monitoren en periodiek herbeoordelen |

**Risicoscore = Kans × Impact** (schaal 1–5)

---

## Risicomatrix

<table>
  <thead>
    <tr>
      <th style="text-align:center">Impact ↓ &nbsp;/&nbsp; Kans →</th>
      <th style="text-align:center">1<br><small>Zeldzaam</small></th>
      <th style="text-align:center">2<br><small>Onwaarschijnlijk</small></th>
      <th style="text-align:center">3<br><small>Mogelijk</small></th>
      <th style="text-align:center">4<br><small>Waarschijnlijk</small></th>
      <th style="text-align:center">5<br><small>Vrijwel zeker</small></th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>5 — Catastrofaal</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>5</strong></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>10</strong></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>15</strong></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>20</strong><br><small>R02</small></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>25</strong><br><small>R01</small></td>
    </tr>
    <tr>
      <td><strong>4 — Ernstig</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>8</strong></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>12</strong><br><small>R05 R06 R08</small></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>16</strong><br><small>R03</small></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>20</strong></td>
    </tr>
    <tr>
      <td><strong>3 — Significant</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>3</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>6</strong></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>9</strong><br><small>R09</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>12</strong><br><small>R07 R11</small></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>15</strong><br><small>R04</small></td>
    </tr>
    <tr>
      <td><strong>2 — Beperkt</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>2</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>6</strong><br><small>R10</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>8</strong></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>10</strong></td>
    </tr>
    <tr>
      <td><strong>1 — Minimaal</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>1</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>2</strong><br><small>R12</small></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>3</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>5</strong></td>
    </tr>
  </tbody>
</table>

---

## Risicoregister

| ID  | Risico                                                        | Getroffen asset(s)    | Kans | Impact | Score  | Klasse    | NEN-7510 control |
| --- | ------------------------------------------------------------- | --------------------- | :--: | :----: | :----: | --------- | ---------------- |
| R01 | PII-logging: patiëntdata in logbestanden                      | DA-01, DA-04, SC-06   | 5    | 5      | **25** | 🔴 Rood   | A.8.15           |
| R02 | Hardcoded credentials in broncode                             | DA-06, SC-03          | 4    | 5      | **20** | 🔴 Rood   | A.9.2            |
| R03 | Geen data-level ACL (patiënt ziet andermans afspraken)        | DA-01, DA-02, SC-01   | 4    | 4      | **16** | 🔴 Rood   | A.8.3            |
| R04 | Typfouten in privilege-constanten (privilege-checks falen)    | SC-02, DA-03          | 5    | 3      | **15** | 🔴 Rood   | A.8.3            |
| R05 | Lege `@Authorized()` op servicemethoden                       | SC-01, SC-02, DA-02   | 3    | 4      | **12** | 🟠 Oranje | A.8.3            |
| R06 | Geen multi-factor authenticatie                               | DA-05, SC-02, PA-02   | 3    | 4      | **12** | 🟠 Oranje | A.8.5            |
| R07 | Geen audit logging voor afspraakmutaties                      | DA-04, PA-04          | 4    | 3      | **12** | 🟠 Oranje | A.8.15           |
| R08 | Geen brute-force beveiliging op inlogpagina                   | DA-05, PA-02          | 3    | 4      | **12** | 🟠 Oranje | A.8.5            |
| R09 | ConcurrentModificationException in `cleanOpenAppointments`    | DA-02, PA-01          | 3    | 3      | **9**  | 🟠 Oranje | A.8.6            |
| R10 | Gebruik van deprecated Date-API                               | DA-02, SC-03          | 2    | 3      | **6**  | 🟢 Groen  | A.8.8            |
| R11 | `retireAppointmentType` / `voidAppointment` zet vlag niet     | DA-02, PA-01          | 4    | 3      | **12** | 🟠 Oranje | A.8.6            |
| R12 | Ongebruikte variabele `satisfyingConstraints`                 | SC-04                 | 2    | 1      | **2**  | 🟢 Groen  | —                |

---

## Toelichting per risico

### R01 — PII-logging (Score 25 — 🔴 Onacceptabel)

**Getroffen assets:** DA-01 (patiëntgegevens), DA-04 (audit logs), SC-06 (loggingmechanisme)

**Wat kan er misgaan:** `AppointmentServiceImpl.java` (regel 1426–1432) schrijft naam, geboortedatum, BSN en geslacht van patiënten als platte tekst naar de applicatielog. Logbestanden worden doorgaans minder streng beveiligd dan patiëntdossiers en zijn toegankelijk voor beheerders en DevOps-medewerkers die geen medische noodzaak hebben.

**Gevolgen:** Overtreding van AVG artikel 9 (bijzondere categorieën persoonsgegevens), meldplicht bij de Autoriteit Persoonsgegevens, boetes tot € 20 miljoen of 4 % van de jaaromzet, reputatieschade.

**Aanbevolen maatregel:** Verwijder PII uit de logstatement; log uitsluitend een gepseudonimiseerde patiënt-UUID. Implementeer aanvullend een apart, toegangsbeheerd auditlog voor legitieme medische audit-doeleinden.

---

### R02 — Hardcoded credentials (Score 20 — 🔴 Onacceptabel)

**Getroffen assets:** DA-06 (databaseconfiguratie), SC-03 (persistentielaag)

**Wat kan er misgaan:** `AppointmentActivator.java` (regels 79–82) bevat een productiewachtwoord en volledige JDBC-verbindingsstring met gebruikersnaam en wachtwoord. Deze zijn zichtbaar in de volledige git-geschiedenis voor iedereen met leestoegang tot de repository.

**Gevolgen:** Directe databasetoegang voor onbevoegden; alle patiënt- en afspraakdata zijn uitleesbaar en aanpasbaar.

**Aanbevolen maatregel:** Verwijder credentials uit broncode; gebruik omgevingsvariabelen of een secrets manager (GitHub Secrets, HashiCorp Vault). Roteer het gelekte wachtwoord onmiddellijk en herzie welke accounts toegang hebben tot de database.

---

### R03 — Geen data-level ACL (Score 16 — 🔴 Onacceptabel)

**Getroffen assets:** DA-01 (patiëntgegevens), DA-02 (afspraakgegevens), SC-01 (REST API)

**Wat kan er misgaan:** Elke geauthenticeerde gebruiker kan alle afspraken van alle patiënten opvragen. Er is geen controle of de opvragende gebruiker behandelaar is van de betreffende patiënt (IDOR-kwetsbaarheid).

**Gevolgen:** Privacyschending; patiëntgegevens van derden zijn inzichtelijk zonder medische noodzaak (AVG Art. 5 lid 1b — doelbinding; NEN-7510 A.8.3 — PA-03).

**Aanbevolen maatregel:** Implementeer row-level filtering in `AppointmentResource.java`: controleer bij `getAppointmentsOfPatient` of de aanroepende gebruiker de behandelend arts is of de patiënt zelf.

---

### R04 — Typfouten in privilege-constanten (Score 15 — 🔴 Onacceptabel)

**Getroffen assets:** SC-02 (autorisatielaag), DA-03 (providergegevens)

**Wat kan er misgaan:** `AppointmentUtils.java` definieert `PRIV_VIEW_PROVIDER_SCHEDULES = "View Provider Scedules"` (foutieve spelling). De database bevat `"View Provider Schedules"` (correcte spelling). Alle privilege-checks voor provider-roosters mislukken hierdoor — afhankelijk van de OpenMRS-implementatie geeft dit óf iedereen óf niemand toegang.

**Aanbevolen maatregel:** Herstel de spelling in `AppointmentUtils.java` naar `"View Provider Schedules"` en `"Manage Provider Schedules"`. Voeg een integratietest toe die verifieert dat elke privilege-constante overeenkomt met een registratie in `config.xml`.

---

### R05 — Lege `@Authorized()` annotaties (Score 12 — 🟠 Verhoogd)

**Getroffen assets:** SC-01 (REST API), SC-02 (autorisatielaag), DA-02 (afspraakgegevens)

**Wat kan er misgaan:** `getAllProviderSchedules()` en `getProviderScheduleByUuid()` hebben een lege `@Authorized()`-annotatie. OpenMRS interpreteert dit als "geen specifiek privilege vereist", waardoor elke ingelogde gebruiker deze methoden kan aanroepen.

**Aanbevolen maatregel:** Voeg het correcte privilege toe: `@Authorized(AppointmentUtils.PRIV_VIEW_PROVIDER_SCHEDULES)`.

---

### R06 — Geen multi-factor authenticatie (Score 12 — 🟠 Verhoogd)

**Getroffen assets:** DA-05 (sessiedata), SC-02 (autorisatielaag), PA-02 (authenticatieproces)

**Wat kan er misgaan:** Accounts zijn uitsluitend beschermd met een wachtwoord. Bij credential-diefstal of een zwak wachtwoord is er geen tweede factor die toegang blokkeert, ook niet voor accounts met toegang tot gevoelige patiëntdata.

**Aanbevolen maatregel:** Activeer MFA op de OpenMRS-instantie; verplicht dit minimaal voor accounts met beheerders- of artsenrechten (NEN-7510 A.8.5).

---

### R07 — Geen audit logging afspraakmutaties (Score 12 — 🟠 Verhoogd)

**Getroffen assets:** DA-04 (audit logs), PA-04 (auditloggingproces)

**Wat kan er misgaan:** Aanmaken, wijzigen en verwijderen van afspraken worden niet gelogd. Bij een incident of klacht is niet aantoonbaar wie een afspraak heeft gewijzigd of verwijderd.

**Gevolgen:** Niet voldoen aan NEN-7510 A.8.15; juridisch bewijsmateriaal ontbreekt bij calamiteiten.

**Aanbevolen maatregel:** Implementeer auditlogging op `saveAppointment`, `voidAppointment` en `cancelAppointment` met minimaal: tijdstip, gebruiker-UUID, actie en resource-UUID.

---

### R08 — Geen brute-force beveiliging (Score 12 — 🟠 Verhoogd)

**Getroffen assets:** DA-05 (sessiedata), PA-02 (authenticatieproces)

**Wat kan er misgaan:** Geen account lockout of rate limiting op de inlogpagina. Een aanvaller kan onbeperkt wachtwoorden uitproberen zonder geblokkeerd te worden.

**Aanbevolen maatregel:** Configureer account lockout na N mislukte pogingen in de OpenMRS-instellingen; voeg IP-rate limiting toe op de reverse proxy (nginx/Apache).

---

### R09 — ConcurrentModificationException (Score 9 — 🟠 Verhoogd)

**Getroffen assets:** DA-02 (afspraakgegevens), PA-01 (afspraakproces)

**Wat kan er misgaan:** `cleanOpenAppointments` (regels 961–988) verwijdert elementen via `List.remove()` tijdens iteratie met een `Iterator`. Dit gooit een `ConcurrentModificationException` zodra een afspraak wordt opgeruimd, waardoor de transactie mislukt en de dienst tijdelijk onbeschikbaar kan worden.

**Aanbevolen maatregel:** Vervang `list.remove(item)` door `iterator.remove()` of verzamel te verwijderen items in een aparte lijst en verwijder deze na de iteratie.

---

### R10 — Deprecated Date-API (Score 6 — 🟢 Acceptabel)

**Getroffen assets:** DA-02 (afspraakgegevens), SC-03 (persistentielaag)

**Wat kan er misgaan:** `AppointmentServiceImpl.java` (regels 1306–1311) gebruikt `Date.getYear()`, `getMonth()` en `getDate()`. Deze methoden zijn deprecated sinds Java 1.1 en geven incorrect resultaat bij tijdzone-gevoelige berekeningen.

**Aanbevolen maatregel:** Vervang door `java.util.Calendar` of `java.time.LocalDateTime` (Java 8+); opnemen in backlog voor de volgende Java-upgrade.

---

### R11 — `retireAppointmentType` / `voidAppointment` zet vlag niet (Score 12 — 🟠 Verhoogd)

**Getroffen assets:** DA-02 (afspraakgegevens), PA-01 (afspraakproces)

**Wat kan er misgaan:** `retireAppointmentType()`, `voidAppointment()`, `voidTimeSlot()` en `voidAppointmentBlock()` slaan op zonder de bijbehorende vlag (`retired` / `voided`) te zetten. Retired of voided records blijven daardoor actief zichtbaar en bruikbaar.

**Aanbevolen maatregel:** Stel in elke methode de vlag in vóór de `save`-aanroep en valideer dit met een unit test.

---

### R12 — Ongebruikte variabele `satisfyingConstraints` (Score 2 — 🟢 Acceptabel)

**Getroffen assets:** SC-04 (inputvalidatie)

**Wat kan er misgaan:** `satisfyingConstraints` (regel 813) wordt aangemaakt maar nooit gelezen. Minimaal beveiligingsrisico; mogelijk aanwijzing dat een validatiecontrole ontbreekt die ooit bedoeld was.

**Aanbevolen maatregel:** Verwijder de variabele of implementeer de ontbrekende validatie als die bedoeld was.

---

## Samenvatting prioriteiten

| Prioriteit | ID | Risico | Score |
|------------|----|--------|-------|
| 🔴 Onmiddellijk aanpakken | R01 | PII-logging | 25 |
| 🔴 Onmiddellijk aanpakken | R02 | Hardcoded credentials | 20 |
| 🔴 Onmiddellijk aanpakken | R03 | Geen data-level ACL | 16 |
| 🔴 Onmiddellijk aanpakken | R04 | Typfouten privilege-constanten | 15 |
| 🟠 Mitigatieplan opstellen | R05 | Lege @Authorized annotaties | 12 |
| 🟠 Mitigatieplan opstellen | R06 | Geen MFA | 12 |
| 🟠 Mitigatieplan opstellen | R07 | Geen audit logging | 12 |
| 🟠 Mitigatieplan opstellen | R08 | Geen brute-force beveiliging | 12 |
| 🟠 Mitigatieplan opstellen | R11 | Void/retire-vlaggen niet gezet | 12 |
| 🟠 Mitigatieplan opstellen | R09 | ConcurrentModificationException | 9 |
| 🟢 Monitoren | R10 | Deprecated Date-API | 6 |
| 🟢 Monitoren | R12 | Ongebruikte variabele | 2 |
