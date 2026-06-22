package org.openmrs.module.appointmentscheduling.web;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;

import java.util.List;

public class DWRAppointmentServiceTest extends MainResourceControllerTest {

    private DWRAppointmentService service;

    @Before
    public void setUp() throws Exception {
        executeDataSet("standardWebAppointmentTestDataset.xml");
        service = new DWRAppointmentService();
        // Required by getPatientDescription to look up a phone attribute type
        Context.getAdministrationService().setGlobalProperty(
                AppointmentUtils.GP_PATIENT_PHONE_NUMBER, "8");
    }

    // Required by MainResourceControllerTest — not used in these tests
    @Override public String getURI() { return "appointmentscheduling/appointmentblock"; }
    @Override public String getUuid() { return "c0c579b0-8e59-401d-8a4a-976a0b183599"; }
    @Override public long getAllCount() { return 4; }
    @Override @Test public void shouldGetAll() throws Exception { /* not relevant here */ }

    // --- getPatientDescription ---

    @Test
    public void getPatientDescription_nonExistentId_returnsNull() {
        // OpenMRS getPatient() returns null for an ID that doesn't exist
        PatientData result = service.getPatientDescription(9999);
        Assert.assertNull(result);
    }

    @Test
    public void getPatientDescription_validPatient_returnsDataWithName() {
        // Patient 2 is in both the base test dataset and standardWebAppointmentTestDataset.xml
        PatientData result = service.getPatientDescription(2);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getFullName());
    }

    // --- checkProviderOpenConsultations ---

    @Test
    public void checkProviderOpenConsultations_nullId_returnsFalse() {
        Assert.assertFalse(service.checkProviderOpenConsultations(null));
    }

    @Test
    public void checkProviderOpenConsultations_noInConsultationAppointments_returnsFalse() {
        // Appointment 1 is SCHEDULED — provider 1 has no INCONSULTATION appointments in dataset
        Assert.assertFalse(service.checkProviderOpenConsultations(1));
    }

    // --- checkProviderOpenConsultationsByPatient ---

    @Test
    public void checkProviderOpenConsultationsByPatient_nullId_returnsFalse() {
        Assert.assertFalse(service.checkProviderOpenConsultationsByPatient(null));
    }

    @Test
    public void checkProviderOpenConsultationsByPatient_patientWithNoInConsultation_returnsFalse() {
        // Patient 2's last appointment is MISSED; its provider has no INCONSULTATION in dataset
        Assert.assertFalse(service.checkProviderOpenConsultationsByPatient(2));
    }

    // --- getPatientsInAppointmentBlock ---

    @Test
    public void getPatientsInAppointmentBlock_nullId_returnsNull() {
        List<List<AppointmentData>> result = service.getPatientsInAppointmentBlock(null);
        Assert.assertNull(result);
    }

    @Test
    public void getPatientsInAppointmentBlock_validBlock_returnsThreeLists() {
        // Block 1 has time slots with SCHEDULED and MISSED appointments
        List<List<AppointmentData>> result = service.getPatientsInAppointmentBlock(1);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void getPatientsInAppointmentBlock_validBlock_scheduledAppointmentInList1() {
        List<List<AppointmentData>> result = service.getPatientsInAppointmentBlock(1);
        // Appointment 1 (time_slot 1, SCHEDULED) ends up in the scheduled bucket (index 1)
        Assert.assertFalse(result.get(1).isEmpty());
    }

    // --- getAppointmentBlocks ---

    @Test
    public void getAppointmentBlocks_emptyDatesNoFilters_returnsNonNullList() throws Exception {
        List<AppointmentBlockData> blocks = service.getAppointmentBlocks("", "", null, null, null);
        Assert.assertNotNull(blocks);
    }

    @Test
    public void getAppointmentBlocks_filterByProvider1_returnsNonNullList() throws Exception {
        List<AppointmentBlockData> blocks = service.getAppointmentBlocks("", "", null, 1, null);
        Assert.assertNotNull(blocks);
    }

    @Test
    public void getAppointmentBlocks_filterByProvider2_returnsNonNullList() throws Exception {
        List<AppointmentBlockData> blocks = service.getAppointmentBlocks("", "", null, 2, null);
        Assert.assertNotNull(blocks);
    }
}
