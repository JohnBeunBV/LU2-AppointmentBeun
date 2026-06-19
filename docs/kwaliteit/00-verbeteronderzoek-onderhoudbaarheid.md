# Verbeteronderzoek Onderhoudbaarheid
**Module:** OpenMRS Appointment Scheduler  
**Norm:** NEN-7510:2024  
**Sprint:** LU2

---

## 1. Analyse Onderhoudbaarheid

> Zie ook: [01-security-&-maintainability.md](01-security-%26-maintainability.md) en [02-coverage-onderbouwing.md](02-coverage-onderbouwing.md)

### 1.1 Non-functional requirements en tooling

De volgende non-functional requirements zijn vastgesteld op basis van de NEN-7510:2024 norm en IEC 62304 klasse B (medische software zonder direct letselrisico). Per eis is de tooling geconfigureerd die dit automatisch meet en het CI-proces laat falen bij niet-voldoen.

| NFR | Meetbare grens | Tooling | CI-gedrag bij niet-voldoen |
|-----|---------------|---------|---------------------------|
| Instruction coverage ≥ 70% (`api`-module) | COVEREDRATIO ≥ 0.70 | JaCoCo 0.8.11 (`jacoco:check`) | `mvn verify` faalt; build stopt |
| Geen kritieke code smells of bugs | SonarQube quality gate PASSED | SonarCloud | Merge naar `main`/`release/*` geblokkeerd |
| Geen dependency-kwetsbaarheden (CVSS ≥ 7) | 0 high/critical CVE's | Snyk | Pipeline faalt bij `snyk test` |
| Geen container-kwetsbaarheden (CRITICAL/HIGH) | 0 CRITICAL/HIGH CVE's in image | Trivy | Pipeline faalt na docker build |
| Geen secrets in broncode | 0 gevonden secrets | Gitleaks | Eerste job in pipeline; blokkeert alles |

Deze tooling is geconfigureerd in `.github/workflows/pipeline.yml` en `openmrs-module-appointmentscheduling/pom.xml`. De JaCoCo-drempel is gekozen op basis van IEC 62304 klasse B (≥ 75% aanbevolen) en pragmatisch verlaagd naar 70% als harde minimumgrens, met 80% als streefdoel — zie [02-coverage-onderbouwing.md](02-coverage-onderbouwing.md) voor de volledige onderbouwing.

---

### 1.2 Kwaliteitseisen

| # | Eis | Norm |
|---|-----|------|
| M1 | Geen gebruik van deprecated API's | Java-methoden als `Date.getYear()` zijn deprecated sinds Java 1.1 |
| M2 | Methoden doen wat ze beloven | `retireAppointmentType`, `voidAppointment` moeten de bijbehorende vlag zetten vóór opslag |
| M3 | Geen typfouten in constantenamen | Privilege-constanten in `AppointmentUtils` moeten exact overeenkomen met de databasewaarden |
| M4 | Correcte iteratie over collecties | Verwijderen tijdens iteratie mag alleen via `Iterator.remove()` |
| M5 | Geen ongebruikte variabelen | Aangemaakt maar nooit gebruikte variabelen moeten worden verwijderd |
| M6 | Code coverage | Minimale instruction coverage van 70% (streven: 80%) — IEC 62304 klasse B vereist ≥ 75% |

### Bevindingen

#### M1 — Deprecated Date-API
**Bestand:** `AppointmentServiceImpl.java` regels 1306–1311

```java
return new Date(
    date.getYear(), date.getMonth(), date.getDate(),
    time.getHours(), time.getMinutes(), time.getSeconds()
);
```

`Date.getYear()`, `getMonth()` etc. zijn deprecated sinds Java 1.1. **Niet conform M1.**

---

#### M2 — Methoden zetten vlag niet
**Bestand:** `AppointmentServiceImpl.java`

- `retireAppointmentType()` roept enkel `saveAppointmentType()` aan zonder `setRetired(true)` of `setRetireReason()` te zetten.
- `voidAppointment()`, `voidTimeSlot()` en `voidAppointmentBlock()` zetten de `voided`-vlag niet vóór opslag.

**Niet conform M2.**

---

#### M3 — Typfouten in privilege-constanten
**Bestand:** `AppointmentUtils.java` regels 29–31

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Scedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Scedules";
```

"Scedules" moet "Schedules" zijn. Als de database de correcte spelling bevat, falen alle privilege-checks voor provider schedules. **Niet conform M3.**

---

#### M4 — ConcurrentModificationException risico
**Bestand:** `AppointmentServiceImpl.java` regels 961–988 (`cleanOpenAppointments`)

De methode itereert via een `Iterator` maar verwijdert elementen via `appointmentsInStates.remove(appointment)`, wat een `ConcurrentModificationException` veroorzaakt. **Niet conform M4.**

---

#### M5 — Ongebruikte variabele
**Bestand:** `AppointmentServiceImpl.java` regel 813

```java
boolean satisfyingConstraints = true;
```

Variabele wordt aangemaakt maar nooit gebruikt. **Niet conform M5.**

---

#### M6 — Code Coverage (meting vóór verbeteringen)
| Module | Coverage vóór |
|--------|--------------|
| `api`  | 70%          |
| `omod` | 32%          |

Gemeten via JaCoCo op instruction-niveau. De `api`-module zat precies op de minimumdrempel; elke regressie zou de build doen falen.

---

### Samenvatting analyse

| Eis | Status vóór verbetering |
|-----|------------------------|
| M1 — Geen deprecated API | Niet conform |
| M2 — Methoden correct | Niet conform |
| M3 — Constanten correct | Niet conform |
| M4 — Veilige iteratie | Niet conform |
| M5 — Geen ongebruikte variabelen | Niet conform |
| M6 — Code coverage ≥ 70% | Net conform (grenswaarde) |

---

## 2. Testopzet en Testresultaten

### Teststrategie

Er zijn twee testtypen ingezet:

| Testtype | Klasse | Doel |
|----------|--------|------|
| Unit test | `AppointmentValidatorTest` | Valideert invoervalidatie zonder database of OpenMRS-context |
| Integratietest | `AppointmentServiceSecurityTest` | Test security-kritieke methoden met echte H2-database en OpenMRS-context |
| Integratietest | `AppointmentResource1_9Test` | Test ACL-logica in de REST resource (enforceProviderAcl) |

---

### 2.1 AppointmentValidatorTest

**Bestand:** `api/src/test/java/.../validator/AppointmentValidatorTest.java`  
**Type:** Pure unit test — geen database, geen Spring context.

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `supports_shouldSupportAppointmentClass` | `supports(Appointment.class)` | `true` |
| `supports_shouldNotSupportArbitraryClass` | `supports(Object.class)` | `false` |
| `validate_nullAppointment_shouldRejectWithGeneralError` | `validate(null, errors)` | Error code `error.general` |
| `validate_appointmentWithNullTimeSlot_shouldRejectTimeSlotField` | Afspraak zonder tijdslot | FieldError op `timeSlot` |
| `validate_appointmentWithNullPatient_shouldRejectPatientField` | Afspraak zonder patiënt | FieldError op `patient` |
| `validate_appointmentWithNullType_shouldRejectAppointmentTypeField` | Afspraak zonder type | FieldError op `appointmentType` |
| `validate_appointmentTypeNotSupportedByBlock_shouldRejectWithNotSupportedTypeCode` | Type staat niet in het blok | Error code `notSupportedType` |
| `validate_fullyValidAppointment_shouldProduceNoErrors` | Volledig geldige afspraak | Geen errors |

---

### 2.2 AppointmentServiceSecurityTest

**Bestand:** `api/src/test/java/.../api/AppointmentServiceSecurityTest.java`  
**Type:** Integratietest — extends `BaseModuleContextSensitiveTest`, laadt `standardAppointmentTestDataset.xml`.

#### PII-logging vulnerability

| Testmethode | Scenario | Doel |
|-------------|----------|------|
| `getAppointmentsForPatientWithLogging_shouldReturnSameResultAsGetAppointmentsOfPatient` | Patiënt 1 | Documenteert vulnerability; bewaakt returnwaarde op regressie |
| `getAppointmentsForPatientWithLogging_shouldNotReturnNullForKnownPatient` | Patiënt 2 | Null-check op resultaat |

#### bookAppointment guards

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `bookAppointment_onAlreadyPersistedAppointment_shouldThrowAPIException` | Afspraak met bestaand id | `APIException` |
| `bookAppointment_whenSlotFullAndOverbookFalse_shouldThrowTimeSlotFullException` | Slot vol, overbook=false | `TimeSlotFullException` |
| `bookAppointment_whenSlotFullAndOverbookTrue_shouldSucceed` | Slot vol, overbook=true | Boeking geslaagd |
| `bookAppointment_withNullStatus_shouldDefaultToScheduled` | Nieuwe afspraak zonder status | Status = `SCHEDULED` |

#### changeAppointmentStatus

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `changeAppointmentStatus_shouldUpdateStatusAndCreateHistoryEntry` | SCHEDULED → COMPLETED | Status bijgewerkt, history aanwezig |
| `changeAppointmentStatus_withNullAppointment_shouldNotThrow` | null meegeven | Geen NPE |

---

### 2.3 AppointmentResource1_9Test (ACL)

**Bestand:** `omod/src/test/java/.../rest/resource/openmrs1_9/AppointmentResource1_9Test.java`  
**Type:** Integratietest — same-package toegang tot package-private methode `enforceProviderAcl`.

| Testmethode | Scenario | Verwacht resultaat |
|-------------|----------|--------------------|
| `enforceProviderAcl_superuserReturnsRequestedProvider` | Admin (superuser) vraagt willekeurige provider | Gevraagde provider teruggegeven |
| `enforceProviderAcl_nonSuperuserWithProviderReturnsOwnProvider` | Niet-superuser is gekoppeld aan provider | Eigen provider teruggegeven |
| `enforceProviderAcl_nonSuperuserRequestingOtherProviderThrows` | Niet-superuser vraagt andere provider | `APIAuthenticationException` |
| `enforceProviderAcl_nonSuperuserWithNoProviderThrows` | Niet-superuser niet gekoppeld aan actieve provider | `APIAuthenticationException` |

---

### Testresultaten

> **TODO:** Voeg hier een screenshot of copy-paste van de CI-testoutput in (mvn test uitvoer / GitHub Actions run).
> Verwacht: alle bovenstaande tests groen, geen regressie in bestaande tests.

---

## 3. Verbeteringen — Prioritering en Onderbouwing

De volgende verbeteringen zijn geselecteerd op basis van de analyse in sectie 1, geprioriteerd op **impact** (ernst van het risico) en **effort** (implementatie-inspanning).

| Prioriteit | Bevinding | Impact | Effort | Onderbouwing |
|-----------|-----------|--------|--------|--------------|
| 1 | **R03 — Data-level ACL** (toegangscontrole REST API) | Hoog — providers zien elkaars afspraken | Medium | Directe overtreding van NEN-7510 toegangscontrole; uitgebuite kwetsbaarheid geeft datalekrisico |
| 2 | **M6 — Code coverage** (JaCoCo gate toevoegen) | Hoog — regressies worden niet gedetecteerd | Laag | Coverage was 70% zonder gate; elke onbedekte regressie gaat onopgemerkt naar productie |
| 3 | **M5 — Ongebruikte variabele** (`satisfyingConstraints`) | Laag — geen runtime impact | Laag | SonarQube code smell; verlaagt leesbaarheid; eenvoudig te verwijderen |
| 4 | **M1 — Deprecated Date-API** | Medium — breekt mogelijk op toekomstige JVM | Medium | Deprecated sinds Java 1.1; risicovolle technische schuld voor toekomstige upgrades |
| 5 | **M2 — Methoden zetten vlag niet** | Hoog — retire/void werkt niet correct | Hoog | Fundamentele logicafout; vergt uitgebreide regressietests |
| 6 | **M3 — Typfouten privilege-constanten** | Hoog — privilege-checks falen stil | Laag | Makkelijk te fixen maar grote impact; risico op rechtenescalatie |
| 7 | **M4 — ConcurrentModificationException** | Medium — runtime crash in `cleanOpenAppointments` | Laag | Latente fout; treedt op bij gelijktijdige taken |

**Gerealiseerde verbeteringen in deze sprint:** prioriteit 1 (R03 ACL) en prioriteit 2 (coverage gate). De overige bevindingen zijn gedocumenteerd in de security backlog voor opvolging.

---

## 4. Aangepast Ontwerp

### 4.1 Probleem: Ontbrekende data-level toegangscontrole (R03)

**Situatie vóór verbetering:**  
`AppointmentResource1_9.doSearch()` gaf alle providers' afspraken terug aan iedere ingelogde gebruiker. Er was geen controle of de opvragende gebruiker gemachtigd was voor de gevraagde provider.

**Ontwerpkeuze: Extract Method + toegangscontrole in de REST-laag**

De ACL-logica is geëxtraheerd naar een aparte methode `enforceProviderAcl()` in de resource-klasse. Dit volgt het **Extract Method** refactoringpatroon (Fowler) om testbaarheid te verhogen zonder de publieke API te wijzigen.

```
doSearch()
    └── enforceProviderAcl(currentUser, requestedProvider)
            ├── isSuperUser() → return requestedProvider (superuser mag alles)
            ├── getProvidersByPerson() leeg → APIAuthenticationException
            ├── requestedProvider ≠ ownProvider → APIAuthenticationException
            └── return ownProvider
```

**Alternatief overwogen: toegangscontrole in de service-laag (`AppointmentServiceImpl`)**  
Dit zou de beveiliging dichter bij de data plaatsen (defence in depth). Nadeel: `AppointmentServiceImpl` heeft geen directe toegang tot de HTTP-context en de huidige `@Authorized`-annotaties werken op privilege-niveau, niet op data-niveau. Gekozen voor de resource-laag omdat daar de gebruikersidentiteit beschikbaar is via `Context.getAuthenticatedUser()`.

**Toegepaste principes:**
- **Extract Method** (Fowler) — testbaar maken van inline logica
- **Fail Fast** — exception direct bij onbevoegde toegang, vóór database-aanroep
- **Least Privilege** — niet-superusers zien alleen hun eigen provider

**Zichtbaarheid:** `enforceProviderAcl` is `package-private` (geen `public`, geen `private`). Dit maakt directe unit-testbaarheid vanuit hetzelfde package mogelijk zonder de publieke API van de resource te vervuilen.

---

### 4.2 Probleem: Ontbrekende coverage gate (M6)

**Situatie vóór verbetering:**  
Er was geen automatische coverage-meting. De coverage van de `api`-module was 70% maar kon zonder waarschuwing dalen.

**Ontwerpkeuze: JaCoCo als build gate in de parent POM**

JaCoCo 0.8.11 is geconfigureerd als Maven plugin op `BUNDLE`-niveau met een minimumdrempel van 70% instruction coverage. De `omod`-module is uitgesloten van de gate (maar niet van rapportage) omdat de omod-laag Tomcat-afhankelijke code bevat die structureel niet unit-testbaar is.

**Alternatief overwogen: alleen rapportage, geen gate**  
Een gate faalt de build actief bij onderschrijding; pure rapportage doet dat niet. Gekozen voor een gate omdat zonder harde grens de drempel niet wordt gehandhaafd in CI.

---

## 5. Realisatie (PoC) & Verantwoording

### Gerealiseerde wijzigingen

| Bestand | Wijziging |
|---------|-----------|
| `AppointmentResource1_9.java` | `enforceProviderAcl()` methode toegevoegd; aangeroepen vanuit `doSearch()` |
| `AppointmentResource1_9Test.java` | 4 nieuwe tests voor alle branches van `enforceProviderAcl` |
| `AppointmentServiceSecurityTest.java` | 8 nieuwe integratietests voor security-kritieke servicemethoden |
| `AppointmentValidatorTest.java` | 8 nieuwe unit tests voor `AppointmentValidator` |
| `pom.xml` (parent) | JaCoCo 0.8.11 toegevoegd met 70% instruction coverage gate |
| `omod/pom.xml` | JaCoCo check uitgeschakeld voor omod-module |
| `.github/workflows/pipeline.yml` | Coverage rapport als CI-artifact; Sonar/Snyk/Trivy/Gitleaks integratie |

### AI-tooling verantwoording

Bij de realisatie is gebruik gemaakt van **Claude Code (Anthropic)** als AI-assistent. Inzet:

- Analyseren van bestaande code op conformiteit met de kwaliteitseisen
- Opstellen van testcases voor `enforceProviderAcl` op basis van het testdataset-schema
- Debuggen van mislukte integratietests (context-authenticatie gedrag van `MainResourceControllerTest`)
- Suggesties voor de Extract Method refactoring

**Kritische reflectie:**  
De AI-assistent gaf initieel testopzetten die uitgingen van `Context.logout()` / `Context.authenticate()` om van gebruiker te wisselen in integratietests. Dit werkte niet: `MainResourceControllerTest.handle()` authenticateert altijd opnieuw als admin. Na eigen analyse van het OpenMRS testframework is de aanpak gewijzigd naar directe unit-test van de geëxtraheerde methode. De uiteindelijke implementatie is handmatig geverifieerd en begrepen vóór commit.

---

## 6. Validatie Verbeteringen (Testen & Regressie)

### Coverage voor vs. na

| Module | Voor | Na |
|--------|------|----|
| `api`  | 70%  | ~73–75% (verwacht na nieuwe tests) |
| `omod` | 32%  | 32% (gate uitgeschakeld; rapport beschikbaar) |

> **TODO:** Vervang bovenstaande schatting door de werkelijke cijfers uit het JaCoCo-rapport (downloadbaar als CI-artifact na een succesvolle run).

---

### Regressiecontrole

De bestaande testsuite bevat tests die de kernfunctionaliteit afdekken. Na de wijzigingen zijn alle bestaande tests opnieuw uitgevoerd:

| Testsuite | Resultaat |
|-----------|-----------|
| `AppointmentServiceTest` | > **TODO: resultaat invoegen** |
| `AppointmentResource1_9ControllerTest` | > **TODO: resultaat invoegen** |
| `AppointmentResource1_9Test` | > **TODO: resultaat invoegen** |
| `AppointmentServiceSecurityTest` | > **TODO: resultaat invoegen** |
| `AppointmentValidatorTest` | > **TODO: resultaat invoegen** |

> **TODO:** Voeg hier een screenshot van de CI-run toe (GitHub Actions → build-and-test → test output) als reproduceerbaar bewijs.

---

### Bewijsvoering ACL-verbetering

De ACL-logica dekt de volgende scenario's aantoonbaar via tests:

| Scenario | Test | Bewijs |
|----------|------|--------|
| Superuser mag alle providers zien | `enforceProviderAcl_superuserReturnsRequestedProvider` | Assert: returned provider == requested provider |
| Niet-superuser ziet alleen eigen provider | `enforceProviderAcl_nonSuperuserWithProviderReturnsOwnProvider` | Assert: UUID eigen provider |
| Niet-superuser mag andere provider niet zien | `enforceProviderAcl_nonSuperuserRequestingOtherProviderThrows` | `APIAuthenticationException` verwacht |
| Gebruiker zonder provider-koppeling geblokkeerd | `enforceProviderAcl_nonSuperuserWithNoProviderThrows` | `APIAuthenticationException` verwacht |
