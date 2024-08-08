package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.*;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;

@Controller
public class AcspMembershipsController implements AcspMembershipsInterface {

  private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

  private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
  private static final String ACSP_NUMBER_KEY = "acspNumber";
  private static final String REQUEST_ID_KEY = "requestId";

  private final UsersService usersService;
  private final AcspDataService acspDataService;
  private final AcspMembersService acspMembersService;

  public AcspMembershipsController( final UsersService usersService, final AcspDataService acspDataService, final AcspMembersService acspMembersService ) {
    this.usersService = usersService;
    this.acspDataService = acspDataService;
    this.acspMembersService = acspMembersService;
  }

  private void throwBadRequestWhenActionIsNotPermittedByOAuth2User( final String requestingUserId, final String acspNumber, final UserRoleEnum role ) {
    final var requestingUserMembership =
            acspMembersService.fetchActiveAcspMembership( requestingUserId, acspNumber )
                    .orElseThrow( () -> {
                      LOG.error( "Requesting user is not a member of the Acsp" );
                      return new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                    } );

    if ( UserRoleEnum.STANDARD.getValue().equals( requestingUserMembership.getUserRole() ) ){
      LOG.error( "Standard user is not permitted to create membership" );
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }

    if ( UserRoleEnum.ADMIN.getValue().equals( requestingUserMembership.getUserRole() ) && UserRoleEnum.OWNER.equals( role ) ){
      LOG.error( "Admin user is not permitted to create owner membership" );
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }

  }

  @Override
  public ResponseEntity<AcspMembership> addMemberForAcsp( final String xRequestId, final String acspNumber, final RequestBodyPost requestBodyPost ) {
    final var targetUserId = requestBodyPost.getUserId();
    final var targetUserRole = AcspMembership.UserRoleEnum.fromValue( requestBodyPost.getUserRole().getValue() );

    LOG.infoContext( xRequestId, String.format( "Attempting to create %s membership for user %s at Acsp %s", targetUserRole.getValue(), targetUserId, acspNumber ), null );

    User targetUser;
    try {
      targetUser = usersService.fetchUserDetails( targetUserId );
    } catch ( NotFoundRuntimeException exception ) {
      throw new BadRequestRuntimeException( ERROR_CODE_1001.getCode() );
    }

    AcspDataDao acspDataDao;
    try {
      acspDataDao = acspDataService.fetchAcspData( acspNumber );
    } catch ( NotFoundRuntimeException exception ) {
      throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
    }

    final var memberships = acspMembersService.fetchAcspMembershipDaos( targetUserId, false );
    if ( !memberships.isEmpty() ) {
      LOG.error( "User already has an active Acsp membership" );
      throw new BadRequestRuntimeException( ERROR_CODE_1002.getCode() );
    }

    final var requestingUserId = Optional.ofNullable( UserContext.getLoggedUser() ).map( User::getUserId ).orElse( null );
    if ( isOAuth2Request() ){
      throwBadRequestWhenActionIsNotPermittedByOAuth2User( requestingUserId, acspNumber, targetUserRole );
    }

    final var membership = acspMembersService.addAcspMembership( targetUser, acspDataDao, acspNumber, targetUserRole, requestingUserId );

    LOG.infoContext( xRequestId, String.format( "Successfully created %s membership for user %s at Acsp %s", targetUserRole.getValue(), targetUserId, acspNumber ), null );

    return new ResponseEntity<>( membership, HttpStatus.CREATED );
  }


  @Override
  public ResponseEntity<AcspMembershipsList> findMembershipsForUserAndAcsp(
      final String requestId,
      final String acspNumber,
      final Boolean includeRemoved,
      final RequestBodyLookup requestBody) {
    Map<String, Object> logMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put("includeRemoved", includeRemoved);
    logMap.put("userEmail", requestBody.getUserEmail());
    LOG.info("Getting members for Acsp & User email", logMap);

    if (Objects.isNull(requestBody.getUserEmail())) {
      LOG.error(String.format("%s: A user email was not provided.", requestId));
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    final var userEmail = requestBody.getUserEmail();
    final var usersList =
        Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
            .filter(users -> !users.isEmpty())
            .orElseThrow(
                () -> {
                  LOG.error(String.format("User %s was not found", userEmail));
                  return new NotFoundRuntimeException(
                      StaticPropertyUtil.APPLICATION_NAMESPACE,
                      PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
                });
    final var user = usersList.getFirst();

    // This will probably be replaced by the ACSP Data Sync API once available.
    acspDataService.fetchAcspData(acspNumber); // can throw 404.

    final var acspMembershipsList =
        acspMembersService.fetchAcspMemberships(user, includeRemoved, acspNumber);

    Map<String, Object> successLogMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put("userEmail", userEmail);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    LOG.info("Getting members for Acsp & User email", successLogMap);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<AcspMembershipsList> getMembersForAcsp(
      final String acspNumber,
      final String requestId,
      final Boolean includeRemoved,
      final Integer pageIndex,
      final Integer itemsPerPage,
      final String role) {
    Map<String, Object> logMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put("includeRemoved", includeRemoved);
    logMap.put("pageIndex", pageIndex);
    logMap.put("itemsPerPage", itemsPerPage);
    logMap.put("role", role);
    LOG.info("Getting members for Acsp", logMap);

    final boolean roleIsValid =
        Optional.ofNullable(role)
            .map(
                theRole ->
                    Arrays.stream(AcspMembership.UserRoleEnum.values())
                        .map(AcspMembership.UserRoleEnum::getValue)
                        .anyMatch(validRole -> validRole.equals(theRole)))
            .orElse(true);

    if (!roleIsValid) {
      LOG.error(String.format("Role was invalid: %s", role));
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    final var paginationParams =
        PaginationValidatorUtil.validateAndGetParams(pageIndex, itemsPerPage);

    // This will probably be replaced by the ACSP Data Sync API once available.
    final var acspDataDao = acspDataService.fetchAcspData(acspNumber);

    final var acspMembershipsList =
        acspMembersService.findAllByAcspNumberAndRole(
            acspNumber,
            acspDataDao,
            role,
            includeRemoved,
            paginationParams.pageIndex,
            paginationParams.itemsPerPage);

    Map<String, Object> successLogMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put(
        "membersCount",
        Optional.ofNullable(acspMembershipsList.getItems()).map(List::size).orElse(0));
    LOG.info("Getting members for Acsp", successLogMap);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }
}
