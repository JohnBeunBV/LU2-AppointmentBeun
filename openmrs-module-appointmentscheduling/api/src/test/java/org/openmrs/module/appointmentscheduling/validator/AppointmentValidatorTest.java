package org.openmrs.module.appointmentscheduling.validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmrs.Patient;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class AppointmentValidatorTest {

    private AppointmentValidator validator;

    @Before
    public void setUp() {
        validator = new AppointmentValidator();
    }

    @Test
    public void supports_shouldSupportAppointmentClass() {
        assertTrue(validator.supports(Appointment.class));
    }

    @Test
    public void supports_shouldNotSupportArbitraryClass() {
        assertFalse(validator.supports(Object.class));
    }

    // Null appointment uses a mock Errors because BindException requires a non-null target object.
    @Test
    public void validate_nullAppointment_shouldRejectWithGeneralError() {
        Errors errors = Mockito.mock(Errors.class);
        validator.validate(null, errors);
        verify(errors).rejectValue("appointment", "error.general");
    }

    @Test
    public void validate_appointmentWithNullTimeSlot_shouldRejectTimeSlotField() {
        Appointment appointment = buildValidAppointment();
        appointment.setTimeSlot(null);

        Errors errors = new BindException(appointment, "appointment");
        validator.validate(appointment, errors);

        assertNotNull("Expected error on timeSlot field", errors.getFieldError("timeSlot"));
        assertEquals("appointmentscheduling.Appointment.emptyTimeSlot",
                errors.getFieldError("timeSlot").getCode());
    }

    @Test
    public void validate_appointmentWithNullPatient_shouldRejectPatientField() {
        Appointment appointment = buildValidAppointment();
        appointment.setPatient(null);

        Errors errors = new BindException(appointment, "appointment");
        validator.validate(appointment, errors);

        assertNotNull("Expected error on patient field", errors.getFieldError("patient"));
        assertEquals("appointmentscheduling.Appointment.emptyPatient",
                errors.getFieldError("patient").getCode());
    }

    @Test
    public void validate_appointmentWithNullType_shouldRejectAppointmentTypeField() {
        Appointment appointment = buildValidAppointment();
        appointment.setAppointmentType(null);

        Errors errors = new BindException(appointment, "appointment");
        validator.validate(appointment, errors);

        assertNotNull("Expected error on appointmentType field", errors.getFieldError("appointmentType"));
        assertEquals("appointmentscheduling.Appointment.emptyType",
                errors.getFieldError("appointmentType").getCode());
    }

    @Test
    public void validate_appointmentTypeNotSupportedByBlock_shouldRejectWithNotSupportedTypeCode() {
        AppointmentType unsupportedType = new AppointmentType("Unsupported", "Not in block", 30);
        unsupportedType.setId(99);

        Appointment appointment = buildValidAppointment();
        appointment.setAppointmentType(unsupportedType);

        Errors errors = new BindException(appointment, "appointment");
        validator.validate(appointment, errors);

        assertNotNull("Expected error on appointmentType field", errors.getFieldError("appointmentType"));
        assertEquals("appointmentscheduling.Appointment.notSupportedType",
                errors.getFieldError("appointmentType").getCode());
    }

    @Test
    public void validate_fullyValidAppointment_shouldProduceNoErrors() {
        Appointment appointment = buildValidAppointment();

        Errors errors = new BindException(appointment, "appointment");
        validator.validate(appointment, errors);

        assertFalse("Expected no validation errors for a valid appointment", errors.hasErrors());
    }

    // Builds a minimal valid Appointment: type present, type supported by block, patient set.
    private Appointment buildValidAppointment() {
        AppointmentType type = new AppointmentType("General", "General appointment", 30);
        type.setId(1);

        Set<AppointmentType> supportedTypes = new HashSet<AppointmentType>();
        supportedTypes.add(type);

        AppointmentBlock block = new AppointmentBlock();
        block.setTypes(supportedTypes);

        TimeSlot slot = new TimeSlot();
        slot.setAppointmentBlock(block);

        Patient patient = new Patient(1);

        Appointment appointment = new Appointment();
        appointment.setTimeSlot(slot);
        appointment.setPatient(patient);
        appointment.setAppointmentType(type);

        return appointment;
    }
}
