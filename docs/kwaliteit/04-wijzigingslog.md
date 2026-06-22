# 04 - Wijzigingslog - OpenMRS Appointment Scheduler

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/trust-boundary-session

**Eis:** S — Trust Boundary Violation (CodeQL High, CWE-501)
**Prioriteit:** Hoog (2 bevindingen, GitHub Security issues #374 en #375)
**Status voor:** Gebruikersinvoer (`@RequestParam Location`) direct opgeslagen in de HTTP-sessie
**Status na:** Opgelost

### Probleem

In Spring MVC wordt `@RequestParam(value = "locationId") Location location` automatisch omgezet door een `LocationEditor`: de integer-ID uit het HTTP-verzoek wordt als sleutel gebruikt om een `Location`-object op te halen. Maar CodeQL beschouwt de binding zelf als gebruikersinvoer en markeert de directe opslag in de sessie als een trust boundary violation:

```java
// Vóór (beide controllers):
httpSession.setAttribute("chosenLocation", location);  // ← tainted by @RequestParam
```

De sessie is een vertrouwde zone binnen de applicatie. Door gebruikersinvoer er direct in te schrijven, kan kwaadaardige of onverwachte input de sessiestaat besmetten.

Gevonden in:
- `AppointmentBlockListController.java` regel 151
- `AppointmentBlockCalendarController.java` regel 150

### Fix

De `Location` expliciet opnieuw ophalen uit de database via `Context.getLocationService().getLocation()`. Dit verbreekt de taint-keten: de sessie ontvangt een object dat aantoonbaar afkomstig is van een vertrouwde bron (de database), niet van de HTTP-request.

```java
// Na (beide controllers):
Location trustedLocation = (location != null)
    ? Context.getLocationService().getLocation(location.getId()) : null;
httpSession.setAttribute("chosenLocation", trustedLocation);
```

### Ontwerppatroon

**Explicit Trust Validation** — data die een trust boundary (HTTP-request naar sessie) oversteekt, wordt eerst gevalideerd via een vertrouwde bron (de database) voordat het wordt opgeslagen. Conform OWASP Session Management Cheat Sheet.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `omod/.../web/controller/AppointmentBlockListController.java` | `location` opnieuw ophalen via `getLocation()` voor sessie-opslag (regel 151) |
| `omod/.../web/controller/AppointmentBlockCalendarController.java` | Idem (regel 150) |

### Regressiecontrole

De `Location` die in de sessie belandt is functioneel identiek aan de vorige waarde — het is hetzelfde object, nu expliciet via de database opgehaald. De GET-handler die de sessie uitleest (`chosenLocation`) ontvangt dezelfde `Location`-instantie. Geen functionele wijziging.

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

### SOLID-principe

**Single Responsibility Principle (SRP):** de loggingverantwoordelijkheid mag niet ook de verantwoordelijkheid dragen voor het formatteren van persoonsgegevens. Door alleen de UUID te loggen, houdt de methode één doel: een audittrail bijhouden, niet een patiëntprofiel.

### Alternatief en motivatie

| Alternatief | Reden afgewezen |
|---|---|
| Logging volledig verwijderen | Verlies van audittrail — vereist door NEN-7510 §8.17 (logging van toegang tot medische data) |
| Eigen maskeerformatter schrijven (`"***"` in plaats van naam) | Meer code, hogere onderhoudslast; UUID is al aanwezig in het domeinmodel en is pseudoniem zonder extra logica |
| Naam hashen (SHA-256) | Hash is technisch niet herleidbaar maar juridisch niet altijd als pseudoniem erkend; UUID voldoet eenvoudiger aan AVG art. 4(5) |

**Keuze:** UUID logging is de minimaal invasieve oplossing die de audittrail intact houdt en direct voldoet aan AVG en NEN-7510.

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

**SOLID-principe:** **Single Responsibility Principle (SRP)** — validatie van invoer is een aparte verantwoordelijkheid van de rekenlogica. Door de guard clause toe te voegen, splitsen we "is de invoer geldig?" af van "bereken het gemiddelde".

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| `try-catch ArithmeticException` | Exception-driven control flow is duurder en minder leesbaar; uitzonderingen zijn bedoeld voor uitzonderlijke situaties, niet voor verwachte randgevallen |
| `Optional<Integer>` retourneren | Vereist aanpassing van alle aanroepers; disproportioneel voor een kleine nulcheck |
| Teller initialiseren op 1 | Maskeert het onderliggende probleem in plaats van het op te lossen |

**Keuze:** Guard Clause is Java 6-compatibel, minimaal invasief en communiceert de precondition expliciet aan toekomstige lezers.

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

**SOLID-principe:** **Open/Closed Principle (OCP)** — de bestaande gedragscontracten van `getDateAndTime()` blijven ongewijzigd (gesloten voor modificatie van gedrag), terwijl de implementatie intern verbeterd wordt (open voor verbetering van kwaliteit). **SRP** — datumconstructie en tijdinstelling worden nu expliciet gescheiden via de Calendar-aanroepen.

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| `java.time.LocalDateTime` (Java 8) | Vereist Java 8; de module is gecompileerd op source level 6/8 maar draait op JVM 7 — `java.time` is niet beschikbaar op de runtime |
| Joda-Time bibliotheek | Extra dependency; disproportioneel voor één methode |
| `new GregorianCalendar(...)` constructor | Functioneel gelijkwaardig maar minder leesbaar dan de setter-aanpak; Calendar is al geïmporteerd |

**Keuze:** `Calendar` is al aanwezig in de codebase, Java 6-compatibel en levert identiek gedrag aan de deprecated `Date`-constructors.

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

**SOLID-principe:** **Single Responsibility Principle (SRP)** — een variabele die nooit wordt gelezen draagt geen verantwoordelijkheid; haar aanwezigheid suggereert ten onrechte dat ze een rol speelt in de logica.

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| Variabele behouden met commentaar `// unused` | Voegt ruis toe; Qodana blijft de warning rapporteren; intentie al duidelijk uit verwijdering |
| Variabele behouden als `@SuppressWarnings("unused")` | Maskeert het probleem in plaats van het op te lossen |

**Keuze:** Verwijdering is de enige oplossing die de codebase daadwerkelijk kleiner en begrijpelijker maakt zonder suppression-hacks.

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

**SOLID-principe:** **Interface Segregation Principle (ISP)** — `Collection` biedt `isEmpty()` als een gespecialiseerde methode precies voor dit doel; `size() == 0` omzeilt die interface en gebruikt een zwaardere operatie voor een simplere intentie.

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| `size() == 0` behouden | Minder leesbaar; vraagt om een extra mentale stap; werkt maar communiceert intentie slechter |
| `Optional` of null-check toevoegen | Disproportioneel — de collecties zijn nooit null op de aanroepplekken in kwestie |
| `Collections.emptyList()` vergelijking | Werkt niet voor alle implementaties; semantisch verkeerd |

**Keuze:** `isEmpty()` is de idiomatische Java-aanpak, heeft gelijke semantiek, en sommige collectie-implementaties optimaliseren hem intern.

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

**SOLID-principe:** **Open/Closed Principle (OCP)** — de methode-interface (`buildLocationList`) blijft gesloten voor wijziging; alleen de interne implementatiestrategie wordt verbeterd. Dit maakt de methode ook open voor grotere locatiebomen zonder performance-regressie.

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| `String.join(",", list)` | Vereist Java 8; runtime is JVM 7 — niet beschikbaar |
| `StringJoiner` | Eveneens Java 8-only |
| Streams met `Collectors.joining(",")` | Java 8 — niet beschikbaar op target runtime |
| `+=` in loop behouden | Kwadratische tijdcomplexiteit; Qodana rapporteert de warning; bij grote locatiebomen duidelijk merkbaar |

**Keuze:** `StringBuilder` is de standaard Java 6-compatibele oplossing voor efficiënte string-opbouw in loops.

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

**SOLID-principe:** **Single Responsibility Principle (SRP)** — een returnstatement heeft één taak: een waarde teruggeven. De ternary voegt een tweede laag toe (een conditie die exact dezelfde waarde herhaalt) die geen nieuwe verantwoordelijkheid draagt maar wel cognitieve overhead toevoegt.

**Alternatief en motivatie:**

| Alternatief | Reden afgewezen |
|---|---|
| Ternary behouden met commentaar | Commentaar op triviaal gedrag voegt ruis toe; de vereenvoudigde versie is zelf-documenterend |
| `Boolean.valueOf(expressie)` | Geen verbetering: extra methode-aanroep voor hetzelfde resultaat |

**Keuze:** Directe teruggave van de booleaanse expressie is de kortste, meest leesbare en meest idiomatische Java-schrijfwijze.

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
