package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_ADMIN_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_ADMINS;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_OWNERS;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_READ_PERMISSION;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_STANDARD;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_NUMBER_PATTERN;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_OWNER_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_SEARCH_ADMIN_SEARCH;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_STANDARD_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import uk.gov.companieshouse.acsp.manage.users.model.Constants;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDataContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDetails;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


public final class RequestUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

  private RequestUtil(){}

  public static boolean isOAuth2Request() {
    return getRequestDetails().getEricIdentityType().equals("oauth2");
  }

  public static boolean isKeyRequest() {
    return getRequestDetails().getEricIdentityType().equals("key");
  }

  private static RequestDetails getRequestDetails() {
    return RequestDataContext.getInstance().getRequestDetails();
  }

  public static boolean ericAuthorisedTokenPermissionsAreValid( final AcspMembersService acspMembersService, final String userId ) {
    final var acspNumber = fetchRequestingUsersActiveAcspNumber();
    final var requestingUsersActiveMembershipOptional = acspMembersService.fetchActiveAcspMembership(userId, acspNumber);
    if ( requestingUsersActiveMembershipOptional.isEmpty() ) {
      return false;
    }
    final var requestingUsersActiveMembership = requestingUsersActiveMembershipOptional.get();

    final var currentUserRole = UserRoleEnum.fromValue( requestingUsersActiveMembership.getUserRole() );
    final var sessionUserRole = fetchRequestingUsersAcspRole();

    if ( currentUserRole.equals( sessionUserRole ) ){
      LOGGER.debugContext( getXRequestId(), "Confirmed that session is in sync with the database.", null );
      return true;
    }
    LOGGER.errorContext( getXRequestId(), new Exception( "Confirmed that session is out of sync with the database." ), null );
    return false;
  }

  public static String fetchRequestingUsersActiveAcspNumber() {
    final var ericAuthorisedTokenPermissions = getRequestDetails().getEricAuthorisedTokenPermissions();
    final var matcher = ACSP_NUMBER_PATTERN.matcher(ericAuthorisedTokenPermissions);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static boolean hasPermission( final String permission ) {
    return Optional.ofNullable(getRequestDetails())
      .map(RequestDetails::getEricAuthorisedTokenPermissions)
      .map(roles -> roles.split(" "))
      .map(Arrays::asList)
      .orElse(List.of())
      .contains(permission);
  }

  public static String getXRequestId() {
    return getRequestDetails().getXRequestId();
  }

  public static String fetchRequestingUsersSpringRole() {
    final var ericAuthorisedTokenPermissions = getRequestDetails().getEricAuthorisedTokenPermissions();
    if ( isKeyRequest() ) {
      return KEY_ROLE;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_OWNERS) ) {
      return ACSP_OWNER_ROLE;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_ADMINS) ) {
      return ACSP_ADMIN_ROLE;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_READ_PERMISSION) ) {
      return ACSP_STANDARD_ROLE;
    }
    if ( getRequestDetails().getEricAuthorisedRoles().contains(ACSP_SEARCH_ADMIN_SEARCH) ) {
      return ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
    }
    return BASIC_OAUTH_ROLE;
  }

  public static UserRoleEnum fetchRequestingUsersAcspRole() {
    final var ericAuthorisedTokenPermissions = getRequestDetails().getEricAuthorisedTokenPermissions();
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_OWNERS) ) {
      return UserRoleEnum.OWNER;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_ADMINS) ) {
      return UserRoleEnum.ADMIN;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_READ_PERMISSION) ) {
      return UserRoleEnum.STANDARD;
    }
    return null;
  }



  public static String getEricIdentity() {
    return getRequestDetails().getEricIdentity();
  }

  public static boolean requestingUserIsPermittedToRetrieveAcspData() {
    if ( getRequestDetails().getEricAuthorisedTokenPermissions().contains( Constants.ACSP_MEMBERS_READ ) ){
      LOGGER.debugContext( getXRequestId(), "Confirmed that user has read permission.", null );
      return true;
    }
    LOGGER.errorContext( getXRequestId(), new Exception( "Confirmed user does not have read permission." ), null );
    return false;

  }

  public static boolean requestingUserIsActiveMemberOfAcsp( final String acspNumber ){
    return acspNumber.equals( fetchRequestingUsersActiveAcspNumber() );
  }

  public static boolean requestingUserCanManageMembership( final UserRoleEnum role ){
    return switch ( role ){
      case UserRoleEnum.OWNER -> hasPermission( ACSP_MEMBERS_OWNERS );
      case UserRoleEnum.ADMIN -> hasPermission( ACSP_MEMBERS_ADMINS );
      case UserRoleEnum.STANDARD -> hasPermission( ACSP_MEMBERS_STANDARD );
    };
  }

}
