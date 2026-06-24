package org.openmrs.module.appointmentscheduling.web.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceControllerTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

public class AppointmentBlockFormControllerTest extends MainResourceControllerTest {

    private AppointmentBlockFormController controller;

    @Before
    public void setUp() throws Exception {
        executeDataSet("standardWebAppointmentTestDataset.xml");
        controller = new AppointmentBlockFormController();
        Context.getAdministrationService().setGlobalProperty(
                AppointmentUtils.GP_DEFAULT_TIME_SLOT_DURATION, "30");
    }

    // Required by MainResourceControllerTest — not used in these tests
    @Override public String getURI() { return "appointmentscheduling/appointmentblock"; }
    @Override public String getUuid() { return "c0c579b0-8e59-401d-8a4a-976a0b183599"; }
    @Override public long getAllCount() { return 4; }
    @Override @Test public void shouldGetAll() throws Exception { /* not relevant here */ }

    // --- getAppointmentBlock ---

    @Test
    public void getAppointmentBlock_nullId_returnsNewEmptyBlock() {
        AppointmentBlock block = controller.getAppointmentBlock(null);
        Assert.assertNotNull(block);
        Assert.assertNull(block.getId());
    }

    @Test
    public void getAppointmentBlock_existingId_returnsBlock() {
        AppointmentBlock block = controller.getAppointmentBlock(1);
        Assert.assertNotNull(block);
        Assert.assertEquals(Integer.valueOf(1), block.getId());
    }

    // --- getTimeSlotLength ---

    @Test
    public void getTimeSlotLength_paramProvided_returnsParam() {
        AppointmentBlock block = new AppointmentBlock();
        String result = controller.getTimeSlotLength(block, "45");
        Assert.assertEquals("45", result);
    }

    @Test
    public void getTimeSlotLength_newBlock_returnsGlobalPropertyDefault() {
        AppointmentBlock newBlock = new AppointmentBlock(); // no creator → new block
        String result = controller.getTimeSlotLength(newBlock, null);
        Assert.assertEquals("30", result);
    }

    @Test
    public void getTimeSlotLength_existingBlock_returnsSlotDurationFromFirstTimeSlot() {
        // Block 1: time_slot_id=1, start=2006-01-01 00:00, end=2006-01-01 01:00 → 60 min
        AppointmentBlock block = controller.getAppointmentBlock(1);
        String result = controller.getTimeSlotLength(block, null);
        Assert.assertEquals("60", result);
    }

    // --- showForm ---

    @Test
    public void showForm_withAppointmentBlockId_populatesModel() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("appointmentBlockId", "1");
        ModelMap model = new ModelMap();

        controller.showForm(model, request);

        Assert.assertTrue(model.containsKey("appointmentBlock"));
        Assert.assertTrue(model.containsKey("timeSlotLength"));
    }

    @Test
    public void showForm_withRedirectedFromCalendar_addsRedirectedFromToModel() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("redirectedFrom", "appointmentBlockCalendar.list");
        ModelMap model = new ModelMap();

        controller.showForm(model, request);

        Assert.assertEquals("appointmentBlockCalendar.list", model.get("redirectedFrom"));
    }

    @Test
    public void showForm_noParams_modelRemainsEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap model = new ModelMap();

        controller.showForm(model, request);

        Assert.assertFalse(model.containsKey("redirectedFrom"));
    }

    // --- onSubmit redirect whitelist (tests the open-redirect fix) ---

    @Test
    public void onSubmit_redirectedFromCalendar_redirectsToCalendar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());

        AppointmentBlock block = new AppointmentBlock();
        BindingResult result = new BeanPropertyBindingResult(block, "appointmentBlock");

        String view = controller.onSubmit(request, new ModelMap(), block, result,
                "30", "no", "appointmentBlockCalendar.list", null);

        Assert.assertEquals("redirect:appointmentBlockCalendar.list", view);
    }

    @Test
    public void onSubmit_redirectedFromList_redirectsToList() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());

        AppointmentBlock block = new AppointmentBlock();
        BindingResult result = new BeanPropertyBindingResult(block, "appointmentBlock");

        String view = controller.onSubmit(request, new ModelMap(), block, result,
                "30", "no", "appointmentBlockList.list", null);

        Assert.assertEquals("redirect:appointmentBlockList.list", view);
    }

    // Note: tests for the open-redirect whitelist fix (blocking "https://evil.com" and null)
    // live on the fix/open-redirect-appointmentblock branch alongside the fix itself.
}
