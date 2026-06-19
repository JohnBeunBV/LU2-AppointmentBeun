# 03 - Bevindingen Backlog - OpenMRS Appointment Scheduler

**Module:** OpenMRS Appointment Scheduler  
**Bronnen:** CodeQL (advanced setup, `security-extended`), Qodana for JVM (323 bevindingen)  
**Scope:** Maintainability en security van de bestaande module — geen nieuwe functionaliteit, geen verwijdering van bestaande functies.

---

## Categorisering

| Categorie                            | Aantal | Aanpak                                               |
| ------------------------------------ | ------ | ---------------------------------------------------- |
| CodeQL — third-party vendor JS       | 17     | **Buiten scope** — niet onze code                    |
| CodeQL — onze JSP/workflow           | 4      | **In scope** — op te lossen                          |
| Qodana — potentiële bugs             | 4      | **In scope — hoge prioriteit**                       |
| Qodana — maintainability (onze code) | ~60    | **In scope — medium prioriteit**                     |
| Qodana — stijl/documentatie          | ~260   | **Buiten scope / laag** — noise, geen runtime impact |

---

## Sectie A — CodeQL bevindingen

### A1 — Buiten scope: third-party vendor JavaScript

De volgende CodeQL alerts betreffen JavaScript-libraries die als vendored resources in de `omod`-module zitten. Dit zijn ongewijzigde upstream bibliothekenversies die niet door ons zijn geschreven en niet losstaand kunnen worden geüpgraded zonder de gehele UI-laag te breken. Upgraden valt buiten de projectscope (vereist een OpenMRS-platform upgrade of volledige UI-herwrite).

| Alert                                 | Bestand                              | Reden buiten scope                           |
| ------------------------------------- | ------------------------------------ | -------------------------------------------- |
| #18 Incomplete regex for hostnames    | `Scripts/gcal.js:20`                 | Google Calendar JS plugin — upstream library |
| #17 DOM text reinterpreted as HTML    | `portlets/appointments.jsp:18`       | **Zie A2 — in scope**                        |
| #9 Incomplete string escaping         | `Scripts/jquery-ui.js:14054`         | jQuery UI 1.x vendored library               |
| #8 Incomplete string escaping         | `Scripts/jquery-ui.js:7197`          | jQuery UI 1.x vendored library               |
| #7 Incomplete string escaping         | `Scripts/jquery.dataTables.js:2095`  | DataTables vendored library                  |
| #6 Incomplete multi-char sanitization | `Scripts/jquery.dataTables.js:11831` | DataTables vendored library                  |
| #5 Incomplete multi-char sanitization | `js/TableTools.js:492`               | TableTools vendored library                  |
| #4 Incomplete multi-char sanitization | `js/TableTools.js:478`               | TableTools vendored library                  |
| #3 Incomplete multi-char sanitization | `Scripts/jquery.dataTables.js:4062`  | DataTables vendored library                  |
| #2 Incomplete multi-char sanitization | `Scripts/jquery.dataTables.js:3843`  | DataTables vendored library                  |
| #1 Incomplete multi-char sanitization | `Scripts/jquery.dataTables.js:2440`  | DataTables vendored library                  |
| #16 Unsafe jQuery plugin              | `Scripts/jquery-ui.js:12126`         | jQuery UI 1.x vendored library               |
| #15 Unsafe jQuery plugin              | `Scripts/jquery-ui.js:8607`          | jQuery UI 1.x vendored library               |
| #14 Unsafe jQuery plugin              | `support/jquery.jeditable.js:447`    | jEditable vendored library                   |
| #13 Unsafe jQuery plugin              | `support/jquery.jeditable.js:432`    | jEditable vendored library                   |
| #12 Unsafe HTML from library input    | `Scripts/jquery-ui.js:7769`          | jQuery UI 1.x vendored library               |
| #11 Unsafe HTML from library input    | `Scripts/fullcalendar.js:759`        | FullCalendar vendored library                |
| #10 Unsafe HTML from library input    | `Scripts/fullcalendar.js:347`        | FullCalendar vendored library                |

**Besluit:** Gedocumenteerd als geaccepteerd restrisico. De vendor-JS wordt uitsluitend door de OpenMRS-omgeving geserveerd en is niet publiek benaderbaar buiten de authenticatielaag. Mitigatie via platform-upgrade valt buiten huidige projectscope.

---

### A2 — In scope: DOM XSS in appointments.jsp

| Alert                              | Bestand                        | Prioriteit |
| ---------------------------------- | ------------------------------ | ---------- |
| #17 DOM text reinterpreted as HTML | `portlets/appointments.jsp:18` | **Hoog**   |

**Bevinding:** Een JSP-expressie wordt direct als HTML-inhoud gerenderd, waardoor invoer van de server als DOM-markup wordt geïnterpreteerd. Dit is onze eigen code.

**Actie:** Gebruik JSTL `<c:out>` of expliciete HTML-escaping in plaats van directe expressie-output. Te implementeren in de volgende sprint.

---

### A3 — In scope: ontbrekende `permissions:` in workflow jobs

| Alert                             | Locatie                                  | Prioriteit |
| --------------------------------- | ---------------------------------------- | ---------- |
| #169 Workflow missing permissions | `pipeline.yml:440` (`deploy-prod`)       | **Medium** |
| #168 Workflow missing permissions | `pipeline.yml:376` (`deploy-acceptance`) | **Medium** |
| #167 Workflow missing permissions | `pipeline.yml:310` (`deploy-test`)       | **Medium** |

**Bevinding:** De drie deploy-jobs in `pipeline.yml` hebben geen expliciete `permissions:` block. GitHub Actions valt dan terug op de repository-brede standaard (vaak te ruim). CodeQL markeert dit als een potentieel privilege-escalatierisico in de workflow zelf.

**Actie:** Voeg `permissions: contents: read` toe aan elke deploy-job (SSH-deploy vereist geen schrijfrechten op de repo). Makkelijk te fixen in dezelfde PR als A2.

---

## Sectie B — Qodana bevindingen

### B1 — Potentiële bugs (hoge prioriteit)

#### B1.1 — DivideByZero en NumericOverflow in StudentT.java

| Regel | Bestand                 | Bevinding                                         |
| ----- | ----------------------- | ------------------------------------------------- |
| 152   | `api/.../StudentT.java` | Division by zero + Numeric overflow in expression |

**Bevinding:** Qodana detecteert op regel 152 een rekenkundige expressie die bij specifieke invoerwaarden een deling door nul of integer-overflow veroorzaakt. `StudentT` is een statistische klasse die wordt gebruikt voor de appointment-scheduling berekeningen.

**Impact:** Runtime `ArithmeticException` of stille fout in statistische berekening bij randgevallen. Heeft geen directe invloed op de primaire planningsfunctie maar kan onverwacht falen bij specifieke datapatronen.

**Actie:** Analyseer regel 152 van `StudentT.java` en voeg een guard toe (`if (denominator == 0)` of gebruik `double` in plaats van `int` voor de tussenliggende berekening).

---

#### B1.2 — ConstantValue (altijd-waar/altijd-vals condities)

| Bestand                                   | Regel  | Bevinding                                                             |
| ----------------------------------------- | ------ | --------------------------------------------------------------------- |
| `HibernateSingleClassDAO.java`            | 84, 92 | `includeRetired` is altijd `false`; `includeVoided` is altijd `false` |
| `AppointmentSchedulerSetup.java`          | 24     | Conditie `task == null` is altijd `true`                              |
| `AppointmentBlockFormController.java`     | 242    | Conditie `!timeSlotLength.isEmpty()` is altijd `true`                 |
| `AppointmentBlockCalendarController.java` | 159    | Conditie `toDate != null` is altijd `true`                            |

**Bevinding:** Qodana stelt vast dat deze condities op basis van dataflow-analyse nooit anders dan één vaste waarde aannemen. Dit duidt op dode code, overbodige checks, of een logicafout waarbij een guard-conditie altijd onwaar of altijd waar is.

**Impact:** `AppointmentSchedulerSetup.java:24` is het meest risicovol — als de conditie altijd `true` is, wordt de setup-logica altijd uitgevoerd, ook als dat niet de bedoeling is.

**Actie:** Per geval onderzoeken:

- `HibernateSingleClassDAO`: verwijder de overbodige parameter als hij nooit wordt gebruikt.
- `AppointmentSchedulerSetup`: controleer of de null-check bedoeld is en of de logica klopt.
- Controllers: verwijder de dode conditie-takken.

---

#### B1.3 — EmptyStatementBody (lege if/else-lichamen)

| Bestand                                  | Regel | Bevinding                               |
| ---------------------------------------- | ----- | --------------------------------------- |
| `AppointmentSettingsFormController.java` | 47    | `if`-statement heeft een leeg lichaam   |
| `AppointmentStatusHistoryValidator.java` | 55    | `else`-statement heeft een leeg lichaam |

**Bevinding:** Een lege `if`- of `else`-tak duidt op onvolledige implementatie of vergeten logica. Dit verlaagt de leesbaarheid en geeft valse geruststelling dat een conditie is afgehandeld.

**Actie:** Voeg commentaar toe dat expliciet de bewuste keuze verklaart (`// intentionally empty — X is handled by Y`), of implementeer de ontbrekende logica.

---

### B2 — Maintainability (medium prioriteit)

Dit zijn Qodana-bevindingen in **onze eigen code** die de onderhoudbaarheid direct beïnvloeden. Alle zijn oplosbaar zonder functionaliteitswijziging.

#### B2.1 — SizeReplaceableByIsEmpty (12 gevallen)

`collection.size() > 0` → `!collection.isEmpty()`  
`collection.size() != 0` → `!collection.isEmpty()`

Bestanden: `HibernateAppointmentDAO.java`, `HibernateAppointmentBlockDAO.java`, `DWRAppointmentService.java` en anderen.

**Actie:** Mechanische vervanging. Verhoogt leesbaarheid en intentie-uitdrukking. Lage effort, geen risicowijziging.

---

#### B2.2 — UnnecessaryLocalVariable (8 gevallen)

Lokale variabelen die direct worden aangemaakt en teruggegeven zonder tussenliggende bewerking.

Voorbeelden:

- `AppointmentStatusResource1_9.java:25` — `simpleObject` direct geretourneerd
- `HibernateAppointmentBlockDAO.java:92` — `appointmentBlocks` direct geretourneerd
- `AppointmentBlockFormController.java:130` — `defaultTimeSlotLength` direct geretourneerd

**Actie:** Inline maken of verwijderen. Verlaagt cognitieve complexiteit zonder functionaliteitswijziging.

---

#### B2.3 — WrapperTypeMayBePrimitive (9 gevallen)

Variabelen gedeclareerd als `Integer`, `Boolean` etc. terwijl `int`, `boolean` volstaat (geen null-gebruik).

Bestanden: `AppointmentActivator.java`, `AppointmentBlockFormController.java`, `AppointmentBlockCalendarController.java`, `PatientDashboardAppointmentExt.java`.

**Actie:** Vervang wrapper-type door primitief type. Vermijdt autoboxing-overhead en maakt null-veiligheid explicieter.

---

#### B2.4 — UseBulkOperation (4 gevallen)

For-each loops die één voor één elementen toevoegen aan een collectie, vervangbaar door `addAll()`.

Bestanden: `AppointmentBlockFormController.java:181`, `AppointmentBlockListController.java:200`, `AppointmentServiceImpl.java:1329`, `AppointmentListController.java:113`.

**Actie:** Vervang loop door `targetList.addAll(sourceList)`. Compacter en sneller.

---

#### B2.5 — TrivialIf (2 gevallen)

`if`-statements die kunnen worden vereenvoudigd tot een directe expressie.

Bestanden: `AppointmentBlockListController.java:128`, `AppointmentFormController.java:83`.

**Actie:** Vereenvoudig de conditie direct.

---

#### B2.6 — SimplifiableConditionalExpression (2 gevallen)

`AppointmentTypeValidator.java` regels 93 en 113:

```java
// Huidige code:
(description.length() > 1024) ? true : false
// Correct:
description.length() > 1024
```

**Actie:** Directe vervanging. Geen gedragswijziging.

---

#### B2.7 — UnusedAssignment (15 gevallen)

Variabelen die een waarde krijgen toegewezen die nooit wordt gelezen voor de volgende overschrijving. Dit is vergelijkbaar met M5 (ongebruikte variabele) maar betreft tussenliggende toewijzingen.

**Actie:** Per geval beoordelen. Sommige zijn init-patronen voor debuggen; andere zijn echte dode toewijzingen die verwijderd kunnen worden.

---

### B3 — Buiten scope: stijl- en documentatiebevindingen

De volgende Qodana-categorieën zijn **bewust buiten scope** geplaatst. Ze vormen geen runtime-risico en vereisen disproportioneel veel inspanning ten opzichte van de onderhoudswinstpunten.

| Categorie                                  | Aantal | Reden buiten scope                                                                                                       |
| ------------------------------------------ | ------ | ------------------------------------------------------------------------------------------------------------------------ |
| `JavadocReference`                         | 56     | Ontbrekende/foutieve Javadoc-referenties in bestaande code. Documentatieprobleem, geen functioneel risico.               |
| `UNCHECKED_WARNING`                        | 55     | Unchecked generics-casts — inherent aan OpenMRS 1.9.9 API (pre-generics design). Niet oplosbaar zonder platform-upgrade. |
| `JavadocDeclaration`                       | 47     | Javadoc-declaratieproblemen. Zelfde reden als JavadocReference.                                                          |
| `UnnecessaryModifier`                      | 17     | Onnodige `public`/`abstract` op interface-methoden. Stijlkwestie, geen risico.                                           |
| `NonSerializableWithSerialVersionUIDField` | 16     | `serialVersionUID` in niet-serialiseerbare klassen. Erfenis van OpenMRS entity-patroon.                                  |
| `DefaultAnnotationParam`                   | 11     | Expliciete standaardwaarden in annotaties. Stijlkwestie.                                                                 |
| `UNUSED_IMPORT`                            | 6      | Ongebruikte imports. Eenvoudig te verwijderen maar buiten prioriteit voor deze sprint.                                   |

---

## Samenvatting prioritering

| ID   | Bevinding                              | Prioriteit | Effort | Status           |
| ---- | -------------------------------------- | ---------- | ------ | ---------------- |
| A2   | DOM XSS in appointments.jsp            | Hoog       | Laag   | Te doen          |
| A3   | Pipeline workflow permissions          | Medium     | Laag   | Te doen          |
| B1.1 | DivideByZero/Overflow StudentT.java    | Hoog       | Medium | Te doen          |
| B1.2 | ConstantValue — dode condities         | Medium     | Medium | Te doen          |
| B1.3 | EmptyStatementBody                     | Laag       | Laag   | Te doen          |
| B2.1 | SizeReplaceableByIsEmpty (12x)         | Laag       | Laag   | Te doen          |
| B2.2 | UnnecessaryLocalVariable (8x)          | Laag       | Laag   | Te doen          |
| B2.3 | WrapperTypeMayBePrimitive (9x)         | Laag       | Laag   | Te doen          |
| B2.4 | UseBulkOperation (4x)                  | Laag       | Laag   | Te doen          |
| B2.5 | TrivialIf (2x)                         | Laag       | Laag   | Te doen          |
| B2.6 | SimplifiableConditionalExpression (2x) | Laag       | Laag   | Te doen          |
| B2.7 | UnusedAssignment (15x)                 | Laag       | Medium | Te doen          |
| A1   | Vendor JS libraries (17 alerts)        | —          | —      | **Buiten scope** |
| B3   | Javadoc/stijl/unchecked (>250x)        | —          | —      | **Buiten scope** |
