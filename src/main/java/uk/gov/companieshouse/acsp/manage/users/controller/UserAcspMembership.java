package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class UserAcspMembership implements UserAcspMembershipInterface {

  private final AcspMembersService acspMembersService;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public UserAcspMembership(final AcspMembersService acspMembersService) {
    this.acspMembersService = acspMembersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspMember(
      @NotNull String xRequestId,
      @NotNull String ericIdentity,
      @Valid RequestBodyPost requestBodyPost) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1149)
  }

  @Override
  public ResponseEntity<AcspMembership> getAcspMembershipForAcspId(
      @NotNull String xRequestId, @Pattern(regexp = "^[a-zA-Z0-9]*$") String id) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1146)
  }

  @Override
  public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
      final String xRequestId, final String ericIdentity, final Boolean shouldIncludeRemoved) {
    LOG.info(
        String.format(
            "Received request for GET `/acsp-members` with X-Request-Id: %s, ERIC-Identity: %s, includeRemoved: %s",
            xRequestId, ericIdentity, shouldIncludeRemoved));
    final boolean includeRemoved = Objects.nonNull(shouldIncludeRemoved) && shouldIncludeRemoved;
    List<AcspMembership> memberships =
        acspMembersService.fetchAcspMemberships(UserContext.getLoggedUser(), includeRemoved);
    LOG.debug(
        String.format("Fetched %d memberships for user ID: %s", memberships.size(), ericIdentity));
    return new ResponseEntity<>(memberships, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> updateAcspMembershipForId(
      @NotNull String xRequestId,
      @Pattern(regexp = "^[a-zA-Z0-9]*$") String id,
      @Valid RequestBodyPatch requestBodyPatch) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1147)
  }
}
