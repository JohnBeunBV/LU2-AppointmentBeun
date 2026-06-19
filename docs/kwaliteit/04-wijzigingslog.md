# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/b11-studentt-divide-by-zero

**Eis:** B1.1 — Geen ConstantValue / DivisionByZero expressies (Qodana)
**Status vóór:** Niet conform — `1.0 / 0.0` als literal in conditiecheck
**Status na:** Opgelost

### Probleem

```java
if (fg != 1.0 / 0.0 && fg != -1.0 / 0.0) {
```

Qodana markeert `1.0 / 0.0` als NumericOverflow — het is een constante expressie voor infinity.

### Fix

```java
if (!Double.isInfinite(fg)) {
```

### Ontwerppatroon

**Replace Magic Value** — gebruik de API-methode die de intentie direct uitdrukt.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../appointmentscheduling/StudentT.java` | `fg != 1.0/0.0 && fg != -1.0/0.0` → `!Double.isInfinite(fg)` |

### Regressiecontrole

Semantisch identiek aan de originele vergelijking.
