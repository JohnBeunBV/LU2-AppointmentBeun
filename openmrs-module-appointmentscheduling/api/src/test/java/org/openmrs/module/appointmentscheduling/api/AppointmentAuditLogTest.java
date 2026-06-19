package org.openmrs.module.appointmentscheduling.api;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.Appointment.AppointmentStatus;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integratietests die verifiëren dat alle mutatieacties op Appointment worden geaudit
 * en dat geen PII in de logoutput verschijnt.
 *
 * Vereisten:
 *  - Succesvolle acties (CREATE, UPDATE, VOID, CANCEL) produceren een [AUDIT]-regel.
 *  - Mislukte/geweigerde acties produceren geen [AUDIT]-regel (methode body bereikt niet).
 *  - Geen PII (naam, geboortedatum, identifier, geslacht) in logberichten.
 */
public class AppointmentAuditLogTest extends BaseModuleContextSensitiveTest {

    private AppointmentService service;
    private AuditCapturingAppender appender;
    private Level originalLevel;

    @Before
    public void before() throws Exception {
        service = Context.getService(AppointmentService.class);
        executeDataSet("standardAppointmentTestDataset.xml");

        appender = new AuditCapturingAppender();

        // OpenMRS test log4j config sets root level to WARN/ERROR, which silently drops
        // log.info() calls before they reach any appender. Force INFO for this test class.
        Logger serviceLogger = Logger.getLogger(
                "org.openmrs.module.appointmentscheduling.api.impl.AppointmentServiceImpl");
        originalLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.INFO);
        serviceLogger.addAppender(appender);
    }

    @After
    public void after() {
        Logger serviceLogger = Logger.getLogger(
                "org.openmrs.module.appointmentscheduling.api.impl.AppointmentServiceImpl");
        serviceLogger.removeAppender(appender);
        serviceLogger.setLevel(originalLevel);
    }

    // ── CREATE ───────────────────────────────────────────────────────────────────

    @Test
    public void saveAppointment_shouldWriteAuditLogEntry() {
        Appointment appointment = service.getAppointment(1);
        appender.clear();

        service.saveAppointment(appointment);

        assertTrue("saveAppointment moet een [AUDIT]-regel schrijven",
                appender.containsAudit("action=saveAppointment"));
    }

    // ── VOID ─────────────────────────────────────────────────────────────────────

    @Test
    public void voidAppointment_shouldWriteAuditLogEntry() {
        Appointment appointment = service.getAppointment(1);
        appender.clear();

        service.voidAppointment(appointment, "test void");

        assertTrue("voidAppointment moet een [AUDIT]-regel schrijven",
                appender.containsAudit("action=voidAppointment"));
    }

    // ── CANCEL (via changeAppointmentStatus) ─────────────────────────────────────

    @Test
    public void changeAppointmentStatus_toCANCELLED_shouldWriteAuditLogEntry() {
        Appointment appointment = service.getAppointment(1);
        appender.clear();

        service.changeAppointmentStatus(appointment, AppointmentStatus.CANCELLED);

        assertTrue("changeAppointmentStatus naar CANCELLED moet een [AUDIT]-regel schrijven",
                appender.containsAudit("action=changeAppointmentStatus"));
        assertTrue("Audit-regel moet newStatus=Cancelled bevatten",
                appender.containsAudit("newStatus=Cancelled"));
    }

    // ── UPDATE (status wijziging) ─────────────────────────────────────────────────

    @Test
    public void changeAppointmentStatus_toCompleted_shouldWriteAuditLogEntry() {
        Appointment appointment = service.getAppointment(1);
        appender.clear();

        service.changeAppointmentStatus(appointment, AppointmentStatus.COMPLETED);

        assertTrue("changeAppointmentStatus naar COMPLETED moet een [AUDIT]-regel schrijven",
                appender.containsAudit("action=changeAppointmentStatus"));
        assertTrue("Audit-regel moet newStatus=Completed bevatten",
                appender.containsAudit("newStatus=Completed"));
    }

    // ── BOOK ─────────────────────────────────────────────────────────────────────

    @Test
    public void bookAppointment_shouldWriteAuditLogEntry() throws Exception {
        TimeSlot spaciousSlot = service.getTimeSlot(8);
        AppointmentType type = service.getAppointmentType(1);
        Patient patient = Context.getPatientService().getPatient(2);

        Appointment appointment = new Appointment();
        appointment.setTimeSlot(spaciousSlot);
        appointment.setAppointmentType(type);
        appointment.setPatient(patient);
        appender.clear();

        service.bookAppointment(appointment, true);

        assertTrue("bookAppointment moet een [AUDIT]-regel schrijven",
                appender.containsAudit("action=bookAppointment"));
    }

    // ── PII-AFWEZIGHEID ──────────────────────────────────────────────────────────

    /**
     * Audit-regels mogen geen PII bevatten: naam, geboortedatum, identifier of geslacht
     * van de patiënt mogen niet als leesbare string in de log verschijnen.
     * Alleen UUID's zijn toegestaan als patiëntverwijzing.
     */
    @Test
    public void auditLog_shouldNotContainPatientPii() {
        Patient patient = Context.getPatientService().getPatient(2);
        assertNotNull(patient);

        // Verzamel de PII-waarden die NIET in de log mogen verschijnen
        List<String> piiValues = new ArrayList<String>();

        // Naam
        if (patient.getPersonName() != null && patient.getPersonName().getFullName() != null) {
            String fullName = patient.getPersonName().getFullName().trim();
            if (!fullName.isEmpty()) {
                piiValues.add(fullName);
            }
        }

        // Geboortedatum als string (yyyy-MM-dd gedeelte)
        if (patient.getBirthdate() != null) {
            // Voeg het jaar toe als proxy voor de datum — uniek genoeg voor de check
            String birthYear = String.valueOf(patient.getBirthdate().getYear() + 1900);
            // Alleen aanmerkelijk als het een specifiek jaar is, niet '1970' default
            if (!birthYear.equals("1970")) {
                piiValues.add(birthYear);
            }
        }

        // Patiëntidentifier(s)
        if (patient.getPatientIdentifier() != null
                && patient.getPatientIdentifier().getIdentifier() != null) {
            String identifier = patient.getPatientIdentifier().getIdentifier().trim();
            if (!identifier.isEmpty()) {
                piiValues.add(identifier);
            }
        }

        // Geslacht
        if (patient.getGender() != null && !patient.getGender().isEmpty()) {
            // "M" en "F" zijn te kort voor een betrouwbare grep; check via de
            // volledige string "gender=M" of "gender=F" die een slechte implementatie
            // zou schrijven — maar sla pure enkelboekers over.
            if (patient.getGender().length() > 1) {
                piiValues.add(patient.getGender());
            }
        }

        // Voer een actie uit die een audit-regel produceert
        Appointment appointment = service.getAppointment(1);
        appender.clear();
        service.saveAppointment(appointment);

        // Controleer elke audit-regel op afwezigheid van PII
        List<String> auditMessages = appender.getAuditMessages();
        assertFalse("Er moeten audit-berichten zijn om te controleren", auditMessages.isEmpty());

        for (String pii : piiValues) {
            for (String message : auditMessages) {
                assertFalse(
                        "Audit-log bevat PII '" + pii + "' in bericht: " + message,
                        message.contains(pii)
                );
            }
        }
    }

    /**
     * Audit-regels mogen nooit methode-aanroepen bevatten die PII retourneren.
     * Dit is een defensieve check: als iemand per ongeluk een toString() aanroept
     * op een Patient-object, mag de uitvoer hiervan niet in de log belanden.
     */
    @Test
    public void auditLog_shouldNotContainPiiMethodOutputPatterns() {
        Appointment appointment = service.getAppointment(1);
        appender.clear();

        service.saveAppointment(appointment);
        service.voidAppointment(service.getAppointment(2), "test");
        service.changeAppointmentStatus(service.getAppointment(4), AppointmentStatus.CANCELLED);

        List<String> auditMessages = appender.getAuditMessages();
        for (String message : auditMessages) {
            // Patronen die duiden op PII-lekkage via object-toString of directe veldwaarden
            assertFalse("Audit-log bevat mogelijk PII via getPersonName: " + message,
                    message.matches(".*\\b[A-Z][a-z]+ [A-Z][a-z]+\\b.*")); // "Voornaam Achternaam"
            assertFalse("Audit-log bevat 'birthdate' als sleutel: " + message,
                    message.toLowerCase().contains("birthdate="));
            assertFalse("Audit-log bevat 'identifier=' als sleutel: " + message,
                    message.toLowerCase().contains("identifier="));
            assertFalse("Audit-log bevat 'gender=' als sleutel: " + message,
                    message.toLowerCase().contains("gender="));
        }
    }

    // ── GEWEIGERDE TOEGANG ────────────────────────────────────────────────────────

    /**
     * Wanneer een niet-geauthenticeerde aanroep een @Authorized-methode raakt,
     * gooit Spring een APIAuthenticationException vóórdat de methodelogica draait.
     * Er mag dus géén [AUDIT]-regel verschijnen voor geweigerde acties.
     */
    @Test
    public void unauthorizedAccess_shouldThrowExceptionAndNotWriteAuditEntry() throws Exception {
        Context.logout();
        appender.clear();

        try {
            service.saveAppointment(new Appointment());
        } catch (APIAuthenticationException expected) {
            // Verwachte uitkomst: toegang geweigerd
        } finally {
            // Herstel authenticatie voor @After cleanup
            authenticate();
        }

        assertFalse(
                "Een geweigerde aanroep mag geen [AUDIT]-regel produceren (methode body niet bereikt)",
                appender.hasAnyAuditMessage()
        );
    }

    // ── HULPKLASSE ───────────────────────────────────────────────────────────────

    private static class AuditCapturingAppender extends AppenderSkeleton {

        private final List<String> messages = new ArrayList<String>();

        @Override
        protected void append(LoggingEvent event) {
            String message = event.getRenderedMessage();
            if (message != null && message.contains("[AUDIT]")) {
                messages.add(message);
            }
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            messages.clear();
        }

        void clear() {
            messages.clear();
        }

        boolean containsAudit(String fragment) {
            for (String msg : messages) {
                if (msg.contains(fragment)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasAnyAuditMessage() {
            return !messages.isEmpty();
        }

        List<String> getAuditMessages() {
            return new ArrayList<String>(messages);
        }
    }
}
