package org.openmrs.module.appointmentscheduling.rest.resource.openmrs1_9;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResourceTest;

public class AppointmentResource1_9Test extends BaseDelegatingResourceTest<AppointmentResource1_9, Appointment> {
	
	@Before
	public void setup() throws Exception {
		executeDataSet("standardWebAppointmentTestDataset.xml");
	}
	
	@Override
	public Appointment newObject() {
		return Context.getService(AppointmentService.class).getAppointmentByUuid(getUuidProperty());
	}
	
	@Override
	public String getDisplayProperty() {
		return "Initial HIV Clinic Appointment : Scheduled";
	}
	
	@Override
	public String getUuidProperty() {
		return "c0c579b0-8e59-401d-8a4a-976a0b183601";
	}
	
	@Test
	public void enforceProviderAcl_superuserReturnsRequestedProvider() {
		User admin = Context.getAuthenticatedUser(); // admin is superuser
		Provider anyProvider = Context.getProviderService()
				.getProviderByUuid("c0c549b0-8e59-401d-8a4a-976a0b183599");
		AppointmentResource1_9 resource = new AppointmentResource1_9();
		Provider result = resource.enforceProviderAcl(admin, anyProvider);
		Assert.assertEquals(anyProvider, result);
	}

	@Test
	public void enforceProviderAcl_nonSuperuserWithProviderReturnsOwnProvider() {
		// person_id=2 is linked to non-retired provider_id=2 in the test dataset
		Person personWithProvider = Context.getPersonService().getPerson(2);
		User nonSuperUser = new User(personWithProvider);

		AppointmentResource1_9 resource = new AppointmentResource1_9();
		Provider result = resource.enforceProviderAcl(nonSuperUser, null);

		Assert.assertNotNull(result);
		Assert.assertEquals("c0c54sd0-8e59-401d-8a4a-976a0b183599", result.getUuid());
	}

	@Test(expected = APIAuthenticationException.class)
	public void enforceProviderAcl_nonSuperuserRequestingOtherProviderThrows() {
		Person personWithProvider = Context.getPersonService().getPerson(2);
		User nonSuperUser = new User(personWithProvider);

		// provider_id=1 belongs to a different person than person_id=2
		Provider otherProvider = Context.getProviderService()
				.getProviderByUuid("c0c549b0-8e59-401d-8a4a-976a0b183599");

		new AppointmentResource1_9().enforceProviderAcl(nonSuperUser, otherProvider);
	}

	@Test(expected = APIAuthenticationException.class)
	public void enforceProviderAcl_nonSuperuserWithNoProviderThrows() {
		// person_id=1 is only linked to retired provider_id=3; includeRetired=false → empty
		Person personWithRetiredProvider = Context.getPersonService().getPerson(1);
		User nonSuperUser = new User(personWithRetiredProvider);

		new AppointmentResource1_9().enforceProviderAcl(nonSuperUser, null);
	}

	public void validateRefRepresentation() throws Exception {
		super.validateRefRepresentation();
		assertPropNotPresent("voided"); // note that the voided property is only present if the property is voided
	}
	
	@Override
	public void validateDefaultRepresentation() throws Exception {
		super.validateDefaultRepresentation();
		assertPropEquals("status", getObject().getStatus());
		assertPropEquals("reason", getObject().getReason());
		assertPropEquals("voided", getObject().isVoided());
		assertPropPresent("visit");
		assertPropPresent("patient");
		assertPropPresent("appointmentType");
		assertPropNotPresent("auditInfo");
	}
	
	@Override
	public void validateFullRepresentation() throws Exception {
		super.validateFullRepresentation();
		assertPropEquals("status", getObject().getStatus());
		assertPropEquals("reason", getObject().getReason());
		assertPropEquals("voided", getObject().isVoided());
		assertPropPresent("visit");
		assertPropPresent("patient");
		assertPropPresent("appointmentType");
		assertPropPresent("auditInfo");
	}
}
