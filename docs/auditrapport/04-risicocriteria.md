# Risicocriteria — OpenMRS Appointment Scheduler

**Sprint:** 2  
**Taak:** SOF-35: Risicocriteria definiëren  
**Module:** OpenMRS Appointment Scheduler  
**Versie:** 1

---

## 1. Doel

Dit document legt de criteria vast waarmee risico's in het project worden beoordeeld. De scoreschaal, risicobereidheid en grenswaarden die hier gedefinieerd worden, zijn de gemeenschappelijke basis voor:

- `04-risicomatrix.md` — applicatierisico's
- `05-bowtie.md` — diepteanalyse hoogste risico
- De risicomatrix voor het CI/CD-proces (SOF-36)
- `06-security-backlog.md` — prioritering van bevindingen

---

## 2. Scoreschaal

Elk risico wordt gescoord op twee dimensies: **kans** (hoe waarschijnlijk is het dat de dreiging zich voordoet?) en **impact** (hoe groot zijn de gevolgen als het misgaat?). De eindscore is het product van beide.

### 2.1 Kansschaal

| Score | Niveau                | Omschrijving                                                                  | Voorbeeld                                               |
| ----- | --------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------- |
| 1     | Zeer onwaarschijnlijk | Komt zelden of nooit voor; vereist specifieke kennis en toegang               | Nation-state aanval gericht op deze module              |
| 2     | Onwaarschijnlijk      | Vereist gerichte inspanning; geen bekende actieve exploits                    | Handmatige SQL-injectie zonder geautomatiseerde tooling |
| 3     | Mogelijk              | Kan voorkomen bij standaard aanvalstechnieken; exploit is publiek beschikbaar | XSS via bekend patroon in userinput                     |
| 4     | Waarschijnlijk        | Komt regelmatig voor in vergelijkbare systemen; laagdrempelig te exploiteren  | Gebruik van verouderde library met publieke CVE         |
| 5     | Zeer waarschijnlijk   | Bijna zeker bij blootstelling; geautomatiseerde tools vinden dit vanzelf      | Ontbrekende authenticatie op publiek endpoint           |

### 2.2 Impactschaal

| Score | Niveau          | Omschrijving                                                                                                 | Voorbeeld                                                        |
| ----- | --------------- | ------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| 1     | Verwaarloosbaar | Geen merkbare schade; intern oplosbaar zonder externe gevolgen                                               | Informatielekje in foutmelding zonder gevoelige data             |
| 2     | Beperkt         | Beperkte schade; geen patiëntdata betrokken; snel herstelbaar                                                | Tijdelijke beschikbaarheidsonderbreking van de module            |
| 3     | Matig           | Beperkt aantal gebruikers of records getroffen; herstel kost inspanning                                      | Ongeautoriseerde inzage in afspraken van één patiënt             |
| 4     | Ernstig         | Grootschalig datalek of langdurige uitval; AVG-meldplicht mogelijk van toepassing                            | Diefstal van alle afspraakgegevens uit de database               |
| 5     | Catastrofaal    | Directe patiëntschade, volledige uitval of onomkeerbaar reputatieverlies; strafrechtelijke aansprakelijkheid | Manipulatie van medische afspraken met gevolgen voor behandeling |

### 2.3 Risicoscore

```
Risicoscore = Kans × Impact   (schaal 1–25)
```

---

## 3. Risicobereidheid en grenswaarden

De risicobereidheid beschrijft welk niveau van restrisico het team/de organisatie acceptabel acht **na** het toepassen van maatregelen.

| Zone        | Scorebereik | Kleurcode | Definitie                                                                 | Vereiste actie                                                                |
| ----------- | ----------- | --------- | ------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **Kritiek** | 20–25       | 🔴 Rood   | Onacceptabel; directe bedreiging voor patiëntveiligheid of AVG-compliance | Onmiddellijke mitigatie vereist nog in huidige sprint                         |
| **Hoog**    | 12–19       | 🟠 Oranje | Niet acceptabel als structurele situatie; mitigatie plannen en uitvoeren  | Opnemen in security backlog met prioriteit Hoog; aanpakken binnen 1 sprint    |
| **Midden**  | 6–11        | 🟡 Geel   | Acceptabel mits gemonitord; mitigatie wenselijk maar niet urgent          | Opnemen in security backlog met prioriteit Midden; aanpakken binnen 2 sprints |
| **Laag**    | 1–5         | 🟢 Groen  | Acceptabel restrisico; geen directe actie vereist                         | Vastleggen en periodiek herbeoordelen                                         |

### 3.1 Risicobereidheid in woorden

> Het team accepteert uitsluitend risico's in de **groene zone** als restrisico na mitigatie. Risico's in de oranje of rode zone mogen **niet open blijven staan** zonder een concreet en gepland mitigatieplan. Risico's in de rode zone worden **onmiddellijk geëscaleerd** naar de opdrachtgever/docent.

### 3.2 Drempelwaarden samengevat

| Grenswaarde                    | Score               |
| ------------------------------ | ------------------- |
| Maximaal acceptabel restrisico | ≤ 5 (groen)         |
| Mitigatie verplicht            | ≥ 6 (geel en hoger) |
| Directe actie vereist          | ≥ 20 (rood)         |
| Escalatie naar opdrachtgever   | ≥ 20 (rood)         |

---

## 4. CIA-weging

Voor de Appointment Scheduler wordt de volgende weging gehanteerd bij het bepalen van de impactscore, gebaseerd op de aard van de verwerkte data (patiëntgegevens vallen onder AVG art. 9):

| CIA-dimensie                            | Wegingsfactor | Toelichting                                                                |
| --------------------------------------- | ------------- | -------------------------------------------------------------------------- |
| **Confidentiality** (Vertrouwelijkheid) | Hoog          | Patiëntdata is bijzonder persoonsgegeven; AVG-meldplicht bij lek           |
| **Integrity** (Integriteit)             | Hoog          | Manipulatie van afspraken kan directe zorggevolgen hebben                  |
| **Availability** (Beschikbaarheid)      | Midden        | Uitval is vervelend maar geen directe patiëntschade bij korte onderbreking |

Bij het scoren van impact wordt de CIA-dimensie met de hoogste wegingsfactor als uitgangspunt genomen.

---

## 5. Contextuele scoring

Naast de standaard CVSS-score hanteren we een **contextuele score** die rekening houdt met de specifieke situatie van de module. De contextuele score kan de CVSS-score naar boven of beneden bijstellen op basis van:

| Factor                                      | Effect op score            | Voorbeeld                                         |
| ------------------------------------------- | -------------------------- | ------------------------------------------------- |
| Kwetsbaarheid alleen intern bereikbaar      | Score verlagen (−1 tot −2) | Database-exploit via intern netwerk               |
| Patiëntdata direct betrokken                | Score verhogen (+1 tot +2) | IDOR op afspraakendpoint geeft patiëntnamen terug |
| Exploit publiek beschikbaar (PoC op GitHub) | Score verhogen (+1)        | CVE met gepubliceerde exploit                     |
| Compenserende maatregel aanwezig            | Score verlagen (−1)        | WAF of extra authenticatielaag actief             |

De contextuele score wordt expliciet vermeld in de security backlog (`06-security-backlog.md`) naast de CVSS-score.

---

## 6. Herevaluatie

Risico's worden herbeoordeeld:

- Bij elke nieuwe sprint (bij significante codewijzigingen)
- Na een penetration test
- Na het uitkomen van een nieuwe CVE die een gebruikte library raakt
- Na een security-incident

---

## 7. Referenties

- NEN-7510:2024-2 — Informatiebeveiliging in de zorg
- ISO/IEC 27005:2022 — Information security risk management
- CVSS v3.1 Calculator: [https://www.first.org/cvss/calculator/3.1](https://www.first.org/cvss/calculator/3.1)
- AVG artikel 9 — Verwerking van bijzondere categorieën persoonsgegevens
