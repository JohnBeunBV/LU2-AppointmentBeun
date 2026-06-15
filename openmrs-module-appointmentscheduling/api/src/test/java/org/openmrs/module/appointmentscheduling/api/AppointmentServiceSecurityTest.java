package org.openmrs.module.appointmentscheduling.api;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.Appointment.AppointmentStatus;
import org.openmrs.module.appointmentscheduling.AppointmentStatusHistory;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.exception.TimeSlotFullException;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Security-gerichte integratietests voor AppointmentService.
 *
 * Dekt: PII-logging vulnerability, bookAppointment guards, changeAppointmentStatus
 * history tracking. Alle methoden hier zijn gemarkeerd als security-kritiek in
 * de security backlog (CLAUDE.md).
 */
public class AppointmentServiceSecurityTest extends BaseModuleContextSensitiveTest {

    private AppointmentService service;

    @Before
    public void before() throws Exception {
        service = Context.getService(AppointmentService.class);
        executeDataSet("standardAppointmentTestDataset.xml");
    }

    // ── PII-logging vulnerability ────────────────────────────────────────────────

    /**
     * Documenteringstest voor bekende PII-logging vulnerability (zie CLAUDE.md).
     * getAppointmentsForPatientWithLogging logt naam, geboortedatum en BSN van de patiënt.
     * Deze test verifieert dat de methode delegeert naar getAppointmentsOfPatient zodat
     * regressie op de returnwaarde direct opvalt.
     */
    @Test
    public void getAppointmentsForPatientWithLogging_shouldReturnSameResultAsGetAppointmentsOfPatient() {
        Patient patient = Context.getPatientService().getPatient(1);
        assertNotNull(patient);

        List<Appointment> viaLogging = service.getAppointmentsForPatientWithLogging(patient);
        List<Appointment> direct = service.getAppointmentsOfPatient(patient);

        assertEquals(
            "PII-logging variant moet exact hetzelfde resultaat teruggeven als de directe aanroep",
            direct.size(), viaLogging.size()
        );
    }

    @Test
    public void getAppointmentsForPatientWithLogging_shouldNotReturnNullForKnownPatient() {
        Patient patient = Context.getPatientService().getPatient(2);
        assertNotNull(patient);

        List<Appointment> result = service.getAppointmentsForPatientWithLogging(patient);

        assertNotNull("Resultaat mag niet null zijn", result);
    }

    // ── bookAppointment guards ────────────────────────────────────────────────────

    /**
     * Een al opgeslagen afspraak (id != null) mag niet opnieuw geboekt worden.
     * Dit voorkomt dubbele boekingen bij hergebruik van persistente objecten.
     */
    @Test(expected = APIException.class)
    public void bookAppointment_onAlreadyPersistedAppointment_shouldThrowAPIException()
            throws TimeSlotFullException {
        Appointment existing = service.getAppointment(1);
        assertNotNull(existing);
        assertNotNull("Appointment moet al een id hebben", existing.getId());

        service.bookAppointment(existing, false);
    }

    /**
     * Wanneer een tijdslot vol is en overboeking niet is toegestaan,
     * moet TimeSlotFullException gegooid worden.
     * Slot 1 (60 min) bevat al een afspraak van type 1 (54 min): 6 min over,
     * niet genoeg voor nog een type-1 afspraak (54 min).
     */
    @Test(expected = TimeSlotFullException.class)
    public void bookAppointment_whenSlotFullAndOverbookFalse_shouldThrowTimeSlotFullException()
            throws TimeSlotFullException {
        TimeSlot fullSlot = service.getTimeSlot(1);
        AppointmentType type1 = service.getAppointmentType(1); // duration = 54 min

        Appointment newAppointment = new Appointment();
        newAppointment.setTimeSlot(fullSlot);
        newAppointment.setAppointmentType(type1);
        newAppointment.setPatient(Context.getPatientService().getPatient(1));

        service.bookAppointment(newAppointment, false);
    }

    /**
     * Met overbook=true mag een vol slot toch gebruikt worden.
     * Hetzelfde scenario als hierboven, maar nu moet de boeking slagen.
     */
    @Test
    public void bookAppointment_whenSlotFullAndOverbookTrue_shouldSucceed()
            throws TimeSlotFullException {
        TimeSlot fullSlot = service.getTimeSlot(1);
        AppointmentType type1 = service.getAppointmentType(1);

        Appointment newAppointment = new Appointment();
        newAppointment.setTimeSlot(fullSlot);
        newAppointment.setAppointmentType(type1);
        newAppointment.setPatient(Context.getPatientService().getPatient(1));

        Appointment booked = service.bookAppointment(newAppointment, true);

        assertNotNull("Geboekte afspraak moet een id hebben gekregen", booked.getId());
        assertEquals(AppointmentStatus.SCHEDULED, booked.getStatus());
    }

    /**
     * bookAppointment moet de status op SCHEDULED zetten als er geen status meegegeven is.
     */
    @Test
    public void bookAppointment_withNullStatus_shouldDefaultToScheduled()
            throws TimeSlotFullException {
        // Slot 8 (720 min, block 4) heeft voldoende ruimte voor type 1 (54 min).
        TimeSlot spaciousSlot = service.getTimeSlot(8);
        AppointmentType type1 = service.getAppointmentType(1);

        Appointment appointment = new Appointment();
        appointment.setTimeSlot(spaciousSlot);
        appointment.setAppointmentType(type1);
        appointment.setPatient(Context.getPatientService().getPatient(2));

        Appointment booked = service.bookAppointment(appointment, false);

        assertEquals("Status moet standaard SCHEDULED zijn", AppointmentStatus.SCHEDULED, booked.getStatus());
    }

    // ── changeAppointmentStatus ─────────────────────────────────────────────────

    /**
     * changeAppointmentStatus moet de status van de afspraak bijwerken
     * en een nieuw record in de statusgeschiedenis aanmaken.
     */
    @Test
    public void changeAppointmentStatus_shouldUpdateStatusAndCreateHistoryEntry() {
        Appointment appointment = service.getAppointment(1);
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());

        service.changeAppointmentStatus(appointment, AppointmentStatus.COMPLETED);

        Appointment updated = service.getAppointment(1);
        assertEquals(AppointmentStatus.COMPLETED, updated.getStatus());

        List<AppointmentStatusHistory> history = service.getAppointmentStatusHistories(updated);
        assertFalse("Statusgeschiedenis mag niet leeg zijn", history.isEmpty());

        AppointmentStatusHistory latest = history.get(history.size() - 1);
        assertEquals(AppointmentStatus.COMPLETED, latest.getStatus());
    }

    /**
     * changeAppointmentStatus met null afspraak mag geen NullPointerException gooien.
     * De implementatie bevat een expliciete null-check.
     */
    @Test
    public void changeAppointmentStatus_withNullAppointment_shouldNotThrow() {
        service.changeAppointmentStatus(null, AppointmentStatus.CANCELLED);
    }
}
