# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/m3-privilege-typos

**Eis:** M3 — Geen typfouten in constantenamen
**Status vóór:** Niet conform — `"Scedules"` in twee privilege-constanten
**Status na:** Opgelost

### Probleem

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Scedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Scedules";
```

Als de database de correcte spelling bevat, falen alle privilege-checks voor provider schedules.

### Fix

```java
public static final String PRIV_VIEW_PROVIDER_SCHEDULES   = "View Provider Schedules";
public static final String PRIV_MANAGE_PROVIDER_SCHEDULES = "Manage Provider Schedules";
```

### Ontwerppatroon

**Rename (Fowler)** — constante gecorrigeerd zodat deze overeenkomt met de waarde in het systeem.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../AppointmentUtils.java` | `"Scedules"` → `"Schedules"` in twee constanten |

### Regressiecontrole

De correcte spelling stond al in de database en JSPs. Geen gedragswijziging.
