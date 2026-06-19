# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/m2-retire-void-flags

**Eis:** M2 — Methoden doen wat ze beloven
**Status vóór:** Niet conform (per GAP-analyse)
**Status na:** Opgelost

### Probleem (historisch)

`retireAppointmentType()` en `voidAppointment()` sloegen het object op zonder de bijbehorende vlag te zetten.

### Fix

Al toegepast vóór deze sprint:
- `retireAppointmentType()`: `setRetired(true)` + `setRetireReason(reason)`
- `voidAppointment()`: `setVoided(true)` + `setVoidReason(reason)`
- `voidTimeSlot()`: idem
- `voidAppointmentBlock()`: idem + cascade naar time slots

### Ontwerppatroon

**Tell, Don’t Ask** — de methode past de toestand toe die ze belooft.

### Gewijzigde bestanden

Geen codewijziging — al correct in de codebase.
