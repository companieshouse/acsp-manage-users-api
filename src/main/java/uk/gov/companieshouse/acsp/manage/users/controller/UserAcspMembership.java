package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
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

  private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN =
      "Please check the request and try again";
  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
  private final AcspMembersService acspMembersService;
  private final AcspMembershipService acspMembershipService;
  private final UsersService usersService;

  public UserAcspMembership(
      final AcspMembersService acspMembersService,
      final AcspMembershipService acspMembershipService,
      final UsersService usersService) {
    this.acspMembersService = acspMembersService;
    this.acspMembershipService = acspMembershipService;
    this.usersService = usersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspMember(
      final String xRequestId,
      final String requestingUserId,
      final RequestBodyPost requestBodyPost) {
    Optional<AcspMembersDao> requestingUserMembership =
        acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
            requestingUserId, requestBodyPost.getAcspNumber());
    if (requestingUserMembership.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    final var inviteeUserId = requestBodyPost.getUserId();
    final var inviteeUserRole = UserRoleMapperUtil.mapToUserRoleEnum(requestBodyPost.getUserRole());

    if (!usersService.doesUserExist(inviteeUserId)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    if (acspMembersService.fetchActiveAcspMember(inviteeUserId).isPresent()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    final var requestingUserRole = requestingUserMembership.get().getUserRole();
    if (!UserRoleMapperUtil.hasPermissionToAddUser(requestingUserRole, inviteeUserRole)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    final AcspMembersDao addedAcspMembership =
        acspMembersService.addAcspMember(requestBodyPost, inviteeUserId);
    var responseBodyPost = new ResponseBodyPost();
    responseBodyPost.setAcspMembershipId(addedAcspMembership.getId());

    return new ResponseEntity<>(responseBodyPost, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<AcspMembership> getAcspMembershipForAcspId(
      final String xRequestId, final String id) {
    LOG.info(
        String.format(
            "Received request for GET `/acsp-members` with X-Request-Id: %s and membership id %s",
            xRequestId, id));
    if (Objects.isNull(id)) {
      LOG.error(String.format("%s: No membership id was provided.", xRequestId));
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    final var association = acspMembershipService.fetchAcspMembership(id);
    if (association.isEmpty()) {
      final var errorMessage = String.format("Cannot find Association for the Id: %s", id);
      LOG.error(errorMessage);
      throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
    }
    return new ResponseEntity<>(association.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
      final String xRequestId, final String ericIdentity, final Boolean includeRemoved) {
    LOG.info(
        String.format(
            "Received request for GET `/acsp-members` with X-Request-Id: %s, ERIC-Identity: %s, includeRemoved: %s",
            xRequestId, ericIdentity, includeRemoved));
    List<AcspMembership> memberships =
        acspMembersService.fetchAcspMemberships(UserContext.getLoggedUser(), includeRemoved);
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
      @Valid RequestBodyPatch requestBodyPatch) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1147)
  }
}
