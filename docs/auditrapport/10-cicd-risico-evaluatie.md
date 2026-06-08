# 10 — Risico-evaluatie CI/CD Proces

**Sprint:** 2 | **Taak:** SOF-36: CI/CD risico-evaluatie | **Datum:** Juni 2026 | **Norm:** NEN-7510:2024

---

## 1. Scope en aanpak

Dit document evalueert de beveiligingsrisico's van de GitHub Actions CI/CD-pipeline (`.github/workflows/pipeline.yml`) voor de OpenMRS Appointment Scheduler in een OTAP-omgeving. De evaluatie is gebaseerd op directe analyse van de workflow-definitie en de GitHub-projectinrichting (branch protection, environments).

De OTAP-omgevingen en hun deploymentpad:

| Omgeving | Branch | Trigger | Secrets gebruikt |
|----------|--------|---------|-----------------|
| Test | `develop` | Push | `TEST_SSH_KEY`, `TEST_SERVER_HOST`, `TEST_SERVER_USER` |
| Acceptatie | `release/*` | Push | `ACC_SSH_KEY`, `ACC_SERVER_HOST`, `ACC_SERVER_USER` |
| Productie | `main` / handmatig | `workflow_dispatch` | `PROD_SSH_KEY`, `PROD_SERVER_HOST`, `PROD_SERVER_USER` |

---

## 2. Bevindingen

### CICD-01 — GitHub beheerderaccount gecompromitteerd

**Wat kan er misgaan:** Een aanvaller die toegang krijgt tot het account van een repository-eigenaar of beheerder kan:
- Branch protection rules uitschakelen
- Rechtstreeks naar `main` pushen en een productie-deploy triggeren
- Alle GitHub Secrets inzien en exporteren via een kwaadaardige workflow
- De pipeline-definitie aanpassen om malware in te bouwen

De pipeline heeft geen MFA-verplichting op organisatieniveau en geen IP-allowlist. De `workflow_dispatch`-trigger voor productie vereist geen tweede goedkeurder.

**Scenario:** Een beheerder ontvangt een phishing-e-mail, voert zijn GitHub-wachtwoord in op een nagemaakte pagina. Aanvaller logt in, voegt een stap toe aan `pipeline.yml` die `PROD_SSH_KEY` naar een externe server stuurt, en triggert `workflow_dispatch`. De productieserver is volledig gecompromitteerd.

**Aanbevolen maatregelen:**
- MFA verplicht stellen op GitHub-organisatieniveau
- Environment protection rules instellen voor `production` met minimaal 1 verplichte reviewer
- Gebruik `CODEOWNERS` om wijzigingen aan `pipeline.yml` altijd door een tweede persoon te laten reviewen

---

### CICD-02 — GitHub Secrets gelekt via workflow-logs of gecompromitteerd account

**Wat kan er misgaan:** GitHub Secrets worden automatisch gemaskeerd in logs, maar dit is niet onfeilbaar. Secrets kunnen uitlekken via:
- Een stap die secrets naar stdout schrijft (bijv. `echo ${{ secrets.PROD_SSH_KEY }}`)
- Een pull request van een externe contributor die een kwaadaardige workflow toevoegt
- Een gecompromitteerd account dat de secret via een workflow exfiltreert

De pipeline schrijft SSH private keys tijdelijk naar schijf (`echo "${{ secrets.PROD_SSH_KEY }}" > ~/.ssh/deploy_key`). Hoewel de key daarna wordt verwijderd, bestaat er een venster waarin de key op de runner aanwezig is. De runner is een gedeelde GitHub-hosted machine.

**Aanbevolen maatregelen:**
- Gebruik `OIDC` (OpenID Connect) voor cloud-deployments in plaats van SSH keys in secrets
- Stel `pull_request` triggers in op `fork`-beperking: voorkom dat PR's van forks secrets kunnen lezen
- Roteer secrets periodiek; documenteer een rotatieschema
- Gebruik `ssh-agent` met `webfactory/ssh-agent` action in plaats van keys naar schijf te schrijven

---

### CICD-03 — SonarQube `continue-on-error: true` — SAST-bevindingen blokkeren build niet

**Wat kan er misgaan:** De SonarQube-analysestap heeft `continue-on-error: true`. Dit betekent dat de pipeline altijd verder gaat, ook als:
- SonarQube kritieke kwetsbaarheden rapporteert (bijv. de PII-logging of hardcoded credentials)
- De SonarQube-server niet bereikbaar is (fout wordt genegeerd)
- Het quality gate faalt

In de praktijk is de SAST-controle hierdoor volledig decoratief: het resultaat heeft geen invloed op de deployability van de code.

**Aanbevolen maatregelen:**
- Verwijder `continue-on-error: true` op de SonarQube-stap op `main` en `release/*` branches
- Stel een quality gate in die minimaal blokkeert op: severity BLOCKER of CRITICAL
- Definieer een apart gedrag voor `develop` (waarschuwing) vs `release/*` en `main` (harde blokkering)

---

### CICD-04 — Geen geheimscanner in pipeline

**Wat kan er misgaan:** Er is geen geautomatiseerde scan op hardcoded secrets (wachtwoorden, API-keys, verbindingsstrings) in de broncode. De bestaande vulnerability R02 (hardcoded credentials in `AppointmentActivator.java`) wordt niet automatisch gedetecteerd door de pipeline en kan ongemerkt in productie komen.

De SBOM wordt gegenereerd maar bevat geen scan op secrets — CycloneDX rapporteert dependencies, niet broncodepatronen.

**Aanbevolen maatregelen:**
- Voeg `gitleaks/gitleaks-action` toe als eerste stap in de pipeline
- Configureer `.gitleaks.toml` voor false-positive onderdrukking (zie §5)
- Blokkeer de pipeline bij detectie van een secret (geen `continue-on-error`)

---

### CICD-05 — Geen SCA / CVE-scan op dependencies en container image

**Wat kan er misgaan:** De SBOM wordt gegenereerd maar er wordt geen vulnerability-scan op uitgevoerd. Bekende CVE's in Maven-dependencies of in de Docker base image (`azul/zulu-openjdk-debian:7`) worden niet gedetecteerd en kunnen ongemerkt in productie komen.

Voorbeeld: Log4Shell (CVE-2021-44228) zou via de `SC-06` asset (Log4j/SLF4J) worden meegeleverd zonder alarm.

**Aanbevolen maatregelen:**
- Voeg Trivy toe voor scanning van de container image (`trivy image`)
- Voeg Trivy of OWASP Dependency Check toe voor scanning van Maven-dependencies (`trivy fs` of `mvn dependency-check:check`)
- Definieer een policy: blokkeer bij CRITICAL of HIGH severity CVE's zonder bekende fix

---

### CICD-06 — Supply chain aanval via niet-gepinde GitHub Actions

**Wat kan er misgaan:** De pipeline gebruikt Actions via floating tags (`@v4`, `@v6`). Als een Action-repository wordt gecompromitteerd en de tag wordt verlegd naar een kwaadaardige commit, voert de pipeline automatisch de kwaadaardige code uit bij de eerstvolgende run.

Betroffen Actions:
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `actions/upload-artifact@v4`
- `docker/login-action@v3`
- `docker/build-push-action@v6`
- `slackapi/slack-github-action@v2`

**Aanbevolen maatregelen:**
- Pin alle externe Actions op een volledige SHA-hash: `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683` (= v4)
- Voeg `step-security/harden-runner` toe als eerste stap voor runtime-monitoring van Actions
- Review periodiek of gepinde SHA's nog actueel zijn

---

### CICD-07 — SSH private key tijdelijk op runner (echo naar bestand)

**Wat kan er misgaan:** De deploy-stappen schrijven SSH private keys naar schijf:
```yaml
echo "${{ secrets.PROD_SSH_KEY }}" > ~/.ssh/deploy_key
```
Hoewel de key aan het einde wordt verwijderd (`rm ~/.ssh/deploy_key`), bestaat een tijdvenster waarin de key op de runner-schijf staat. Bij een crashdump, een debugging-stap of een kwaadaardige Action in dezelfde job kan de key worden uitgelezen.

**Aanbevolen maatregelen:**
- Gebruik `webfactory/ssh-agent` action: deze laadt de key in de SSH-agent in geheugen, zonder schrijven naar schijf
- Overweeg overgang naar OIDC-gebaseerde authenticatie (voor cloud-providers)

---

### CICD-08 — Productie deploy zonder geformaliseerde rollback

**Wat kan er misgaan:** De productie-deploystap voert `docker-compose down` uit voordat de nieuwe image beschikbaar is:
```yaml
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```
Als de nieuwe image defect is of de `up -d` mislukt, staat de productieomgeving down totdat handmatig herstel plaatsvindt. Er is geen blue-green of canary strategie aanwezig.

De databasebackup wordt wel gemaakt, maar er is geen geautomatiseerde rollback naar de vorige image.

**Aanbevolen maatregelen:**
- Sla het vorige image-tag op vóór deploy en voer een rollback uit bij falen van de health check
- Overweeg blue-green deployment: start de nieuwe container naast de oude en switch pas na succesvolle health check

---

### CICD-09 — Geen verplichte reviewer voor productie-environment

**Wat kan er misgaan:** De `workflow_dispatch`-trigger voor de `deploy-prod` job vereist geen tweede goedkeurder. Elke medewerker met `write`-rechten op de repository kan een productiedeploy starten zonder toestemming van een tweede persoon.

**Aanbevolen maatregelen:**
- Stel in GitHub de `production` environment in met **Required reviewers** (minimaal 1 extra persoon)
- Combineer met een wachttijd (`wait-timer`) van minimaal 5 minuten om impulsieve deploys te voorkomen

---

### CICD-10 — SSH host key TOFU via `ssh-keyscan`

**Wat kan er misgaan:** De pipeline voert `ssh-keyscan -H <host>` uit om de host key te accepteren ("Trust On First Use"). Op het moment van de scan kan een man-in-the-middle aanval de kwaadaardige host key in `known_hosts` plaatsen, waarna alle SSH-commando's naar de aanvaller worden gerouteerd.

**Aanbevolen maatregelen:**
- Sla de bekende host keys vast als GitHub Secret (`TEST_SSH_KNOWN_HOSTS` etc.) en schrijf deze naar `known_hosts` in plaats van `ssh-keyscan` te gebruiken
- Verifieer handmatig de fingerprint bij eerste opzet

---

### CICD-11 — Gedeelde Slack webhook voor alle omgevingen

**Wat kan er misgaan:** Alle omgevingen (test, acceptatie, productie) gebruiken dezelfde `SLACK_WEBHOOK`. Een aanvaller die de webhook URL weet (bijv. via een gelekte log) kan valse deployment-meldingen sturen en operators misleiden.

**Aanbevolen maatregelen:** Gebruik aparte webhooks per omgeving; dit verbetert ook de notificatie-granulariteit.

---

## 3. Overzicht van bevindingen

| ID | Bevinding | Kans | Impact | Score | Zone |
|----|-----------|:----:|:------:|:-----:|------|
| CICD-03 | `continue-on-error` SonarQube — SAST blokkeert niet | 5 | 4 | **20** | 🔴 Rood |
| CICD-04 | Geen geheimscanner (gitleaks) | 5 | 4 | **20** | 🔴 Rood |
| CICD-01 | GitHub beheerderaccount gecompromitteerd | 3 | 5 | **15** | 🟠 Oranje |
| CICD-02 | GitHub Secrets gelekt | 3 | 5 | **15** | 🟠 Oranje |
| CICD-05 | Geen SCA / CVE-scan | 4 | 4 | **16** | 🟠 Oranje |
| CICD-08 | Geen rollback procedure productie | 3 | 5 | **15** | 🟠 Oranje |
| CICD-09 | Geen verplichte reviewer productie | 3 | 5 | **15** | 🟠 Oranje |
| CICD-14 | `main` push triggert productie-deploy | 3 | 5 | **15** | 🟠 Oranje |
| CICD-07 | SSH key tijdelijk op schijf | 3 | 4 | **12** | 🟠 Oranje |
| CICD-06 | Unpinned GitHub Actions (supply chain) | 2 | 5 | **10** | 🟡 Geel |
| CICD-10 | SSH host key TOFU (ssh-keyscan) | 2 | 4 | **8** | 🟡 Geel |
| CICD-13 | Verlies toegang repository-eigenaar | 2 | 4 | **8** | 🟡 Geel |
| CICD-12 | Geen Docker image signing | 2 | 3 | **6** | 🟡 Geel |
| CICD-11 | Gedeelde Slack webhook alle omgevingen | 2 | 2 | **4** | 🟢 Groen |

---

## 4. Koppeling aan NEN-7510:2024

| NEN-7510 Control | Omschrijving | Betrokken bevindingen |
|---|---|---|
| A.8.8 — Technische kwetsbaarheden | Beheer van bekende CVE's in dependencies | CICD-04, CICD-05 |
| A.8.9 — Configuratiebeheer | Veilige inrichting van CI/CD omgeving | CICD-03, CICD-06, CICD-07 |
| A.8.25 — Beveiligd ontwikkelen | SAST, secrets scanning in pipeline | CICD-03, CICD-04 |
| A.5.16 — Identiteitsbeheer | MFA op GitHub accounts | CICD-01 |
| A.8.3 — Toegangsbeheeer | Verplichte reviewer productie | CICD-01, CICD-09 |
| A.5.29 — Informatiebeveiliging tijdens verstoring | Rollback na mislukte deploy | CICD-08 |

---

## 5. Aanpak false positives

Bij het inschakelen van geautomatiseerde scanners (gitleaks, Trivy, SonarQube) zullen false positives optreden. Onderstaand beleid is van toepassing.

### 5.1 Definitie

Een **false positive** is een bevinding van een scanner die na handmatige beoordeling geen daadwerkelijk risico vormt. Voorbeelden:
- Gitleaks meldt een testsleutel in een unit-testbestand
- Trivy meldt een CVE in een library die uitsluitend in de testomgeving wordt gebruikt
- SonarQube meldt een "SQL injection" op een methode die Hibernate-parameterisatie gebruikt

### 5.2 Proces

```
1. Scanner rapporteert bevinding
        ↓
2. Teamlid beoordeelt handmatig (< 24 uur)
        ↓
3a. Echte kwetsbaarheid → opnemen in security backlog (09-security-backlog.md)
        ↓
3b. False positive → onderbouwde suppression toevoegen met:
        - Bestandspad of regel
        - Reden voor suppression
        - Naam reviewer en datum
        ↓
4. Suppression gecommit via PR met verplichte review
        ↓
5. Herbeoordelen bij volgende sprint of bij nieuwe versie van de library
```

### 5.3 Suppression per tool

| Tool | Suppression-methode | Locatie |
|------|--------------------|---------| 
| Gitleaks | `[[allowlist]]` in `.gitleaks.toml` | Repository-root |
| Trivy | `.trivyignore` (CVE-ID per regel) | Repository-root |
| SonarQube | `// NOSONAR` inline of "Won't Fix" markering in UI | Broncode / SonarQube project |
| OWASP Dep. Check | `<suppress>` in `suppressions.xml` | `owasp/` map |

### 5.4 Regels voor suppression

- Een suppression **mag nooit** worden toegevoegd zonder schriftelijke onderbouwing
- Suppressions voor CRITICAL of HIGH severity vereisen **goedkeuring van twee teamleden**
- Suppressions worden elk kwartaal herbeoordeeld
- Suppressions worden gelogd in `docs/auditrapport/09-security-backlog.md` onder een aparte kolom "Geaccepteerd als false positive"
