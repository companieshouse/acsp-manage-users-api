package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.UserRoleMapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.ActionEnum;
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

  private static final String ACSP_NUMBER_KEY = "acspNumber";
  private static final String REQUESTING_USER_ID_KEY = "requestingUserId";
  private static final String INVITEE_USER_ID_KEY = "inviteeUserId";

  private final AcspMembersService acspMembersService;
  private final AcspDataService acspDataService;
  private final AcspMembershipService acspMembershipService;
  private final UsersService usersService;

  public UserAcspMembership(
      final AcspMembersService acspMembersService,
      final AcspDataService acspDataService,
      final AcspMembershipService acspMembershipService,
      final UsersService usersService) {
    this.acspMembersService = acspMembersService;
    this.acspDataService = acspDataService;
    this.acspMembershipService = acspMembershipService;
    this.usersService = usersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspMember(
      final String xRequestId,
      final String requestingUserId,
      final RequestBodyPost requestBodyPost) {
    LOG.info(
        String.format(
            "Received request for POST `/acsp-members` with X-Request-Id: %s, requesting user ID: %s, and ACSP number: %s",
            xRequestId, requestingUserId, requestBodyPost.getAcspNumber()));

    var acspNumber = requestBodyPost.getAcspNumber();
    Optional<AcspMembersDao> requestingUserMembership =
        acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(requestingUserId, acspNumber);

    if (requestingUserMembership.isEmpty()) {
      LOG.infoContext(
          xRequestId,
          "Requesting user is not an active ACSP member",
          new HashMap<>(
              Map.of(REQUESTING_USER_ID_KEY, requestingUserId, ACSP_NUMBER_KEY, acspNumber)));
      throw new BadRequestRuntimeException("Requesting user is not an active ACSP member");
    }

    var acspData = acspDataService.fetchAcspData(acspNumber);
    if ("deauthorised".equals(acspData.getAcspStatus())) {
      LOG.infoContext(
          xRequestId,
          "ACSP is currently deauthorised, cannot add users",
          new HashMap<>(
              Map.of(REQUESTING_USER_ID_KEY, requestingUserId, ACSP_NUMBER_KEY, acspNumber)));
      throw new BadRequestRuntimeException("ACSP is currently deauthorised, cannot add users");
    }

    final var inviteeUserId = requestBodyPost.getUserId();
    LOG.debugContext(
        xRequestId,
        "Checking if invitee user exists",
        new HashMap<>(Map.of(INVITEE_USER_ID_KEY, inviteeUserId)));

    if (!usersService.doesUserExist(inviteeUserId)) {
      LOG.infoContext(
          xRequestId,
          "Invitee user does not exist",
          new HashMap<>(Map.of(INVITEE_USER_ID_KEY, inviteeUserId)));
      throw new NotFoundRuntimeException(
          StaticPropertyUtil.APPLICATION_NAMESPACE, "Invitee user does not exist");
    }

    LOG.debugContext(
        xRequestId,
        "Checking if invitee is already an active ACSP member",
        new HashMap<>(Map.of(INVITEE_USER_ID_KEY, inviteeUserId)));
    if (acspMembersService.fetchActiveAcspMember(inviteeUserId).isPresent()) {
      LOG.infoContext(
          xRequestId,
          "Invitee is already an active ACSP member",
          new HashMap<>(Map.of(INVITEE_USER_ID_KEY, inviteeUserId)));
      throw new BadRequestRuntimeException("Invitee is already an active ACSP member");
    }

    final var requestingUserRole = requestingUserMembership.get().getUserRole();
    LOG.debugContext(
        xRequestId,
        "Checking if requesting user has permission to add user",
        new HashMap<>(
            Map.of(
                "requestingUserRole",
                requestingUserRole,
                "requestedUserRole",
                requestBodyPost.getUserRole())));

    if (!UserRoleMapperUtil.hasPermissionToAddUser(
        requestingUserRole, requestBodyPost.getUserRole())) {
      LOG.infoContext(
          xRequestId,
          "Requesting user does not have permission to add user with specified role",
          new HashMap<>(
              Map.of(
                  "requestingUserRole",
                  requestingUserRole,
                  "requestedUserRole",
                  requestBodyPost.getUserRole())));
      throw new BadRequestRuntimeException(
          "Requesting user does not have permission to add user with specified role");
    }

    LOG.infoContext(
        xRequestId,
        "Adding new ACSP member",
        new HashMap<>(
            Map.of(
                INVITEE_USER_ID_KEY,
                inviteeUserId,
                ACSP_NUMBER_KEY,
                requestBodyPost.getAcspNumber(),
                "userRole",
                requestBodyPost.getUserRole())));

    final AcspMembersDao addedAcspMembership =
        acspMembersService.addAcspMember(requestBodyPost, inviteeUserId);

    var responseBodyPost = new ResponseBodyPost();
    responseBodyPost.setAcspMembershipId(addedAcspMembership.getId());

    LOG.infoContext(
        xRequestId,
        "Successfully added new ACSP member",
        new HashMap<>(
            Map.of(
                "acspMembershipId",
                addedAcspMembership.getId(),
                INVITEE_USER_ID_KEY,
                inviteeUserId)));

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

  private void toBadRequestWhenActionIsNotPermitted( final String xRequestId, final AcspMembersDao requestingAcspMember, final AcspMembersDao targetAcspMembership ){
    final String requestingUserId = requestingAcspMember.getUserId();
    final UserRoleEnum requestingUserRole = requestingAcspMember.getUserRole();
    final String targetUserId = targetAcspMembership.getUserId();
    final UserRoleEnum targetUserRole = targetAcspMembership.getUserRole();

    if ( requestingUserRole.equals( UserRoleEnum.STANDARD ) ){
      LOG.error( String.format( "%s: Requesting user %s has a standard role", xRequestId, requestingUserId ) );
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }

    if ( requestingUserRole.equals( UserRoleEnum.ADMIN ) && targetUserRole.equals( UserRoleEnum.OWNER ) ){
      LOG.error( String.format( "%s: Requesting user %s has an admin role, and target user %s has an owner role", xRequestId, requestingUserId, targetUserId ) );
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }

    if ( targetUserRole.equals( UserRoleEnum.OWNER ) && ( targetUserId.equals( requestingUserId ) ) ){
      LOG.error( String.format( "%s: Requesting user %s is an owner and is attempting to update themselves", xRequestId, requestingUserId ) );
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }
  }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForId( final String xRequestId, final String targetAcspMemberId, final RequestBodyPatch requestBodyPatch ) {
      final var action = requestBodyPatch.getAction();
      final var targetUserNewRole = requestBodyPatch.getUserRole();

      LOG.infoContext( xRequestId, String.format( "Attempting to perform %s operation on AcspMember %s", action, targetAcspMemberId ), null );

      final var targetAcspMembership =
      acspMembersService.fetchAcspMembersDao( targetAcspMemberId )
                        .orElseThrow( () -> {
                            LOG.error( String.format( "%s: AcspMember %s does not exist", xRequestId, targetAcspMemberId ) );
                            return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                        } );

      final var requestingUserId = UserContext.getLoggedUser().getUserId();
      final var requestingAcspMember =
      acspMembersService.fetchAcspMembership( targetAcspMembership.getAcspNumber(), requestingUserId )
                        .orElseThrow( () -> {
                            LOG.error( String.format( "%s: AcspMember where user_id=%s and acsp_number=%s does not exist", xRequestId, requestingUserId, targetAcspMembership.getAcspNumber() ) );
                            return new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                        } );

      toBadRequestWhenActionIsNotPermitted( xRequestId, requestingAcspMember, targetAcspMembership );

      if ( action.equals( ActionEnum.EDIT_ROLE ) ){
        Optional.ofNullable( targetUserNewRole )
                .filter( role -> !role.equals( RequestBodyPatch.UserRoleEnum.OWNER ) )
                .orElseThrow( () -> {
                    LOG.error( String.format( "%s: role is null or owner", xRequestId ) );
                    return new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                } );
        acspMembersService.updateRole( targetAcspMemberId, targetUserNewRole.getValue() );
      }

      if ( action.equals( ActionEnum.REMOVE_MEMBER ) ){
        acspMembersService.removeMember( targetAcspMemberId, requestingUserId );
      }

      return new ResponseEntity<>( HttpStatus.OK );
    }

}

