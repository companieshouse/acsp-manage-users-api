package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;

class UserAcspMembershipInternalTest {

  private UserAcspMembershipInternal controller;

  @BeforeEach
  void setUp() {
    controller = new UserAcspMembershipInternal();
  }

  @Test
  void testAddAcspOwner() {
    String xRequestId = "testRequestId";
    String acspNumber = "ACSP123";
    InternalRequestBodyPost requestBody = new InternalRequestBodyPost();

    ResponseEntity<ResponseBodyPost> response =
        controller.addAcspOwner(xRequestId, acspNumber, requestBody);

    assertNull(response, "Response should be null as the method is not yet implemented");
  }

  @Test
  void testIsActiveMember() {
    String xRequestId = "testRequestId";
    String acspNumber = "ACSP123";
    String userEmail = "test@example.com";

    ResponseEntity<Boolean> response = controller.isActiveMember(xRequestId, acspNumber, userEmail);

    assertNull(response, "Response should be null as the method is not yet implemented");
  }

  @Test
  void testPerformActionOnAcsp() {
    String xRequestId = "testRequestId";
    String acspNumber = "ACSP123";
    InternalRequestBodyPatch requestBody = new InternalRequestBodyPatch();

    ResponseEntity<Void> response =
        controller.performActionOnAcsp(xRequestId, acspNumber, requestBody);

    assertNull(response, "Response should be null as the method is not yet implemented");
  }
}
