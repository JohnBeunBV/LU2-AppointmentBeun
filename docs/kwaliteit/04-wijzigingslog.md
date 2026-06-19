# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/s2-remove-pii-logging

**Eis:** S2 — Geen PII in logbestanden  
**Status vóór:** Niet conform — naam, geboortedatum, patiënt-ID en geslacht werden als platte tekst gelogd  
**Status na:** Opgelost

### Probleem

`AppointmentServiceImpl.getAppointmentsForPatientWithLogging()` schreef de volgende PII naar de applicatielog:

```java
log.info("[AUDIT] Fetching appointments for patient: name=" + patient.getPersonName()
        + " dob=" + patient.getBirthdate()
        + " identifier=" + patient.getPatientIdentifier().getIdentifier()
        + " gender=" + patient.getGender());
```

Dit is een directe AVG-overtreding: persoonsgegevens (naam, geboortedatum, BSN-equivalent, geslacht) mogen niet als platte tekst in logbestanden staan (NEN-7510:2024 §8.17 — informatiebeveiliging in logbestanden).

### Fix

Logstatement herschreven zodat alleen de pseudonieme patiënt-UUID en de UUID van de ingelogde gebruiker worden gelogd:

```java
log.info("[AUDIT] action=getAppointmentsForPatient patientUuid=" + patient.getUuid()
        + " userUuid=" + userUuid);
```

De UUID is pseudoniem: zonder de koppeltabel in de database is een UUID niet herleidbaar tot een persoon.

### Ontwerppatroon

**Substitute Safe Identifier** — directe persoonsgegevens vervangen door een pseudoniem sleutel (UUID) die niet buiten de applicatiecontext herleidbaar is.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../impl/AppointmentServiceImpl.java` | PII verwijderd uit log statement; alleen `patientUuid` en `userUuid` gelogd |

### Regressiecontrole

`getAppointmentsForPatientWithLogging` staat niet op de `AppointmentService`-interface en is daardoor niet testbaar via de Spring-proxy. De `AppointmentAuditLogTest` en `AppointmentServiceSecurityTest` dekken de onderliggende `getAppointmentsOfPatient`-aanroep indirect. Geen regressie verwacht.
