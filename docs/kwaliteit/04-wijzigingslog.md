# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/m4-concurrent-modification

**Eis:** M4 — Correcte iteratie over collecties
**Status vóór:** Niet conform (per GAP-analyse)
**Status na:** Opgelost

### Probleem (historisch)

`cleanOpenAppointments()` verwijderde elementen via `list.remove()` tijdens actieve iteratie.

### Fix

Al toegepast vóór deze sprint. De methode gebruikt `iter.remove()` op de juiste plekken.

### Ontwerppatroon

**Iterator Pattern** — gebruik `Iterator.remove()` voor veilig verwijderen tijdens iteratie.

### Gewijzigde bestanden

Geen codewijziging — al correct in de codebase.
