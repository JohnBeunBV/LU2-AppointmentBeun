# 11 Risicomatrix CI/CD Proces - OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-36: Risicomatrix & bow-tie CI/CD-proces  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Norm:** NEN-7510:2024

Zie `04-risicocriteria.md` voor de volledige definitie van kans- en impactschalen.

---

## Kleurcodering

| Kleur     | Score   | Beoordeling                                     | Actie                                                          |
| --------- | ------- | ----------------------------------------------- | -------------------------------------------------------------- |
| 🔴 Rood   | 20 – 25 | Kritiek — onacceptabel                          | Onmiddellijke mitigatie, nog in huidige sprint                 |
| 🟠 Oranje | 12 – 19 | Hoog — niet acceptabel als structurele situatie | Security backlog prioriteit Hoog; aanpakken binnen 1 sprint    |
| 🟡 Geel   | 6 – 11  | Midden — acceptabel mits gemonitord             | Security backlog prioriteit Midden; aanpakken binnen 2 sprints |
| 🟢 Groen  | 1 – 5   | Laag — acceptabel restrisico                    | Vastleggen en periodiek herbeoordelen                          |

**Risicoscore = Kans × Impact** (schaal 1–5)

---

## Risicomatrix

<table>
  <thead>
    <tr>
      <th style="text-align:center">Impact ↓ &nbsp;/&nbsp; Kans →</th>
      <th style="text-align:center">1<br><small>Zeldzaam</small></th>
      <th style="text-align:center">2<br><small>Onwaarschijnlijk</small></th>
      <th style="text-align:center">3<br><small>Mogelijk</small></th>
      <th style="text-align:center">4<br><small>Waarschijnlijk</small></th>
      <th style="text-align:center">5<br><small>Vrijwel zeker</small></th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>5 — Catastrofaal</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>5</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>10</strong><br><small>CICD-06</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>15</strong><br><small>CICD-01 CICD-02 CICD-08 CICD-09 CICD-14</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>20</strong></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>25</strong></td>
    </tr>
    <tr>
      <td><strong>4 — Ernstig</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>8</strong><br><small>CICD-10 CICD-13</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>12</strong><br><small>CICD-07</small></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>16</strong><br><small>CICD-05</small></td>
      <td style="background:#F44336;color:white;text-align:center"><strong>20</strong><br><small>CICD-03 CICD-04</small></td>
    </tr>
    <tr>
      <td><strong>3 — Matig</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>3</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>6</strong><br><small>CICD-12</small></td>
      <td style="background:#CDDC39;color:white;text-align:center"><strong>9</strong></td>
      <td style="background:#CDDC39;color:white;text-align:center"><strong>12</strong></td>
      <td style="background:#FF9800;color:white;text-align:center"><strong>15</strong></td>
    </tr>
    <tr>
      <td><strong>2 — Beperkt</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>2</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong><br><small>CICD-11</small></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>6</strong></td>
      <td style="background:#CDDC39;color:white;text-align:center"><strong>8</strong></td>
      <td style="background:#CDDC39;color:white;text-align:center"><strong>10</strong></td>
    </tr>
    <tr>
      <td><strong>1 — Verwaarloosbaar</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>1</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>2</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>3</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>4</strong></td>
      <td style="background:#4CAF50;color:white;text-align:center"><strong>5</strong></td>
    </tr>
  </tbody>
</table>

---

## Risicoregister CI/CD

| ID      | Risico                                                                 | Kans | Impact | Score  | Zone      | NEN-7510 |
| ------- | ---------------------------------------------------------------------- | :--: | :----: | :----: | --------- | -------- |
| CICD-03 | `continue-on-error` SonarQube — SAST-bevindingen blokkeren build nooit |  5   |   4    | **20** | 🔴 Rood   | A.8.25   |
| CICD-04 | Geen geheimscanner — hardcoded credentials ongedetecteerd in pipeline  |  5   |   4    | **20** | 🔴 Rood   | A.8.25   |
| CICD-05 | Geen SCA/CVE-scan op dependencies of container image                   |  4   |   4    | **16** | 🟠 Oranje | A.8.8    |
| CICD-01 | GitHub beheerderaccount gecompromitteerd (geen MFA-afdwinging)         |  3   |   5    | **15** | 🟠 Oranje | A.5.16   |
| CICD-02 | GitHub Secrets gelekt via workflow-logs of gecompromitteerd account    |  3   |   5    | **15** | 🟠 Oranje | A.8.3    |
| CICD-08 | Productie deploy zonder rollback — uitval bij defecte image            |  3   |   5    | **15** | 🟠 Oranje | A.5.29   |
| CICD-09 | Geen verplichte reviewer voor productie-environment                    |  3   |   5    | **15** | 🟠 Oranje | A.8.3    |
| CICD-14 | `main` push triggert productie-deploy zonder extra goedkeuring         |  3   |   5    | **15** | 🟠 Oranje | A.8.3    |
| CICD-07 | SSH private key tijdelijk op runner-schijf (echo naar bestand)         |  3   |   4    | **12** | 🟠 Oranje | A.8.9    |
| CICD-06 | Supply chain aanval via niet-gepinde GitHub Actions (`@v4`, `@v6`)     |  2   |   5    | **10** | 🟡 Geel   | A.8.9    |
| CICD-10 | SSH host key TOFU via `ssh-keyscan` — kwetsbaar voor MITM              |  2   |   4    | **8**  | 🟡 Geel   | A.8.9    |
| CICD-13 | Repository-eigenaar verliest toegang (account verwijderd / verlaten)   |  2   |   4    | **8**  | 🟡 Geel   | A.5.16   |
| CICD-12 | Geen Docker image signing — image-integriteit niet aantoonbaar         |  2   |   3    | **6**  | 🟡 Geel   | A.8.9    |
| CICD-11 | Gedeelde Slack webhook voor alle OTAP-omgevingen                       |  2   |   2    | **4**  | 🟢 Groen  | —        |

---

## Samenvatting prioriteiten

| Prioriteit      | ID's                                                          | Actie                                                                   |
| --------------- | ------------------------------------------------------------- | ----------------------------------------------------------------------- |
| 🔴 Onmiddellijk | CICD-03, CICD-04                                              | Fix in huidige sprint: verwijder `continue-on-error`, voeg gitleaks toe |
| 🟠 Hoog         | CICD-01, CICD-02, CICD-05, CICD-07, CICD-08, CICD-09, CICD-14 | Mitigatieplan, aanpakken binnen 1 sprint                                |
| 🟡 Midden       | CICD-06, CICD-10, CICD-12, CICD-13                            | Backlog, aanpakken binnen 2 sprints                                     |
| 🟢 Laag         | CICD-11                                                       | Vastleggen, periodiek herbeoordelen                                     |
