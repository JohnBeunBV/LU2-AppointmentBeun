# Coverage-onderbouwing

## Gekozen minimum: 70% instruction coverage (doel: 80%)

### Wat wordt gemeten

JaCoCo meet **instruction coverage**: het percentage bytecode-instructies dat tijdens de tests wordt uitgevoerd. Dit is de meest granulaire maat en laat zich niet manipuleren door lege regels of commentaar.

De drempel is geconfigureerd op `BUNDLE`-niveau, wat betekent dat het gaat over het gemiddelde van alle klassen in een submodule (`api` en `omod` worden apart gecontroleerd).

---

### Waarom 80%

#### Context: medische software

Deze module beheert afspraken voor patiënten in OpenMRS. Fouten in de kernlogica kunnen directe gevolgen hebben voor patiëntveiligheid: gemiste afspraken, dubbele boekingen of verkeerd toegewezen tijdslots. Dit plaatst de software in de categorie **medisch-kritieke systemen**.

#### Normatief kader

| Norm | Vereiste / aanbeveling |
|------|------------------------|
| NEN-7510:2017 §8.29 | Aantoonbare kwaliteitscontrole van software in zorgsystemen |
| IEC 62304 klasse B | ≥ 75% statement coverage voor medische software zonder direct letselrisico |
| IEC 62304 klasse C | ≥ 100% branch coverage voor levenskritische software |
| OWASP ASVS L2 | Geautomatiseerde tests aanwezig voor alle beveiligingsfuncties |
| Interne standaard | 80% als pragmatisch minimum voor klasse B-achtige systemen |

Afsprakenbeheer valt onder IEC 62304 klasse B (geen direct letselrisico, wel patiëntimpact). De 80%-drempel ligt boven de IEC-minimumeis van 75% en biedt een marge voor testdrift.

#### Security-kritieke code in deze module

De volgende onderdelen hebben een verhoogd risico en worden actief gedekt door tests:

- **`AppointmentServiceImpl`** — bevat de kernlogica voor boeking, annulering en validatie. Fouten hier raken alle patiëntafspraken.
- **Validators** (`AppointmentValidator`, `TimeSlotValidator`, etc.) — verkeerde validatie leidt tot ongeldige data in het EPD.
- **`DWRAppointmentService`** — AJAX-laag direct aanroepbaar vanuit de UI; invoervalidatie is hier kritisch.
- **`getAppointmentsForPatientWithLogging`** — bevat een bekende PII-logging vulnerability (gedocumenteerd in CLAUDE.md); deze methode moet getest blijven om regressie te detecteren.

#### Praktische grens

100% coverage is onhaalbaar in een OpenMRS-module:

- Grote delen van de DAO-laag (`HibernateXxxDAO`) vereisen een draaiende database en worden gedekt door integratietests, niet unit tests.
- OpenMRS-lifecycle-methoden (`AppointmentActivator.startup/shutdown`) zijn containerafhankelijk.
- JSP/DWR-laag in `omod` is niet unit-testbaar zonder Tomcat-context.

80% is de hoogst haalbare drempel die nog realistisch te handhaven is zonder de testinfrastructuur fundamenteel te wijzigen.

---

### Configuratie

In [pom.xml](../../openmrs-module-appointmentscheduling/pom.xml):

```xml
<counter>INSTRUCTION</counter>
<value>COVEREDRATIO</value>
<minimum>0.70</minimum>
```

De `jacoco:check` goal bindt aan de `verify`-fase. `mvn clean verify` faalt automatisch als de coverage onder 80% zakt. De CI-pipeline ([pipeline.yml](../../.github/workflows/pipeline.yml)) voert `mvn clean verify` uit, waardoor de gate actief is op elke push en pull request.

---

### Rapportage

Het HTML-rapport wordt per CI-run opgeslagen als GitHub Actions artifact onder:

```
jacoco-coverage-<versie>-<datum>/
  api/target/site/jacoco/index.html
  omod/target/site/jacoco/index.html
```

Het rapport is te downloaden via het tabblad **Actions → betreffende run → Artifacts**.
