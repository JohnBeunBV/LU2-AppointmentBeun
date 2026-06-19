# Auditrapport — OpenMRS Appointment Scheduler

**Module:** OpenMRS Appointment Scheduler  
**Versie:** [vul versienummer in]  
**Repository:** [vul repository-URL in]  
**Beoordelingsperiode:** [van … tot …]  
**Auteur(s):** [vul namen in]  
**Datum:** [vul datum in]  
**Norm:** NEN-7510:2024 Deel 2 (primair) · CRA (aanvullend) · AVG (privacy/logging)

> **Opzet:** Dit document volgt de structuur uit WS06 (7 secties). Elke sectie verwijst naar bestaande md-bestanden uit eerdere sprints als bronmateriaal. Vul per sectie de inhoud aan; verwijder deze toelichtende quote-blokken voor oplevering.

---

## 1. Executive Summary

> **Doelgroep:** Raad van Bestuur / besluitvormers, geen technische achtergrond.
> **Regels:** max. 400 woorden, geen jargon (geen CVE-nummers of CVSS-scores in de tekst — die horen in sectie 4), risico's vertaald naar zorg-impact, RAG-status onderbouwd.
> **Schrijf dit als laatste**, ook al staat het vooraan.
> **Bronmateriaal:** samenvatting van alle onderstaande secties; met name de risicomatrix, security backlog en RAR.

### RAG-status: 🔴 / 🟠 / 🟢 [kies en onderbouw in één zin]

[Eénzin-samenvatting van de huidige status]

### Top 3 risico's voor de organisatie

| # | Risico (in zorg-taal, niet technisch) | Ernst | Status |
|---|---|---|---|
| 1 | [bijv. "Patiëntgegevens kunnen onbedoeld in logbestanden terechtkomen"] | 🔴/🟠/🟢 | Open / In behandeling / Opgelost |
| 2 | [...] | | |
| 3 | [...] | | |

### Geprioriteerde roadmap

| Prioriteit | Actie | Termijn |
|---|---|---|
| 🔴 Nu | [...] | Direct |
| 🟠 Deze sprint | [...] | Sprint [x] |
| 🟢 Later | [...] | [periode] |

---

## 2. Scope & Context

> **Bronmateriaal:** projectopzet, module-keuze (sprint 1), security backlog scope-sectie.
> **Vermeld ook wat NIET getest is, en waarom.**

### 2.1 Wat is beoordeeld

- **Module/applicatie:** [naam, versie, repository-URL]
- **Beoordelingsperiode:** [van … tot …]
- **Testomgeving:** [Development / Test / Acceptatie — welke OTAP-omgeving(en)]

### 2.2 Wat is buiten scope

[bijv. productie-infrastructuur, OpenMRS core, andere modules — verwijs naar de "buiten scope"-tabel in `09-security-backlog.md`]

### 2.3 Toepasselijk normenkader

| Norm/wet | Reikwijdte |
|---|---|
| NEN-7510:2024 Deel 2 | Primair kader — informatiebeveiliging in de zorg |
| CRA | Aanvullend — leveranciersverplichtingen voor producten met digitale elementen |
| AVG | Privacy- en logging-gerelateerde controls |

---

## 3. Audit Methodologie

> **Bronmateriaal:** CI/CD pipeline (`pipeline.yml`), security backlog, risicomatrix-methode.
> **Beschrijf HOE getest is** — techniek, tool, wanneer, waar het resultaat staat.

| Techniek | Tool | Wanneer | Resultaat (verwijzing) |
|---|---|---|---|
| SAST | [SonarQube / CodeQL] | Bij elke PR + eindmeting | [bijlage-referentie] |
| SCA | [Dependabot / Snyk] | Wekelijks + eindmeting | [bijlage-referentie] |
| SBOM | [CycloneDX Maven Plugin] | Build-tijd | `docs/sbom.cdx.json` |
| Code review | Handmatig (peer review) | [sprint] | [PR-overzicht] |
| Risicoanalyse | ISO 27005 methode | Sprint 2 | `05-risicomatrix.md` |
| Threat modelling | Microsoft TMT (C4-gebaseerd) | Sprint 3 | `13-threat-model-bijgewerkt.md` |
| Penetration testing | [tool, bijv. OWASP ZAP] | Sprint 3/4 | [pentest-rapport] |

---

## 4. Risico-analyse & Bevindingen

> **Eis:** ten minste 4 bevindingen, elk volgens vast formaat.
> **Bronmateriaal:** `05-risicomatrix.md`, `09-security-backlog.md`, `06-bowtie.md`, `10-risk-assessment-report.md`.
> **Vergeet niet:** contextuele score naast CVSS — impact kan in de zorgcontext hoger/lager liggen dan CVSS aangeeft.

### Bevinding format (kopieer per bevinding)

```
┌─────────────────────────────────────────────────────────────────┐
│ Bevinding ID:        [R01 / B-00x]                                │
│ Titel:                [...]                                       │
│ Datum gevonden:       [...]                                        │
│ Status:               Open / In behandeling / Opgelost             │
├─────────────────────────────────────────────────────────────────┤
│ CVSS-score:           [...] ([Low/Medium/High/Critical])           │
│ CVSS-vector:           [AV:.../AC:.../...]                          │
│ Contextuele score:    [...] — [onderbouwing zorgcontext]            │
├─────────────────────────────────────────────────────────────────┤
│ NEN-7510 Control:     [bijv. 8.15 Logregistratie]                   │
│ Beschrijving:         [wat is het probleem, waar in de code]        │
├─────────────────────────────────────────────────────────────────┤
│ Bewijs:               [bestand + regelnummer / scan-output / PR]    │
├─────────────────────────────────────────────────────────────────┤
│ Aanbeveling:          [concrete maatregel]                          │
└─────────────────────────────────────────────────────────────────┘
```

### Overzichtstabel bevindingen

| ID | Titel | CVSS | Contextuele score | NEN-7510 | Status |
|---|---|---|---|---|---|
| [R01] | [...] | [...] | [...] | [...] | [...] |
| [R02] | [...] | [...] | [...] | [...] | [...] |
| [R03] | [...] | [...] | [...] | [...] | [...] |
| [R04] | [...] | [...] | [...] | [...] | [...] |

[Vul minimaal 4 bevindingen volledig uit conform het format hierboven — gebruik de meest kritieke uit `05-risicomatrix.md`]

---

## 5. SBOM & Supply Chain Security

> **Bronmateriaal:** `docs/sbom.cdx.json`, Snyk/OWASP/Trivy scanresultaten.
> **Eis:** benoem minimaal de top 10 dependencies op risico (hoogste CVSS), plus alle bekende CVE's. Volledige SBOM als bijlage.

| Component | Versie | Licentie | Bekende CVE's | Status |
|---|---|---|---|---|
| [...] | [...] | [...] | [CVE-... (CVSS ...)] | [Gepatcht/Open/OK] |
| [...] | [...] | [...] | [...] | [...] |

**Relevante NEN-7510 controls:**
- 8.8 Beheer van technische kwetsbaarheden
- 5.22 Monitoring leveranciers

---

## 6. Conclusie & Advies

> **Beantwoord de auditvraag:** Voldoet de module aan de relevante NEN-7510:2024-2 controls en CRA-verplichtingen?
> **Bronmateriaal:** samenvatting van sectie 4 en 5.

### Antwoord op de auditvraag

[...]

### Prioritering van aanbevelingen

| Prioriteit | Criterium | Actie |
|---|---|---|
| 🔴 Nu | Kritieke/hoge bevinding, nog open | Direct oplossen vóór productie-deployment |
| 🟠 Deze sprint | Gemiddelde bevinding, plan aanwezig | Inplannen in aankomende sprint |
| 🟢 Later | Lage bevinding of lange-termijn verbetering | Opnemen in security roadmap |

---

## 7. Bijlagen

> **Eis:** elke bijlage heeft een ID en beschrijving, en wordt vanuit de hoofdtekst gerefereerd. Een bijlage zonder referentie heeft geen auditwaarde.

| Bijlage | Inhoud | Bronbestand |
|---|---|---|
| A | Gap-analyse (sprint 1 + herevaluatie sprint 3) | `01-gap-analyse.md`, `12-gap-analyse-logging.md` |
| B | Traceability matrix | zie sectie 9 hieronder |
| C | SBOM (CycloneDX JSON) | `docs/sbom.cdx.json` |
| D | SAST-output | [SonarQube/CodeQL export] |
| E | Risicomatrix | `05-risicomatrix.md` |
| F | Bow-tie diagrammen / threat models | `06-bowtie.md`, `13-threat-model-bijgewerkt.md`, C4-diagrammen |
| G | Snyk-/OWASP-/Trivy-rapport | [scan-output] |
| H | CRA-mapping | zie sectie 8 hieronder |
| I | Security backlog / geprioriteerde verbeteraanpak | `09-security-backlog.md` |
| J | Penetration test rapport + PoC's | [pentest-rapport, sprint 3/4] |
| K | Asset-identificatie | `04-assets.md` |
| L | Risicocriteria | `04-risicocriteria.md` |
| M | Bronnen (CVE-referenties, normreferenties, tools) | [...] |

---

## 8. CRA-mapping (aanvullend op sectie 7H)

> **Bronmateriaal:** zie presentatie-slide "CRA vs NEN-7510".

| CRA-verplichting | NEN-7510:2024-2 control |
|---|---|
| Software leveren zonder bekende (actieve) kwetsbaarheden | 8.8 Beheer van technische kwetsbaarheden |
| SBOM beschikbaar stellen aan gebruikers | 8.8 + 5.22 Monitoring leveranciers |
| Beveiligingsupdates leveren gedurende de levensduur | 8.8 Patch management |
| Secure by design | 8.25 Beveiligen tijdens de ontwikkelcyclus |
| Actief misbruikte kwetsbaarheden melden aan ENISA | 6.8 Rapportage van informatiebeveiligingsgebeurtenissen |
| Logging en monitoring ondersteunen | 8.15 Logregistratie + 8.16 Monitoringactiviteiten |
| Toegangscontrole voor beheerinterfaces | 8.2 Beheer van bevoorrechte toegangsrechten |

---

## 9. Traceability Matrix

> **Eis:** minimaal 5 NEN-7510:2024-2 controls. Elk "Na"-bewijs moet een verifieerbaar artefact zijn: commit hash, PR-nummer, scan-output met datum, testresultaat, screenshot.
> **Valkuilen om te vermijden:** te vage normverwijzing ("beveiliging" i.p.v. "8.28 Veilig coderen"), geen datum/tool-referentie bij "Vóór", ontbrekend PR-nummer bij "Aanpassing", ongefundeerde claim bij "Na" ("we geloven dat het werkt").

| Norm | Maatregel | Vóór (bevinding) | Aanpassing | Na (bewijs) |
|---|---|---|---|---|
| NEN-7510 8.28 Veilig coderen | [...] | [bevinding + bronverwijzing + datum] | [PR-nummer + beschrijving] | [herhaalde scan + testresultaat] |
| NEN-7510 8.8 Kwetsbaarheidsbeheer | [...] | [...] | [...] | [...] |
| NEN-7510 8.15 Logregistratie | [...] | [...] | [...] | [...] |
| NEN-7510 8.25 Beveiliging ontwikkelcyclus | [...] | [...] | [...] | [...] |
| NEN-7510 5.35 Onafhankelijke beoordeling | Dit auditrapport zelf | Geen formele beoordeling vóór dit rapport | Auditrapport opgesteld conform NEN-7510:2024-2 scope | Dit document, inclusief alle bijlagen |

---

## 10. Responsible Disclosure (indien van toepassing)

> **Alleen invullen als tijdens de audit een kritieke kwetsbaarheid is gevonden die buiten het eigen team om gemeld moet worden** (bijv. een kwetsbaarheid in OpenMRS core zelf, niet in de eigen module).

| Stap | Status | Datum |
|---|---|---|
| 1. Kwetsbaarheid gedocumenteerd (CVSS, reproduceerbare beschrijving) | [...] | [...] |
| 2. Contact opgenomen met vendor/CERT | [...] | [...] |
| 3. Hersteltermijn afgesproken | [...] | [...] |
| 4. Coördinatie via NCSC/CERT-CC (indien nodig) | [...] | [...] |
| 5. Publicatie na patch of na termijn | [...] | [...] |

**Relevante normen:** NEN-7510 6.8 (rapportage van informatiebeveiligingsgebeurtenissen), CRA Art. 14 (meldplicht ENISA binnen 24 uur bij actief misbruik).

---

## 11. Verantwoording AI-tooling

> **Verplicht onderdeel.** Doel: aantonen dat *jij* de security-beslissingen hebt genomen, niet de AI.

```
┌─────────────────────────────────────────────────────────────────┐
│ AI-TOOLING VERANTWOORDING                                         │
│                                                                     │
│ Gebruikte tools: [...]                                             │
│                                                                     │
│ Wat ik aan AI heb gevraagd:                                        │
│ • [...]                                                            │
│                                                                     │
│ Wat de AI heeft gegenereerd:                                       │
│ • [...]                                                            │
│                                                                     │
│ Wat ik zelf heb gecontroleerd:                                     │
│ • [...]                                                            │
│                                                                     │
│ Beslissingen die ik zelf heb gemaakt:                              │
│ • [...]                                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Checklist vóór oplevering

- [ ] Executive summary ≤ 400 woorden, geen jargon, RAG-status onderbouwd
- [ ] Minimaal 4 volledig uitgewerkte bevindingen in sectie 4
- [ ] Traceability matrix met minimaal 5 NEN-7510 controls, elk met verifieerbaar bewijs
- [ ] Alle bijlagen gerefereerd vanuit de hoofdtekst
- [ ] SBOM-sectie met top 10 dependencies op risico
- [ ] CRA-mapping aanwezig
- [ ] AI-tooling verantwoording ingevuld
- [ ] Geen bevindingen verzwegen — ook open/onopgeloste bevindingen zijn opgenomen
- [ ] Document opgeslagen als artefact in de repository (PDF of Word)
