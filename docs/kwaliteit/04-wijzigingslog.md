# 04 - Wijzigingslog - OpenMRS Appointment Scheduler

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

| Bestand                                    | Wijziging                                                                   |
| ------------------------------------------ | --------------------------------------------------------------------------- |
| `api/.../impl/AppointmentServiceImpl.java` | PII verwijderd uit log statement; alleen `patientUuid` en `userUuid` gelogd |

### Regressiecontrole

`getAppointmentsForPatientWithLogging` staat niet op de `AppointmentService`-interface en is daardoor niet testbaar via de Spring-proxy. De `AppointmentAuditLogTest` en `AppointmentServiceSecurityTest` dekken de onderliggende `getAppointmentsOfPatient`-aanroep indirect. Geen regressie verwacht.

---

## Kwaliteit-en-security---verbeteronderzoek-onderhoudbaarheid (Qodana-fixes)

**Eis:** M1, M5, extra kwaliteitsverbeteringen op basis van Qodana-scan  
**Status vóór:** 323 Qodana-warnings (baseline sprint 3)  
**Status na:** Verwachte daling naar ±280 warnings na deze fixes

### Aanleiding

Qodana for JVM rapporteerde 323 problemen bij een scan van de volledige codebase (zie §1.3 in `00-verbeteronderzoek-onderhoudbaarheid.md`). Op basis van ernst en koppeling aan bestaande kwaliteitseisen zijn de onderstaande categorieën geselecteerd voor verbetering.

---

### Fix 1 — Division by zero (M-nieuw)

**Eis:** Geen runtime-crashes door onveilige deling  
**Bestanden:** `AppointmentServiceImpl.java` regels 1096–1098 en 1162–1164

#### Probleem

`getAverageHistoryDurationByConditions()` en `getAverageHistoryDurationByConditionsPerProvider()` deelden zonder nulcheck op de teller:

```java
// Vóór:
for (Map.Entry<AppointmentType, Integer> counter : counters.entrySet())
    averages.put(counter.getKey(), averages.get(counter.getKey()) / counter.getValue());
```

Als `counter.getValue()` nul of null is, gooit dit een `ArithmeticException` of `NullPointerException` op.

#### Fix

```java
// Na:
for (Map.Entry<AppointmentType, Integer> counter : counters.entrySet())
    if (counter.getValue() != null && counter.getValue() > 0)
        averages.put(counter.getKey(), averages.get(counter.getKey()) / counter.getValue());
```

**Ontwerppatroon:** Guard Clause — valideer de precondition vóór de operatie.

---

### Fix 2 — Deprecated Date-API (M1)

**Eis:** M1 — Geen gebruik van deprecated API's  
**Bestand:** `AppointmentServiceImpl.java` methode `getDateAndTime()` regels 1347–1352

#### Probleem

`getDateAndTime()` gebruikte 7 deprecated methoden uit `java.util.Date` (deprecated sinds Java 1.1):

```java
// Vóór:
return new Date(
    date.getYear(), date.getMonth(), date.getDate(),
    time.getHours(), time.getMinutes(), time.getSeconds()
);
```

#### Fix

Herschreven met `java.util.Calendar` (reeds geïmporteerd):

```java
// Na:
Calendar dateCal = Calendar.getInstance();
dateCal.setTime(date);
Calendar timeCal = Calendar.getInstance();
timeCal.setTime(time);
dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
dateCal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
dateCal.set(Calendar.MILLISECOND, 0);
return dateCal.getTime();
```

**Ontwerppatroon:** Substitute Deprecated API — vervang verouderde methoden door hun moderne equivalenten zonder gedragswijziging.

---

### Fix 3 — Unused assignment (M5)

**Eis:** M5 — Geen ongebruikte variabelen  
**Bestand:** `AppointmentServiceImpl.java` regel 837

#### Probleem

```java
// Vóór:
boolean satisfyingConstraints = true;
```

De variabele `satisfyingConstraints` werd aangemaakt maar nooit gelezen. Dit is een code smell die Qodana categoriseert als "Unused assignment".

#### Fix

Variabele volledig verwijderd — de omliggende logica werkt zonder deze variabele.

**Ontwerppatroon:** Remove Dead Code — code zonder effect verwijderen.

---

### Fix 4 — `size() == 0` vervangen door `isEmpty()` (leesbaarheid)

**Bestanden:** 6 bestanden, 12 gevallen

#### Probleem

`Collection.size() == 0`, `size() > 0` en `size() != 0` zijn functioneel equivalent aan `isEmpty()` / `!isEmpty()`, maar minder leesbaar en potentieel minder efficiënt (sommige implementaties optimaliseren `isEmpty()`).

#### Fix (patroon, toegepast in alle gevallen)

```java
// Vóór:
if (appointment.size() > 0)
if (statuses != null && statuses.size() > 0)
if (location.getChildLocations().size() == 0)
return (inconsultationAppointments.size() != 0);

// Na:
if (!appointment.isEmpty())
if (statuses != null && !statuses.isEmpty())
if (location.getChildLocations().isEmpty())
return !inconsultationAppointments.isEmpty();
```

**Gewijzigde bestanden:**

| Bestand                               | Gevallen                 |
| ------------------------------------- | ------------------------ |
| `HibernateAppointmentDAO.java`        | 3 (regels 88, 112, 135)  |
| `HibernateAppointmentBlockDAO.java`   | 1 (regel 79)             |
| `AppointmentServiceImpl.java`         | 1 (regel 1320)           |
| `DWRAppointmentService.java`          | 2 (regels 194, 236, 258) |
| `AppointmentBlockFormController.java` | 2 (regels 253, 265)      |
| `AppointmentBlockValidator.java`      | 1 (regel 68)             |

**Ontwerppatroon:** Idiomatic API Usage — gebruik de meest expressieve en semantisch correcte methode.

---

### Fix 5 — String concatenation in loop (performance)

**Bestand:** `DWRAppointmentService.java` methode `buildLocationList()` regels 191–202

#### Probleem

```java
// Vóór:
String ans = "";
ans = location.getId() + "";
for (Location locationChild : location.getChildLocations()) {
    ans += "," + buildLocationList(locationChild);  // String += in loop: O(n²)
}
```

String concatenatie met `+=` in een loop maakt elke iteratie een nieuw String-object. Bij grote locatiebomen is dit kwadratisch in geheugen en tijd.

#### Fix

```java
// Na:
StringBuilder ans = new StringBuilder(location.getId() + "");
if (!location.getChildLocations().isEmpty()) {
    for (Location locationChild : location.getChildLocations()) {
        ans.append(",").append(buildLocationList(locationChild));
    }
}
return ans.toString();
```

**Ontwerppatroon:** Replace String Concatenation with StringBuilder — standaard refactoring voor string-opbouw in loops.

---

### Fix 6 — Redundant ternary `? true : false` (leesbaarheid)

**Bestand:** `AppointmentTypeValidator.java` regels 93 en 113

#### Probleem

```java
// Vóór:
return (appointmentName.length() > 100) ? true : false;
return (description.length() > 1024) ? true : false;
```

Een booleaanse expressie evalueren en dan `? true : false` teruggeven is redundant — de expressie zelf is al een `boolean`.

#### Fix

```java
// Na:
return appointmentName.length() > 100;
return description.length() > 1024;
```

**Ontwerppatroon:** Simplify Boolean Expression — verwijder overtollige logica.

---

### Gewijzigde bestanden (totaaloverzicht)

| Bestand                                                       | Fixes                                                                                 |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `api/.../impl/AppointmentServiceImpl.java`                    | Division by zero (×2), Deprecated Date-API (×7), Unused assignment (×1), isEmpty (×1) |
| `api/.../db/hibernate/HibernateAppointmentDAO.java`           | isEmpty (×3)                                                                          |
| `api/.../db/hibernate/HibernateAppointmentBlockDAO.java`      | isEmpty (×1)                                                                          |
| `api/.../validator/AppointmentBlockValidator.java`            | isEmpty (×1)                                                                          |
| `api/.../validator/AppointmentTypeValidator.java`             | Redundant ternary (×2)                                                                |
| `omod/.../web/DWRAppointmentService.java`                     | StringBuilder (×1), isEmpty (×2)                                                      |
| `omod/.../web/controller/AppointmentBlockFormController.java` | isEmpty (×2)                                                                          |

### Regressiecontrole

- **Division by zero:** guard clause voegt geen gedragswijziging toe bij normale waarden (teller start altijd op 1); alleen defensief voor edge-cases
- **Deprecated Date-API:** `Calendar`-implementatie produceert identieke datum/tijd-combinatie; gedekt door bestaande integratietests die tijdslot-aanmaak testen
- **isEmpty:** semantisch identiek aan `size() == 0`; geen gedragswijziging
- **StringBuilder:** output van `buildLocationList()` identiek; alleen interne implementatie efficiënter
- **Ternary simplification:** logica ongewijzigd
