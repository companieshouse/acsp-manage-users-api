package uk.gov.companieshouse.acsp.manage.users.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

@RestController
public class UserAcspMembershipController implements UserAcspMembershipInterface {

  private final AcspMembersService acspMembersService;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public UserAcspMembershipController(final AcspMembersService acspMembersService) {
    this.acspMembersService = acspMembersService;
  }

  @Override
  public ResponseEntity<AcspMembershipsList> getAcspMembershipsForUserId(
      final String xRequestId, final String ericIdentity, final Boolean includeRemoved) {
    LOG.info(
        String.format(
            "Received request for GET `/memberships` with X-Request-Id: %s, ERIC-Identity: %s, includeRemoved: %s",
            xRequestId, ericIdentity, includeRemoved));
    final var loggedUser = UserContext.getLoggedUser();
    final var acspMemberships =
        acspMembersService.fetchAcspMemberships(Objects.requireNonNull(loggedUser), includeRemoved);
    LOG.infoContext(
        xRequestId,
        String.format(
            "Successfully fetched ACSP memberships for the user with ID %s",
            loggedUser.getUserId()),
        null);
    return new ResponseEntity<>(acspMemberships, HttpStatus.OK);
  }
}
