# 04 — Wijzigingslog

Per branch wordt hieronder gedocumenteerd wat er is gewijzigd, waarom, en welk ontwerppatroon is toegepast.

---

## fix/m1-deprecated-date-api

**Eis:** M1 — Geen gebruik van deprecated API's
**Status vóór:** Niet conform — `Date.getYear()`, `getMonth()`, `getDate()`, `getHours()`, `getMinutes()`, `getSeconds()`
**Status na:** Opgelost

### Probleem

```java
return new Date(
    date.getYear(), date.getMonth(), date.getDate(),
    time.getHours(), time.getMinutes(), time.getSeconds()
);
```

Deprecated sinds Java 1.1.

### Fix

```java
Calendar datePart = Calendar.getInstance();
datePart.setTime(date);
Calendar timePart = Calendar.getInstance();
timePart.setTime(time);
datePart.set(Calendar.HOUR_OF_DAY, timePart.get(Calendar.HOUR_OF_DAY));
datePart.set(Calendar.MINUTE, timePart.get(Calendar.MINUTE));
datePart.set(Calendar.SECOND, timePart.get(Calendar.SECOND));
datePart.set(Calendar.MILLISECOND, 0);
return datePart.getTime();
```

### Ontwerppatroon

**Replace Deprecated API** — verouderde methoden vervangen door de `Calendar`-API.

### Gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `api/.../impl/AppointmentServiceImpl.java` | `getDateAndTime()` herschreven met `Calendar` |

### Regressiecontrole

Semantisch equivalent. `Calendar` is tijdzone-bewust.
