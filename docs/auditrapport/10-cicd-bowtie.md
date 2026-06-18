# 10 CI/CD Bow-tie Analyse: Gecompromitteerde CI/CD Pipeline - OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-36: Risicomatrix & bow-tie CI/CD-proces  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Norm:** NEN-7510:2024

---

## 1. Top Event

> **Kwaadaardige of kwetsbare code bereikt de productieomgeving**

Dit is het centrale ongewenste event in de bow-tie. Het omvat elk scenario waarbij code of configuratie die de productieomgeving schaadt — via een aanvaller, een ontwikkelaarsfout of een falende beveiligingscontrole — daadwerkelijk in productie terechtkomt.

Dit top event is gekozen omdat:

- Het de combinatie is van de twee hoogst-scorende CI/CD-risico's (CICD-03 + CICD-04, score 20)
- Het de expliciete scenario's omvat die de opdrachtgever noemde: gelekte secrets en gecompromitteerde accounts
- De gevolgen direct raken aan patiëntveiligheid (DA-01, DA-02) en AVG-compliance

---

## 2. Bow-tie Diagram

```
                          PREVENTIEVE BARRIÈRES                    TOP EVENT                  HERSTELBARRIÈRES
                                (Links)                              (Midden)                      (Rechts)

DREIGINGEN                                                                                                          GEVOLGEN
──────────                                                                                                          ────────

                        ┌─────────────────────────────┐
                        │ B1: MFA afdwingen op         │
T1: GitHub beheerder  ──┤    GitHub organisatieniveau  ├──┐
    account gehackt     └─────────────────────────────┘  │
    (phishing/          ┌─────────────────────────────┐  │
     credential theft)  │ B2: Environment protection   │  │
                      ──┤    rules + verplichte        ├──┤
                        │    reviewer productie         │  │
                        └─────────────────────────────┘  │
                                                          │
                        ┌─────────────────────────────┐  │          ┌──────────────────────────────┐
                        │ B3: CODEOWNERS op            │  │          │ H1: Health check blokkeert   │
T2: Kwaadaardige PR   ──┤    pipeline.yml              ├──┤          │    deploy bij falen          ├──┐
    van externe         └─────────────────────────────┘  │          └──────────────────────────────┘  │
    contributor         ┌─────────────────────────────┐  │                                             │
                        │ B4: Fork-isolatie: PR's van  │  │          ┌──────────────────────────────┐  │
                      ──┤    forks krijgen geen        ├──┤          │ H2: Databasebackup aanwezig  │  │
                        │    toegang tot secrets        │  │          │    voor productie-deploy     ├──┤
                        └─────────────────────────────┘  │          └──────────────────────────────┘  │
                                                          │                                             │
                        ┌─────────────────────────────┐  │    ╔══════════════════════════════╗        │
                        │ B5: Gitleaks — detecteert    │  │    ║                              ║        │
T3: Hardcoded secret  ──┤    secrets in broncode       ├──┼───▶║  KWAADAARDIGE / KWETSBARE   ║        │
    in code (bijv.      └─────────────────────────────┘  │    ║  CODE BEREIKT PRODUCTIE     ║        │
    PROD_SSH_KEY                                          │    ║                              ║        │
    hardcoded)          ┌─────────────────────────────┐  │    ╚══════════════════════════════╝        │
                        │ B6: SonarQube quality gate   │  │                                             │
                      ──┤    blokkeert bij CRITICAL    ├──┤          ┌──────────────────────────────┐  │
                        │    (geen continue-on-error)   │  │          │ H3: Geautomatiseerde         │  │
                        └─────────────────────────────┘  │          │    rollback naar vorige image ├──┤
                                                          │          └──────────────────────────────┘  │
                        ┌─────────────────────────────┐  │                                             │
                        │ B7: Trivy / SCA-scan —       │  │          ┌──────────────────────────────┐  │
T4: CVE in dependency ──┤    blokkeert bij CRITICAL    ├──┤          │ H4: Slack-alert naar team    │  │   G1: Productieserver
    of base image       │    CVE zonder fix            │  │          │    bij deployment failure    ├──┤──▶  gecompromitteerd
                        └─────────────────────────────┘  │          └──────────────────────────────┘  │
                                                          │                                             │
                        ┌─────────────────────────────┐  │          ┌──────────────────────────────┐  │   G2: Patiëntdata
                        │ B8: Actions gepind op SHA —  │  │          │ H5: Incident response plan   │  │──▶  gelekt / AVG-
T5: Supply chain        │    supply chain beperkt      ├──┘          │    (handmatige escalatie)    ├──┘       overtreding
    aanval via          └─────────────────────────────┘             └──────────────────────────────┘
    kwaadaardige                                                                                            G3: Service
    GitHub Action                                                                                      ──▶  onbeschikbaar

                                                                                                       G4: Reputatie-
                                                                                                      ──▶  schade /
                                                                                                            boetes AP
```

---

## 3. Dreigingen (linkerzijde)

| ID  | Dreiging                                                                     | Huidige status                       | CICD-risico |
| --- | ---------------------------------------------------------------------------- | ------------------------------------ | ----------- |
| T1  | GitHub beheerderaccount gehackt (phishing, credential theft, sessiediefstal) | ⚠️ Geen MFA-afdwinging op org-niveau | CICD-01     |
| T2  | Kwaadaardige pull request van externe contributor met aangepaste pipeline    | ⚠️ Fork-isolatie niet geconfigureerd | CICD-02     |
| T3  | Hardcoded secret in broncode (SSH key, wachtwoord, API token)                | ❌ Geen geheimscanner aanwezig       | CICD-04     |
| T4  | Bekende CVE in Maven-dependency of Docker base image                         | ❌ Geen SCA/CVE-scan in pipeline     | CICD-05     |
| T5  | Supply chain aanval via gecompromitteerde GitHub Action (tag verlegd)        | ⚠️ Actions niet gepind op SHA        | CICD-06     |

---

## 4. Preventieve barrières (linkerzijde)

| ID  | Barrière                                                      | Status                                | Implementatie                                                          |
| --- | ------------------------------------------------------------- | ------------------------------------- | ---------------------------------------------------------------------- |
| B1  | MFA afdwingen op GitHub organisatieniveau                     | ❌ Niet ingesteld                     | GitHub Org → Settings → Authentication → Require 2FA                   |
| B2  | Environment protection: verplichte reviewer voor `production` | ❌ Niet ingesteld                     | GitHub → Environments → production → Required reviewers                |
| B3  | `CODEOWNERS` voor `.github/workflows/`                        | ❌ Ontbreekt                          | `.github/CODEOWNERS`: `/.github/workflows/ @team-security`             |
| B4  | PR's van forks mogen geen secrets lezen                       | ⚠️ Standaard deels geblokkeerd        | GitHub Org → Actions → Fork pull request workflows: "Require approval" |
| B5  | Gitleaks — secrets scanner in CI-pipeline                     | ❌ Ontbreekt                          | `gitleaks/gitleaks-action@v2` als eerste job-stap                      |
| B6  | SonarQube quality gate zonder `continue-on-error`             | ❌ `continue-on-error: true` aanwezig | Verwijder flag; stel quality gate in op BLOCKER/CRITICAL               |
| B7  | Trivy CVE-scan op dependencies en container image             | ❌ Ontbreekt                          | `aquasecurity/trivy-action` in pipeline                                |
| B8  | GitHub Actions gepind op volledige SHA-hash                   | ❌ Floating tags `@v4`, `@v6`         | Vervang door `@<sha>` voor alle externe actions                        |

---

## 5. Herstelbarrières (rechterzijde)

| ID  | Barrière                                                             | Status                 | Implementatie                                                              |
| --- | -------------------------------------------------------------------- | ---------------------- | -------------------------------------------------------------------------- |
| H1  | Health check blokkeert deploy bij falen                              | ✅ Aanwezig            | `pipeline.yml` regels 154–165 (test), 214–225 (acc), 302–313 (prod)        |
| H2  | Databasebackup vóór productie-deploy                                 | ✅ Aanwezig            | `pipeline.yml` regels 267–281 (Database Backup stap)                       |
| H3  | Geautomatiseerde rollback naar vorige image bij health check failure | ❌ Ontbreekt           | Na `exit 1` in health check: `docker-compose pull <previous-tag> && up -d` |
| H4  | Slack-alert bij deployment failure                                   | ✅ Aanwezig            | `pipeline.yml` Slack failure notification stappen                          |
| H5  | Incident response plan voor productiecompromis                       | ❌ Niet gedocumenteerd | Opstellen: escalatiepad, contactpersonen, isolatieprocedure                |

---

## 6. Gevolgen (rechterzijde)

| ID  | Gevolg                                                           | Ernst        | Betrokken asset |
| --- | ---------------------------------------------------------------- | ------------ | --------------- |
| G1  | Productieserver gecompromitteerd — aanvaller heeft shell-toegang | Catastrofaal | DA-06, SC-03    |
| G2  | Patiëntdata gelekt — AVG art. 9 overtreding, meldplicht AP       | Catastrofaal | DA-01, DA-02    |
| G3  | Service onbeschikbaar — zorgverlening verstoord                  | Ernstig      | DA-02, PA-01    |
| G4  | Reputatieschade en boetes Autoriteit Persoonsgegevens (tot €20M) | Catastrofaal | Organisatie     |

---

## 7. Samenvatting: openstaande barrières

Van de 13 geïdentificeerde barrières zijn er:

| Status       | Aantal | Barrières                  |
| ------------ | ------ | -------------------------- |
| ✅ Aanwezig  | 3      | H1, H2, H4                 |
| ❌ Ontbreekt | 7      | B1, B2, B3, B5, B6, B7, H3 |
| ⚠️ Deels     | 3      | B4, B8, H5                 |

**Conclusie:** De preventieve kant van de bow-tie is onvoldoende ingevuld. Van de 8 preventieve barrières zijn er slechts 0 volledig actief. De meeste dreigingen (T1–T5) hebben een directe weg naar het top event zonder blokkade.

**Hoogste prioriteit om te implementeren:**

1. **B5 (gitleaks)** + **B6 (SonarQube quality gate)** — direct in te stellen in de pipeline, hoge impact
2. **B1 (MFA)** + **B2 (environment protection)** — GitHub-instellingen, geen codewijziging nodig
3. **H3 (automatische rollback)** — vermindert de schade als het top event toch plaatsvindt

---

## 8. Referenties

- `10-cicd-risico-evaluatie.md` — uitgebreide beschrijving van alle CI/CD-bevindingen
- `11-cicd-risicomatrix.md` — risicoscores en prioritering
- `05-risicomatrix.md` — applicatierisico's (aanvullend op CI/CD-risico's)
- `04-risicocriteria.md` — scoremethodiek
