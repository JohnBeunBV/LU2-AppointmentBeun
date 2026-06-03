# 02 — Pipeline Compliance

> Kwaliteitseisen en bevindingen staan in [00-Security-&-Maintainability.md](00-Security-%26-Maintainability.md).
> NEN-7510 gap-analyse staat in [01-gap-analyse.md](01-gap-analyse.md).

## NEN-7510 Controls → Pipeline-maatregel → Bewijs

| NEN-7510 Control | Omschrijving | Pipeline-maatregel | Bewijs |
|---|---|---|---|
| A.8.3 / 9.4.1 | Beperking van toegang tot informatie | `@Authorized`-annotaties worden meegeleverd in de broncode en gecontroleerd bij elke build | `AppointmentService.java` — privilege-eisen per methode |
| A.8.5 / 9.4.3 | Beheer van wachtwoorden en credentials | Build faalt niet op hardcoded credentials — **bevinding S1 is open** | `AppointmentActivator.java` regels 79–82 — niet conform |
| A.8.15 / 12.4.1 | Gebeurtenissen registreren (logging) | Build faalt niet op PII-logging — **bevinding S2 is open** | `AppointmentServiceImpl.java` regels 1426–1432 — niet conform |
| 12.6.1 | Beheer van technische kwetsbaarheden | SBOM gegenereerd via CycloneDX Maven-plugin; alle dependencies zichtbaar met versie | `sbom-cyclonedx` artifact in workflow-run (`target/bom.json`) |
| 14.2.1 | Beleid voor beveiligd ontwikkelen | Build en tests draaien automatisch bij elke PR via `mvn clean verify` | `build-and-sbom.yml` stap *Build project* |
| 14.2.2 | Procedures voor wijzigingsbeheer | Wijzigingen gaan via een Pull Request naar `main`; workflow triggert op PR | GitHub branch `main` + PR-trigger in `build-and-sbom.yml` |
| 15.2.1 | Beheer van dienstverlening door leveranciers | Alle externe Maven-dependencies traceerbaar via de SBOM | `sbom-cyclonedx` artifact — volledige dependency-lijst met versies en licenties |

---

## Openstaande pipeline-gaps

De huidige pipeline detecteert onderstaande bevindingen **niet automatisch**. Er is geen build-stap die hierop controleert:

| Bevinding | Eis | Ontbrekende pipeline-controle |
|---|---|---|
| Hardcoded credentials in `AppointmentActivator.java` | S1 / NEN-7510 9.4.3 | Secret-scanner (bijv. `truffleHog` of `gitleaks`) |
| PII-logging in `AppointmentServiceImpl.java` | S2 / NEN-7510 A.8.15 | Statische code-analyse (bijv. SpotBugs, SonarQube) |
| Lege `@Authorized`-annotaties | S3 / NEN-7510 9.4.1 | Geen automatische check aanwezig |
