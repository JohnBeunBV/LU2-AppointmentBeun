# 00 - Security & Maintainability - OpenMRS Appointment Scheduler

**Sprint:** 1
**Taak:** SOF-22: GAP-Analyse
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 2
**Norm:** NEN-7510:2024

## Kwaliteitseisen

### Security

| #   | Eis                                    | Norm                                                                                                                       |
| --- | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| S1  | Geen hardcoded credentials in broncode | Wachtwoorden, gebruikersnamen en verbindingsstrings mogen niet als literal in de code staan                                |
| S2  | Geen PII in logbestanden               | Persoonsgegevens (naam, geboortedatum, patiënt-ID, geslacht) mogen niet via `log.info` of `log.debug` worden weggeschreven |
| S3  | Toegangscontrole op service-methoden   | Elke publieke methode in `AppointmentService` moet een `@Authorized`-annotatie hebben met het juiste privilege             |

### Maintainability

| #   | Eis                               | Norm                                                                                                                         |
| --- | --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| M1  | Geen gebruik van deprecated API's | Verouderde Java-methoden (bijv. `Date.getYear()`, `Date.getMonth()`) mogen niet worden gebruikt                              |
| M2  | Methoden doen wat ze beloven      | Methoden als `retireAppointmentType` en `voidAppointment` moeten de bijbehorende vlag op het object zetten vóór opslag       |
| M3  | Geen typfouten in constantenamen  | Privilege-constanten in `AppointmentUtils` moeten exact overeenkomen met de waarden die in de database zijn geregistreerd    |
| M4  | Correcte iteratie over collecties | Verwijderen van elementen uit een lijst tijdens iteratie mag alleen via `Iterator.remove()`, niet via directe lijstoperaties |
| M5  | Geen ongebruikte variabelen       | Variabelen die worden aangemaakt maar nooit gebruikt (bijv. `satisfyingConstraints`) moeten worden verwijderd                |
| M6  | Code coverage                     | Minimale code coverage vereist van 70%. Streven naar 80%.                                                                    |

---

## Bevindingen

### S1 — Hardcoded credentials (kritiek)

**Bestand:** `AppointmentActivator.java` regels 79–82

```java
private static final String HL7_EXPORT_PASSWORD = "Appt@Export2021!";
private static final String HL7_DB_URL = "jdbc:mysql://hl7-reports.hospital.internal:3306/appointments?user=appt_export_svc&password=Appt@Export2021!";
```

Productiewachtwoord en JDBC-verbindingsstring staan hardcoded in de broncode en zijn daarmee zichtbaar in de git-geschiedenis. **Niet conform S1.**

---

### S2 — PII-logging (kritiek) — ✅ OPGELOST

**Bestand:** `AppointmentServiceImpl.java`

PII (naam, geboortedatum, patiënt-ID, geslacht) verwijderd uit het logstatement. Alleen de pseudonieme `patientUuid` en `userUuid` worden nu gelogd. Zie [`04-wijzigingslog.md`](04-wijzigingslog.md) — fix/s2-remove-pii-logging. **Conform S2.**

---

### S3 — Ontbrekende `@Authorized`-annotaties

**Bestand:** `AppointmentService.java`

De methoden `getAllProviderSchedules()` en `getProviderScheduleByUuid()` hebben een lege `@Authorized()`-annotatie zonder privilege, waardoor elke gebruiker ze kan aanroepen. **Niet conform S3.**

---

### M1 — Deprecated Date-API

**Bestand:** `AppointmentServiceImpl.java` regels 1306–1311

```java
return new Date(
    date.getYear(), date.getMonth(), date.getDate(),
    time.getHours(), time.getMinutes(), time.getSeconds()
);
```

`Date.getYear()`, `getMonth()`, `getDate()` etc. zijn deprecated sinds Java 1.1. **Niet conform M1.**

---

### M2 — Methoden zetten vlag niet

**Bestand:** `AppointmentServiceImpl.java`

- `retireAppointmentType()` (regel 222) roept enkel `saveAppointmentType()` aan zonder `setRetired(true)` of `setRetireReason()` te zetten.
- `voidAppointment()`, `voidTimeSlot()` en `voidAppointmentBlock()` zetten de `voided`-vlag niet vóór opslag.

**Niet conform M2.**

---

### M3 — Typfouten in privilege-constanten

**Bestand:** `AppointmentUtils.java` regels 29–31

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Scedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Scedules";
```

"Scedules" moet "Schedules" zijn. Als de database de correcte spelling bevat, falen alle privilege-checks voor provider schedules. **Niet conform M3.**

---

### M4 — ConcurrentModificationException risico

**Bestand:** `AppointmentServiceImpl.java` regels 961–988 (`cleanOpenAppointments`)

De methode itereert via een `Iterator` maar verwijdert elementen via `appointmentsInStates.remove(appointment)`, wat een `ConcurrentModificationException` veroorzaakt. **Niet conform M4.**

---

### M5 — Ongebruikte variabele

**Bestand:** `AppointmentServiceImpl.java` regel 813

```java
boolean satisfyingConstraints = true;
```

Variabele wordt aangemaakt maar nooit gebruikt. **Niet conform M5.**

---

## Samenvatting

| Eis                              | Status        |
| -------------------------------- | ------------- |
| S1 — Geen hardcoded credentials  | Niet conform  |
| S2 — Geen PII in logs            | Conform ✅    |
| S3 — Toegangscontrole            | Deels conform |
| M1 — Geen deprecated API         | Niet conform  |
| M2 — Methoden correct            | Niet conform  |
| M3 — Constanten correct          | Conform ✅    |
| M4 — Veilige iteratie            | Conform ✅    |
| M5 — Geen ongebruikte variabelen | Conform ✅    |
