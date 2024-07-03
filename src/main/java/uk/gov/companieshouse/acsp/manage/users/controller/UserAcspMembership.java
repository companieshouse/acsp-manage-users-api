package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.UserRoleMapperUtil;
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
  private final UsersService usersService;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public UserAcspMembership(
      final AcspMembersService acspMembersService, final UsersService usersService) {
    this.acspMembersService = acspMembersService;
    this.usersService = usersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspMember(
      String xRequestId, String inviteeUserId, RequestBodyPost requestBodyPost) {
    final String userId = requestBodyPost.getUserId();
    final AcspMembership.UserRoleEnum requestingRole =
        UserRoleMapperUtil.mapToUserRoleEnum(requestBodyPost.getUserRole());

    if (!usersService.doesUserExist(userId)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    if (acspMembersService.fetchAcspMember(userId).isPresent()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    Optional<AcspMembersDao> inviteeMembership =
        acspMembersService.fetchAcspMemberByUserIdAndAcspNumber(
            inviteeUserId, requestBodyPost.getAcspNumber());

    if (inviteeMembership.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    final AcspMembership.UserRoleEnum inviteeRole = inviteeMembership.get().getUserRole();
    if (!UserRoleMapperUtil.hasPermissionToAddUser(requestingRole, inviteeRole)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    AcspMembersDao addedAcspMembership =
        acspMembersService.addAcspMember(requestBodyPost, inviteeUserId);
    ResponseBodyPost responseBodyPost = new ResponseBodyPost();
    responseBodyPost.setAcspMembershipId(addedAcspMembership.getId());

    return new ResponseEntity<>(responseBodyPost, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<AcspMembership> getAcspMembershipForAcspId(
      @NotNull String xRequestId, @Pattern(regexp = "^[a-zA-Z0-9]*$") String id) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1146)
    }

  @Override
  public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
      final String xRequestId, final String ericIdentity, final Boolean includeRemoved) {
    LOG.info(
        String.format(
            "Received request for GET `/acsp-members` with X-Request-Id: %s, ERIC-Identity: %s, includeRemoved: %s",
            xRequestId, ericIdentity, includeRemoved));
    final List<AcspMembership> memberships =
        acspMembersService.fetchAcspMemberships(
            Objects.requireNonNull(UserContext.getLoggedUser()), includeRemoved);
    LOG.info(
        String.format(
            "X-Request-Id: %s, Fetched %d memberships for user ID: %s",
            xRequestId, memberships.size(), ericIdentity));
    return new ResponseEntity<>(memberships, HttpStatus.OK);
  }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForId(
            @NotNull String xRequestId,
            @Pattern(regexp = "^[a-zA-Z0-9]*$") String id,
            @Valid RequestBodyPatch requestBodyPatch
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1147)
    }

}

