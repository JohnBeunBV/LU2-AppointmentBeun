# 02 — Pipeline Compliance

Dit document beschrijft de GitHub-maatregelen die zijn ingericht om de kwaliteit en veiligheid van wijzigingen te borgen. De nadruk ligt op branch protection, verplichte reviews en de geautomatiseerde build-pipeline.

---

## Branch Protection Rules

Op de repository zijn branch protection rules ingesteld voor drie branches: `main`, `develop` en `release/*`. Elke branch heeft 4 actieve regels.

**Bewijs:**

![Branch protection rules](../../images/Screenshot%202026-06-04%20101056.png)

### Ingestelde regels (per branch)

| Regel | Omschrijving | Van toepassing op |
|---|---|---|
| Pull Request vereist | Direct pushen naar de branch is geblokkeerd — wijzigingen gaan altijd via een PR | `main`, `develop`, `release/*` |
| Review vereist | Minimaal één goedkeuring van een reviewer is verplicht voordat een PR gemerged mag worden | `main`, `develop`, `release/*` |
| Status checks vereist | De CI-pipeline (build + tests) moet slagen voordat mergen mogelijk is | `main`, `develop`, `release/*` |
| Conversation resolution | Alle opmerkingen in een PR moeten opgelost zijn voor merge | `main`, `develop`, `release/*` |

---

## CI/CD Pipeline

In deze repository is momenteel geen GitHub Actions workflow (bijv. `build-and-sbom.yml`) aanwezig om build/tests/SBOM automatisch af te dwingen.

### Pipeline-stappen

| Stap | Wat het doet | Compliance-doel |
|---|---|---|
| Checkout repository | Haalt de broncode op | Reproduceerbaarheid |
| Set up Java 8 | Installeert de juiste Java-versie | Consistente build-omgeving |
| Cache Maven packages | Hergebruikt dependencies | Snellere en stabielere builds |
| Build project (`mvn clean verify`) | Compileert de code en draait alle tests | Detecteert fouten voor merge |
| Generate CycloneDX SBOM | Maakt een overzicht van alle dependencies met versies | NEN-7510 12.6.1 — kwetsbaarheidsbeheer |
| Upload SBOM artifact | Slaat de SBOM op als downloadbaar artifact | Traceerbaarheid van dependencies |
| Upload test reports | Slaat testrapporten op, ook bij een gefaalde build | Audittrail van testresultaten |

---

## Koppeling aan NEN-7510

| NEN-7510 Control | Pipeline-maatregel | Bewijs |
|---|---|---|
| 12.6.1 — Technische kwetsbaarheden | SBOM-generatie via CI is nog niet ingericht | Nog niet ingericht (workflow ontbreekt) |
| 14.2.1 — Beveiligd ontwikkelen | Build + tests via CI is nog niet ingericht | Nog niet ingericht (workflow ontbreekt) |
| 14.2.2 — Wijzigingsbeheer | Wijzigingen via PR met verplichte review | Branch protection rules (zie screenshot) |
| 15.2.1 — Leveranciersbeheer | Dependency-overzicht via SBOM is nog niet ingericht | Nog niet ingericht (workflow ontbreekt) |

---

## Openstaande gaps

De pipeline detecteert onderstaande bevindingen momenteel **niet automatisch**:

| Bevinding | Ontbrekende controle |
|---|---|
| Hardcoded credentials (`AppointmentActivator.java`) | Secret-scanner zoals `gitleaks` |
| PII in logbestanden (`AppointmentServiceImpl.java`) | Statische analyse zoals SonarQube of SpotBugs |
| Lege `@Authorized`-annotaties | Geen geautomatiseerde check aanwezig |
