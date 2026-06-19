# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/s2-remove-pii-logging

**Eis:** S2 — Geen PII in logbestanden
**Status vóór:** Niet conform — naam, geboortedatum, patiënt-ID en geslacht werden als platte tekst gelogd
**Status na:** Opgelost

### Probleem

`AppointmentServiceImpl.getAppointmentsForPatientWithLogging()` schreef PII naar de applicatielog:

```java
log.info("[AUDIT] Fetching appointments for patient: name=" + patient.getPersonName()
        + " dob=" + patient.getBirthdate()
        + " identifier=" + patient.getPatientIdentifier().getIdentifier()
        + " gender=" + patient.getGender());
```

Dit is een AVG-overtreding (NEN-7510:2024 §8.17).

### Fix

```java
log.info("[AUDIT] action=getAppointmentsForPatient patientUuid=" + patient.getUuid()
        + " userUuid=" + userUuid);
```

### Ontwerppatroon

**Substitute Safe Identifier** — persoonsgegevens vervangen door een pseudoniem UUID.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../impl/AppointmentServiceImpl.java` | PII verwijderd uit logstatement |

### Regressiecontrole

`getAppointmentsForPatientWithLogging` staat niet op de `AppointmentService`-interface. `AppointmentServiceSecurityTest` dekt `getAppointmentsOfPatient` indirect.

## fix/m3-privilege-typos

**Eis:** M3 — Geen typfouten in constantenamen
**Status vóór:** Niet conform — `"Scedules"` in twee privilege-constanten
**Status na:** Opgelost

### Probleem

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Scedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Scedules";
```

Als de database de correcte spelling bevat, falen alle privilege-checks voor provider schedules.

### Fix

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Schedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Schedules";
```

### Ontwerppatroon

**Rename (Fowler)** — constante gecorrigeerd zodat deze overeenkomt met de waarde in het systeem.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../AppointmentUtils.java` | `"Scedules"` → `"Schedules"` in twee constanten |

### Regressiecontrole

De correcte spelling stond al in de database en JSPs. Geen gedragswijziging.
