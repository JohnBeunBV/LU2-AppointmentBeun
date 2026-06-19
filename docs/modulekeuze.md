## Module-keuze vastleggen

## Gekozen module

| Eigenschap   | Waarde                                                          |
| ------------ | --------------------------------------------------------------- |
| Naam         | Appointment Scheduling Module                                   |
| Artifact ID  | appointmentscheduling                                           |
| Versie       | 1.17.0-SNAPSHOT                                                 |
| Broncode     | https://github.com/openmrs/openmrs-module-appointmentscheduling |
| Documentatie | https://wiki.openmrs.org/display/docs/Appointment+Module+Module |

## Beschrijving van de module

De Appointment Scheduling Module is een OpenMRS-module waarmee afspraken van patiënten kunnen worden gepland en beheerd. De module ondersteunt onder andere het beheren van afspraken, tijdslots, zorgverleners en agenda's. Hierdoor kunnen zorginstellingen patiëntafspraken efficiënt organiseren en opvolgen.

## Motivatie voor de keuze

Voor dit project is gekozen voor de Appointment Scheduling Module vanwege de omvang, complexiteit en de aanwezigheid van bedrijfskritische functionaliteit. Ook is deze module niet te klein waardoor we hopelijk genoeg risico's kunnen dichten.

### Complexiteit

De module bevat verschillende functionele onderdelen die met elkaar samenwerken, waaronder:

- Afspraakbeheer
- Planning van zorgverleners
- Agenda- en tijdslotbeheer
- Patiëntregistratie gekoppeld aan afspraken
- Validatie van planningsregels

Hierdoor bevat de module voldoende businesslogica om een grondige analyse op onderhoudbaarheid uit te voeren.

### Scope

De module heeft een afgebakende scope waardoor het project beheersbaar blijft binnen de beschikbare sprinttijd. Tegelijkertijd is de module groot genoeg om verbeterpunten te identificeren op het gebied van:

- Onderhoudbaarheid
- Testbaarheid
- Codekwaliteit
- Architectuur
- Security

### Kritieke functionaliteit

Afsprakenplanning vormt een belangrijk onderdeel binnen zorgsystemen. Fouten in deze functionaliteit kunnen leiden tot:

- Gemiste afspraken
- Dubbele boekingen
- Verkeerde planning van zorgverleners
- Verminderde beschikbaarheid van zorg

Hierdoor is de module geschikt voor zowel het onderhoudbaarheidsonderzoek als het security- en complianceonderzoek.

## Verwachte onderzoeksmogelijkheden

Binnen deze module verwachten wij voldoende mogelijkheden te vinden voor:

### Onderhoudbaarheid

- Code smells identificeren
- Complexe onderdelen analyseren
- Refactoringvoorstellen opstellen
- Verbeteringen aantonen met testen

### Security & Compliance

- Security code reviews uitvoeren
- Software Bill of Materials (SBOM) opstellen
- Afhankelijkheden analyseren op bekende kwetsbaarheden (CVE's)
- Penetratietesten uitvoeren op kritische functionaliteit
- Mitigaties ontwerpen en implementeren

In de risicoanalyse gaan we uitgebreider in op welke risico's er binnen deze module zijn.
