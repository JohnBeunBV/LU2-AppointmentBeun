# Gap-Analyse: OpenMRS Appointment Scheduling Module vs NEN-7510:2024-2

**Datum**: Juni 2, 2026 | **Totaalscore: 51% compliant**

## Samenvatting per Control

| Control                     | Status          | Score | Bewijs                                                     |
| --------------------------- | --------------- | ----- | ---------------------------------------------------------- |
| A.8.3 - Toegangsbeveiliging | 🟡 Gedeeltelijk | 60%   | @Authorized decorators, 13 privileges, geen data-level ACL |
| A.8.5 - Authenticatie       | 🟢 Aanwezig     | 75%   | OpenMRS Context auth werkend, geen MFA/login logging       |
| A.8.15 - Logging            | 🔴 Afwezig      | 20%   | Basis framework, **KRITIEK: PII in logs**                  |

---

## A.8.3 - TOEGANGSBEVEILIGING

**Aanwezig:**

- ✅ 13 privileges in [config.xml](openmrs-module-appointmentscheduling/omod/src/main/resources/config.xml)
- ✅ @Authorized decorators op 80+ methoden in [AppointmentService.java](openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/api/AppointmentService.java)
- ✅ Runtime permission checks in controllers

**Afwezig:**

- ❌ Data-level Access Control Lists (ACL)
- ❌ Segregation of Duties
- ❌ Patiënt ziet alle afspraken van anderen

---

## A.8.5 - AUTHENTICATIE

**Aanwezig:**

- ✅ Context.isAuthenticated() checks in [AppointmentBlockCalendarController.java](../../openmrs-module-appointmentscheduling/omod/src/main/java/org/openmrs/module/appointmentscheduling/web/controller/AppointmentBlockCalendarController.java#L51)
- ✅ User identification methods
- ✅ Session management

**Afwezig:**

- ❌ Geen login/logout event logging
- ❌ Geen multi-factor authentication
- ❌ Geen brute force protection

---

## A.8.15 - LOGGING ⚠️ KRITIEK

**Aanwezig:**

- ✅ Log framework (Apache Commons Logging)
- ✅ Error logging in exceptions

**🔴 KRITIEKE VULNERABILITY:**

```java
// openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/api/impl/AppointmentServiceImpl.java (line 1424-1430)
log.info("[AUDIT] Fetching appointments for patient: "
    + "name=" + patient.getPersonName()           // ❌ PII
    + " dob=" + patient.getBirthdate()            // ❌ PII
    + " identifier=" + patient.getPatientIdentifier().getIdentifier()  // ❌ PII
    + " gender=" + patient.getGender());
```

**Impact:** GDPR/HIPAA schending, gevoelige data in logs

**Afwezig:**

- ❌ Audit trail voor afspraken (create/update/delete)
- ❌ Authorization failure logging
- ❌ User activity logging

---

## Prioriteiten voor Oplossing

| Priority   | Issue               | Actie                                                         |
| ---------- | ------------------- | ------------------------------------------------------------- |
| 🔴 KRITIEK | PII in logs         | Verwijder patient.getPersonName(), DOB, ID uit line 1424-1430 |
| 🔴 KRITIEK | Geen audit logging  | Implementeer logging voor alle appointment changes            |
| 🟡 HOOG    | Geen data-level ACL | Patients mogen alleen eigen afspraken zien                    |
