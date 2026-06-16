# 13 Gap-analyse Logging: OpenMRS Appointment Scheduler

**Sprint:** 3  
**Taak:** SOF-43: Gap-analyse logging uitvoeren
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Norm:** NEN-7510:2024-2, A.8.15 — Logging en monitoring  
**Gebaseerd op:** Sprint 1 gap-analyse (`01-gap-analyse.md`), security backlog (`09-security-backlog.md`), broncode-analyse `AppointmentServiceImpl.java`, `AppointmentService.java`, `AppointmentResource1_9.java`

---

## 1. Inleiding

Deze gap-analyse inventariseert alle logging in de OpenMRS Appointment Scheduler module en beoordeelt of deze voldoet aan NEN-7510:2024-2 control A.8.15 (Logging en monitoring). De analyse is een re-evaluatie en uitbreiding van de sprint 1 gap-analyse, aangevuld met kennis opgedaan tijdens de security audit van sprint 2.

NEN-7510 A.8.15 vereist dat:

- Gebeurtenissen die relevant zijn voor informatiebeveiliging worden gelogd
- Logbestanden worden beschermd tegen manipulatie en ongeautoriseerde toegang
- Gevoelige gegevens **niet** in logbestanden terechtkomen
- Logs regelmatig worden geëvalueerd

De centrale stelregel voor deze analyse: **niet gelogd == niet gebeurd**.

---

## 2. Huidige logging - inventarisatie

### 2.1 Aanwezig logging framework

De module gebruikt `org.apache.commons.logging.Log` via `LogFactory`, geïnitialiseerd in `AppointmentServiceImpl.java`:

```java
protected final Log log = LogFactory.getLog(this.getClass());
```

Dit is een standaard Java logging-abstractie die doorschakelt naar Log4j of SLF4J afhankelijk van de runtime-configuratie. Het framework zelf is aanwezig, maar het **gebruik** ervan is minimaal en incorrect.

### 2.2 Wat er daadwerkelijk gelogd wordt

Na analyse van `AppointmentServiceImpl.java` en `AppointmentResource1_9.java` zijn de volgende log-aanroepen gevonden:

| #   | Locatie                                                                  | Log-niveau | Inhoud                                                                                  | Bevinding                           |
| --- | ------------------------------------------------------------------------ | ---------- | --------------------------------------------------------------------------------------- | ----------------------------------- |
| L01 | `AppointmentServiceImpl.java` — `getAppointmentsForPatientWithLogging()` | `log.info` | `[AUDIT] Fetching appointments for patient: name=... dob=... identifier=... gender=...` | ❌ **PII in log** — AVG-overtreding |
| L02 | `BaseOpenmrsService` (geërfd)                                            | Diversen   | Framework-level debug logs                                                              | ✅ Geen PII, maar geen auditwaarde  |

**Conclusie:** Er is in de gehele module slechts **één bewuste log-aanroep** in de applicatiecode, en die bevat PII. Er is geen enkele auditlog voor mutaties.

---

## 3. Gap-analyse per event

### 3.1 Afspraak-events (Appointment)

| Event                        | Methode                                   | REST-endpoint                           | Gelogd?           | Gevoelige data in log?         | Compliant A.8.15? | Prioriteit |
| ---------------------------- | ----------------------------------------- | --------------------------------------- | ----------------- | ------------------------------ | ----------------- | ---------- |
| Afspraak aanmaken (CREATE)   | `saveAppointment()` / `bookAppointment()` | `POST /appointment`                     | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🔴 Kritiek |
| Afspraak wijzigen (UPDATE)   | `saveAppointment()`                       | `POST /appointment/{uuid}`              | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🔴 Kritiek |
| Afspraak inzien (READ)       | `getAppointmentsForPatientWithLogging()`  | `GET /appointment?patient=...`          | ✅ Ja — maar fout | ❌ **Naam, DOB, ID, geslacht** | ❌ Nee (PII-lek)  | 🔴 Kritiek |
| Afspraak annuleren (CANCEL)  | `changeAppointmentStatus()`               | `POST /appointment/{uuid}`              | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🔴 Kritiek |
| Afspraak voiden (VOID)       | `voidAppointment()`                       | `DELETE /appointment/{uuid}`            | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🔴 Kritiek |
| Afspraak statuswijziging     | `changeAppointmentStatus()`               | impliciet via save                      | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🔴 Kritiek |
| Afspraak verwijderen (PURGE) | `purgeAppointment()`                      | `DELETE /appointment/{uuid}?purge=true` | ❌ Nee            | n.v.t.                         | ❌ Nee            | 🟠 Hoog    |

### 3.2 Afspraakblok-events (AppointmentBlock)

| Event         | Methode                  | REST-endpoint                     | Gelogd? | Gevoelige data in log? | Compliant A.8.15? | Prioriteit |
| ------------- | ------------------------ | --------------------------------- | ------- | ---------------------- | ----------------- | ---------- |
| Blok aanmaken | `saveAppointmentBlock()` | `POST /appointmentblock`          | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟠 Hoog    |
| Blok wijzigen | `saveAppointmentBlock()` | `POST /appointmentblock/{uuid}`   | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟠 Hoog    |
| Blok voiden   | `voidAppointmentBlock()` | `DELETE /appointmentblock/{uuid}` | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟠 Hoog    |

### 3.3 Tijdslot-events (TimeSlot)

| Event           | Methode          | REST-endpoint          | Gelogd? | Gevoelige data in log? | Compliant A.8.15? | Prioriteit |
| --------------- | ---------------- | ---------------------- | ------- | ---------------------- | ----------------- | ---------- |
| Tijdslot voiden | `voidTimeSlot()` | indirect via blok-void | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟠 Hoog    |

### 3.4 Afspraaktype-events (AppointmentType)

| Event                  | Methode                   | REST-endpoint                    | Gelogd? | Gevoelige data in log? | Compliant A.8.15? | Prioriteit |
| ---------------------- | ------------------------- | -------------------------------- | ------- | ---------------------- | ----------------- | ---------- |
| Type aanmaken/wijzigen | `saveAppointmentType()`   | `POST /appointmenttype`          | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟡 Midden  |
| Type retiren           | `retireAppointmentType()` | `DELETE /appointmenttype/{uuid}` | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟡 Midden  |

### 3.5 Provider-rooster-events (ProviderSchedule)

| Event                     | Methode                  | REST-endpoint                     | Gelogd? | Gevoelige data in log? | Compliant A.8.15? | Prioriteit |
| ------------------------- | ------------------------ | --------------------------------- | ------- | ---------------------- | ----------------- | ---------- |
| Rooster aanmaken/wijzigen | `saveProviderSchedule()` | `POST /providerschedule`          | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟡 Midden  |
| Rooster voiden            | `voidProviderSchedule()` | `DELETE /providerschedule/{uuid}` | ❌ Nee  | n.v.t.                 | ❌ Nee            | 🟡 Midden  |

### 3.6 Autorisatie-events

| Event                                         | Locatie                                                    | Gelogd?                                 | Gevoelige data in log? | Compliant A.8.15?      | Prioriteit |
| --------------------------------------------- | ---------------------------------------------------------- | --------------------------------------- | ---------------------- | ---------------------- | ---------- |
| Ongeautoriseerde toegangspoging               | `@Authorized`-interceptor (OpenMRS core)                   | ⚠️ Onbekend — core-verantwoordelijkheid | n.v.t.                 | ⚠️ Buiten scope module | 🟠 Hoog    |
| Lege `@Authorized()` — iedereen heeft toegang | `getAllProviderSchedules()`, `getProviderScheduleByUuid()` | ❌ Nee                                  | n.v.t.                 | ❌ Nee                 | 🔴 Kritiek |
| IDOR — opvragen afspraken andere patiënt      | `getAppointmentsOfPatient()`                               | ❌ Nee                                  | n.v.t.                 | ❌ Nee                 | 🔴 Kritiek |

### 3.7 Batch-events

| Event                                   | Methode                   | Gelogd? | Gevoelige data? | Compliant A.8.15? | Prioriteit |
| --------------------------------------- | ------------------------- | ------- | --------------- | ----------------- | ---------- |
| Automatisch opruimen verlopen afspraken | `cleanOpenAppointments()` | ❌ Nee  | n.v.t.          | ❌ Nee            | 🟠 Hoog    |

---

## 4. Re-evaluatie sprint 1 gap-analyse

De sprint 1 gap-analyse constateerde een totaalscore van **51% compliant** met een score van 20% op A.8.15. Na diepere broncode-analyse moet deze score neerwaarts worden bijgesteld:

| Control                     | Sprint 1 score | Herziene score | Reden bijstelling                                                                                                                                                 |
| --------------------------- | -------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A.8.3 — Toegangsbeveiliging | 60%            | 45%            | Lege `@Authorized()` op `getAllProviderSchedules()` en `getProviderScheduleByUuid()` was niet meegewogen; typfout in privilege-constanten maakt checks onwerkzaam |
| A.8.5 — Authenticatie       | 75%            | 70%            | Geen wijziging; score blijft gelijk                                                                                                                               |
| A.8.15 — Logging            | 20%            | **5%**         | Slechts één log-aanroep in de module, en die bevat PII; geen enkel auditspoor voor mutaties                                                                       |
| **Totaal**                  | **51%**        | **40%**        |                                                                                                                                                                   |

---

## 5. Huidig vs. gewenst

### 5.1 Gewenst logging-model (conform NEN-7510 A.8.15)

Een compliant auditlog voor een medische module moet minimaal de volgende velden bevatten per geloggde gebeurtenis:

| Veld              | Vereiste                                                | Voorbeeld                          |
| ----------------- | ------------------------------------------------------- | ---------------------------------- |
| Tijdstip          | Automatisch via logging framework                       | `2026-06-09T14:23:01Z`             |
| Gebruiker UUID    | `Context.getAuthenticatedUser().getUuid()`              | `550e8400-e29b-41d4-a716-...`      |
| Actie             | CREATE / READ / UPDATE / VOID / CANCEL / RETIRE / PURGE | `VOID`                             |
| Resource type     | Klasse van het object                                   | `Appointment`                      |
| Resource UUID     | `object.getUuid()`                                      | `a3f7c2d1-...`                     |
| Resultaat         | SUCCESS / FAILURE                                       | `SUCCESS`                          |
| Reden (optioneel) | Bij void/retire/cancel                                  | `"Patient requested cancellation"` |

**Nooit loggen:**

- Patiëntnaam, geboortedatum, geslacht, BSN/patiënt-ID
- Wachtwoorden, tokens, credentials
- Volledige request-bodies met PII

### 5.2 Gap-overzicht

| Categorie                            | Huidig                 | Gewenst                                            | Gap                     |
| ------------------------------------ | ---------------------- | -------------------------------------------------- | ----------------------- |
| Auditlog bij CREATE afspraak         | ❌ Geen                | ✅ Log met user UUID + appointment UUID + actie    | Ontbreekt volledig      |
| Auditlog bij UPDATE afspraak         | ❌ Geen                | ✅ Log met user UUID + appointment UUID + actie    | Ontbreekt volledig      |
| Auditlog bij READ patiëntafspraken   | ❌ PII-lek             | ✅ Log met user UUID + patient UUID                | Aanwezig maar incorrect |
| Auditlog bij VOID/CANCEL afspraak    | ❌ Geen                | ✅ Log met user UUID + appointment UUID + reden    | Ontbreekt volledig      |
| Auditlog bij blok-/tijdslot-mutaties | ❌ Geen                | ✅ Log met user UUID + resource UUID + actie       | Ontbreekt volledig      |
| Auditlog bij autorisatiefout         | ❌ Geen (module-scope) | ✅ Log met user UUID + methode + resultaat=FAILURE | Ontbreekt (deels core)  |
| PII-vrije logging                    | ❌ PII aanwezig        | ✅ Uitsluitend UUID's in logs                      | Kritieke overtreding    |
| Gestructureerd log-formaat           | ❌ Vrije tekst         | ✅ Consistente `[AUDIT]`-prefix + vaste velden     | Ontbreekt               |

---

## 6. Bevindingen gekoppeld aan attack surface

| Attack surface punt                   | Event                               | Logging-gap                      | Risico-ID | NEN-7510       |
| ------------------------------------- | ----------------------------------- | -------------------------------- | --------- | -------------- |
| `POST /appointment`                   | Afspraak aanmaken                   | Geen auditlog                    | R07       | A.8.15         |
| `POST /appointment/{uuid}`            | Afspraak wijzigen / status wijzigen | Geen auditlog                    | R07       | A.8.15         |
| `GET /appointment?patient=`           | Afspraken inzien                    | PII in log                       | R01       | A.8.15         |
| `DELETE /appointment/{uuid}`          | Afspraak voiden                     | Geen auditlog                    | R07       | A.8.15         |
| `GET /providerschedule`               | Provider roosters inzien            | Geen autorisatie + geen log      | R05       | A.8.3 + A.8.15 |
| `cleanOpenAppointments()` (scheduler) | Batch statuswijziging               | Geen auditlog                    | R07       | A.8.15         |
| Alle mutatieoperaties                 | Void/retire vlaggen niet gezet      | Geen integriteitswaarborg in log | R11       | A.8.6 + A.8.15 |

---

## 7. Aanbevelingen

### Prioriteit 1 - Onmiddellijk (sprint 3)

**A. Verwijder PII uit logstatement (R01)**

```java
// Voor (❌):
log.info("[AUDIT] Fetching appointments for patient: name=" + patient.getPersonName() + ...);

// Na (✅):
log.info("[AUDIT] appointment.read | user={} | patientUuid={} | result=SUCCESS",
    Context.getAuthenticatedUser().getUuid(),
    patient.getUuid());
```

**B. Voeg auditlogging toe aan alle mutatiemethoden (R07)**

```java
// saveAppointment:
log.info("[AUDIT] appointment.{} | user={} | appointmentUuid={} | result=SUCCESS",
    appointment.getId() == null ? "create" : "update",
    Context.getAuthenticatedUser().getUuid(),
    appointment.getUuid());

// voidAppointment:
log.info("[AUDIT] appointment.void | user={} | appointmentUuid={} | reason={} | result=SUCCESS",
    Context.getAuthenticatedUser().getUuid(),
    appointment.getUuid(),
    reason);

// changeAppointmentStatus:
log.info("[AUDIT] appointment.statusChange | user={} | appointmentUuid={} | newStatus={} | result=SUCCESS",
    Context.getAuthenticatedUser().getUuid(),
    appointment.getUuid(),
    newStatus);

// cleanOpenAppointments:
log.info("[AUDIT] appointment.batchClean | user=SYSTEM | processed={} | result=SUCCESS",
    appointmentsInStates.size());
```

### Prioriteit 2 - Volgende sprint

**C.** Auditlogging voor `voidAppointmentBlock()`, `voidTimeSlot()`, `saveAppointmentBlock()`

**D.** Overweeg een Spring AOP-aspect dat mislukte `@Authorized`-checks logt als FAILURE

---

## 8. Vereiste tests (input voor SOF-44)

| Test ID | Testbeschrijving                           | Te testen methode                        | Verwacht resultaat                                                               |
| ------- | ------------------------------------------ | ---------------------------------------- | -------------------------------------------------------------------------------- |
| LOG-01  | Auditlog verschijnt bij aanmaken afspraak  | `saveAppointment()` (nieuw)              | `[AUDIT] appointment.create` in log, geen PII                                    |
| LOG-02  | Auditlog verschijnt bij wijzigen afspraak  | `saveAppointment()` (bestaand)           | `[AUDIT] appointment.update` in log, geen PII                                    |
| LOG-03  | Auditlog verschijnt bij voiden afspraak    | `voidAppointment()`                      | `[AUDIT] appointment.void` in log met reden                                      |
| LOG-04  | Auditlog verschijnt bij statuswijziging    | `changeAppointmentStatus()`              | `[AUDIT] appointment.statusChange` in log                                        |
| LOG-05  | Auditlog bij READ bevat geen PII           | `getAppointmentsForPatientWithLogging()` | Log bevat UUID, niet `getPersonName()` etc.                                      |
| LOG-06  | Geen PII-velden in loguitvoer (grep-check) | Alle servicemethoden                     | Geen `getPersonName`, `getBirthdate`, `getGender`, `getPatientIdentifier` in log |
| LOG-07  | Auditlog bij batch-opruimen aanwezig       | `cleanOpenAppointments()`                | `[AUDIT] appointment.batchClean` in log                                          |

---

## 9. Samenvatting

| Aspect                              | Status                                | Actie vereist         |
| ----------------------------------- | ------------------------------------- | --------------------- |
| Log framework aanwezig              | ✅                                    | Nee                   |
| PII-vrije logging                   | ❌ Kritiek                            | Ja — R01, sprint 3    |
| Auditlog bij CREATE/UPDATE          | ❌ Ontbreekt                          | Ja — R07, sprint 3    |
| Auditlog bij VOID/CANCEL            | ❌ Ontbreekt                          | Ja — R07, sprint 3    |
| Auditlog bij blok-/tijdslotmutaties | ❌ Ontbreekt                          | Ja — sprint 3/4       |
| Auditlog bij autorisatiefouten      | ❌ Ontbreekt                          | Deels — sprint 3/4    |
| Gestructureerd logformaat           | ❌ Ontbreekt                          | Ja — sprint 3         |
| Tests voor logging                  | ❌ Ontbreekt                          | Ja — SOF-44, sprint 3 |
| **A.8.15 compliantiescore**         | **5% → streef naar 80%+ na sprint 3** |                       |
