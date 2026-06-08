# 06 Bow-tie analyse - OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-28: Bow-tie analyse  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1
**Norm:** NEN-7510:2024
**Gebaseerd op:** Risicomatrix `05-risicomatrix.md`, risico R01 (score 25)

---

## 1. Inleiding

Een bow-tie analyse brengt de oorzaken en gevolgen van een specifiek risico gestructureerd in kaart. De analyse bestaat uit:

- **Hazard** — de onderliggende gevaarlijke situatie
- **Top-event** — het moment waarop controle verloren gaat
- **Preventieve barrières** — maatregelen die het top-event voorkomen (linkerzijde)
- **Correctieve barrières** — maatregelen die de gevolgen beperken nadat het top-event heeft plaatsgevonden (rechterzijde)

De analyse is gekoppeld aan de **audit mindset**: _niet gelogd == niet gebeurd_. Dit principe betekent dat een systeem zonder adequate, integere auditlogging feitelijk blind is voor misbruik - en dat onvoldoende of verkeerde logging zelf een risico vormt.

---

## 2. Hazard

> **Patiëntgegevens (PII/bijzondere persoonsgegevens) worden verwerkt in een systeem zonder adequate toegangscontrole op logbestanden en zonder volledig auditspoor van mutaties.**

De Appointment Scheduler verwerkt naam, geboortedatum, patiënt-ID en medische afspraakdata. Deze data belandt aantoonbaar in logbestanden (`AppointmentServiceImpl.java`, regels 1426–1432) die minder streng beveiligd zijn dan de primaire patiëntdossiers.

---

## 3. Top-event

> **Onbevoegde toegang tot of onbedoelde openbaarmaking van patiëntgegevens via logbestanden of door ontbrekende autorisatiechecks.**

Dit top-event treedt op wanneer een van de oorzaken niet geblokkeerd wordt door een preventieve barrière. Het is het kantelpunt tussen "gevaar aanwezig" en "schade daadwerkelijk opgetreden".

---

## 4. Bow-tie diagram

```
                          PREVENTIEVE BARRIÈRES

  [Oorzaak 1]             [Barrière P1]
  PII in logstatements ──► Geen PII in logs          ──┐
                                                        │
  [Oorzaak 2]             [Barrière P2]                │
  Geen data-level ACL  ──► Row-level autorisatie    ──┤
                                                        │        ┌── [Gevolg 1]
  [Oorzaak 3]             [Barrière P3]                │        │   AVG-datalek
  Lege @Authorized()   ──► Correcte privilege-checks──┤         │
                                                        ▼        │
  [Oorzaak 4]             [Barrière P4]           [TOP-EVENT]───┤── [Gevolg 2]
  Geen audit logging   ──► Volledige auditlogging  ──┤ Onbevoegde│   Juridische
                                                        │ toegang / │   aansprakelijk-
  [Oorzaak 5]             [Barrière P5]                │ openbaring │   heid
  Geen brute-force     ──► Rate limiting +          ──┘ PII        │
  beveiliging              account lockout                          │── [Gevolg 3]
                                                                    │   Reputatie-
                          CORRECTIEVE BARRIÈRES                     │   schade
                                                                    │
                          [Barrière C1]                             │── [Gevolg 4]
                          Incident response plan   ◄────────────────┤   AP-melding
                                                                    │   vereist
                          [Barrière C2]                             │
                          Detectie via SIEM/alerts ◄────────────────┤── [Gevolg 5]
                                                                    │   Verlies
                          [Barrière C3]                             │   patiënt-
                          Notificatie betrokkenen  ◄────────────────┤   vertrouwen
                                                                    │
                          [Barrière C4]                             │
                          Forensisch auditspoor    ◄────────────────┘
```

---

## 5. Oorzaken (linkerzijde)

| ID  | Oorzaak                                                         | Risico-ID | Bewijs in broncode                                          |
| --- | --------------------------------------------------------------- | --------- | ----------------------------------------------------------- |
| O1  | PII wordt als plaintext naar logbestanden geschreven            | R01       | `AppointmentServiceImpl.java` r. 1426–1432                  |
| O2  | Geen data-level ACL: elke gebruiker kan alle afspraken opvragen | R03       | `AppointmentResource.java` — geen patiënt-eigenaarcheck     |
| O3  | Lege `@Authorized()`-annotaties op servicemethoden              | R05       | `AppointmentServiceImpl.java` — `getAllProviderSchedules()` |
| O4  | Afspraakmutaties worden niet geaudit-gelogd                     | R07       | Geen logging op `saveAppointment`, `voidAppointment`        |
| O5  | Geen brute-force beveiliging op authenticatie                   | R08       | Geen rate limiting of account lockout geconfigureerd        |

---

## 6. Preventieve barrières (linkerzijde)

| ID  | Barrière                                          | Maatregel                                                                                                     | NEN-7510:2024-2 control     | Status                  |
| --- | ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | --------------------------- | ----------------------- |
| P1  | Verwijder PII uit logstatements                   | Log uitsluitend gepseudonimiseerde UUID; nooit naam, geboortedatum of BSN                                     | A.8.15 — Monitoring         | ❌ Niet geïmplementeerd |
| P2  | Row-level autorisatie op afspraakendpoints        | Controleer in `AppointmentResource.java` of de aanroepende gebruiker behandelaar of patiënt is                | A.8.3 — Toegangsbeveiliging | ❌ Niet geïmplementeerd |
| P3  | Correcte privilege-checks op alle servicemethoden | Vervang lege `@Authorized()` door expliciete privilege-constanten; herstel typfout in `AppointmentUtils.java` | A.8.3 — Toegangsbeveiliging | ❌ Niet geïmplementeerd |
| P4  | Volledige auditlogging van mutaties               | Log `saveAppointment`, `voidAppointment`, `cancelAppointment` met tijdstip, gebruiker-UUID en resource-UUID   | A.8.15 — Monitoring         | ❌ Niet geïmplementeerd |
| P5  | Rate limiting en account lockout                  | Configureer lockout na N mislukte pogingen; voeg IP-rate limiting toe op reverse proxy                        | A.8.5 — Authenticatie       | ❌ Niet geïmplementeerd |

---

## 7. Gevolgen (rechterzijde)

| ID  | Gevolg                                   | Ernst   | Toelichting                                                                                               |
| --- | ---------------------------------------- | ------- | --------------------------------------------------------------------------------------------------------- |
| G1  | AVG-datalek (meldplicht)                 | Kritiek | Bijzondere persoonsgegevens (art. 9 AVG) zijn gelekt; melding bij AP binnen 72 uur verplicht              |
| G2  | Juridische aansprakelijkheid             | Kritiek | Boetes tot € 20 miljoen of 4% jaaromzet; mogelijke civiele claims van patiënten                           |
| G3  | Reputatieschade voor de zorginstelling   | Hoog    | Verlies van patiëntvertrouwen; negatieve media-aandacht                                                   |
| G4  | Verlies van patiëntvertrouwen            | Hoog    | Patiënten mijden de zorginstelling; langetermijnschade aan zorgrelatie                                    |
| G5  | Ontbreken van forensisch bewijsmateriaal | Hoog    | Zonder auditspoor is niet aantoonbaar wie wat heeft ingezien of gewijzigd ("niet gelogd == niet gebeurd") |

---

## 8. Correctieve barrières (rechterzijde)

| ID  | Barrière                       | Maatregel                                                                                          | NEN-7510:2024-2 control        | Status                  |
| --- | ------------------------------ | -------------------------------------------------------------------------------------------------- | ------------------------------ | ----------------------- |
| C1  | Incident response plan         | Vastgelegd procedure voor detectie, indamming, melding en herstel bij datalekken                   | A.5.26 — Respons op incidenten | ❌ Niet vastgelegd      |
| C2  | Detectie via monitoring/alerts | SIEM of logging-alerts die afwijkend gedrag signaleren (bijv. bulk-opvragen van afspraken)         | A.8.15 — Monitoring            | ❌ Niet geïmplementeerd |
| C3  | Notificatie van betrokkenen    | Patiënten informeren conform AVG art. 34 bij hoog-risico datalek                                   | A.5.26 — Respons op incidenten | ❌ Niet vastgelegd      |
| C4  | Forensisch auditspoor          | Onveranderlijk auditlog waarmee achteraf aantoonbaar is wie welke data heeft ingezien of gewijzigd | A.8.15 — Monitoring            | ❌ Niet geïmplementeerd |

---

## 9. Audit mindset: niet gelogd == niet gebeurd

Dit risico illustreert het audit mindset-principe op twee manieren:

**Verkeerde logging (oorzaak O1):** Door PII in logbestanden te schrijven ontstaat juist een extra aanvalsvector. Logging die niet doordacht is ingericht beschermt niet — het vergroot het risico.

**Ontbrekende logging (oorzaak O4 / gevolg G5):** Zonder auditspoor van afspraakmutaties is bij een incident niet aantoonbaar wat er is gebeurd. Een aanvaller die afspraken inziet of manipuleert laat geen spoor achter. Voor een zorgomgeving onder NEN-7510 is dit onacceptabel: het systeem is feitelijk blind voor misbruik.

De correctieve barrières C2 en C4 zijn daarom niet alleen technische maatregelen — ze zijn de minimale voorwaarde om überhaupt te kunnen vaststellen dat een incident heeft plaatsgevonden.

---

## 10. NEN-7510:2024-2 controls — overzicht

| Control | Omschrijving                                | Barrières      |
| ------- | ------------------------------------------- | -------------- |
| A.8.3   | Toegangsbeveiliging tot informatiesystemen  | P2, P3         |
| A.8.5   | Veilige authenticatie                       | P5             |
| A.8.15  | Logging en monitoring                       | P1, P4, C2, C4 |
| A.5.26  | Respons op informatiebeveiligingsincidenten | C1, C3         |

---

## 11. Conclusie

Het top-event, 'onbevoegde toegang tot of openbaarmaking van patiëntgegevens' is het resultaat van meerdere gelijktijdig aanwezige kwetsbaarheden. Geen enkele preventieve barrière is op dit moment geïmplementeerd. Dit betekent dat alle vijf oorzaken ongehinderd tot het top-event kunnen leiden.

Prioriteit ligt bij **P1** (PII uit logs verwijderen) en **P4** (auditlogging implementeren), omdat deze direct de meest kritieke risico's R01 en R07 adresseren en de audit mindset vastleggen in de codebase.
