# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/m5-unused-variable

**Eis:** M5 — Geen ongebruikte variabelen
**Status vóór:** Niet conform — `satisfyingConstraints` aangemaakt maar nooit gelezen
**Status na:** Opgelost

### Probleem

```java
boolean satisfyingConstraints = true;
```

Variabele in `getAppointmentsByConstraints()` die nooit werd gelezen — restant van eerdere implementatie.

### Fix

Declaratie verwijderd. De loop-body werkt ongewijzigd verder.

### Ontwerppatroon

**Remove Dead Code** — ongebruikte declaraties verwijderen.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../impl/AppointmentServiceImpl.java` | `boolean satisfyingConstraints = true;` verwijderd |

### Regressiecontrole

Variabele werd niet gelezen. Geen runtimeeffect.
