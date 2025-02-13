package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_ADMINS;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_OWNERS;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_MEMBERS_READ_PERMISSION;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_NUMBER_PATTERN;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_SEARCH_ADMIN_SEARCH;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY_ROLE;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import uk.gov.companieshouse.acsp.manage.users.model.Constants;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDataContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDetails;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;


public final class RequestUtil {


  public static boolean isOAuth2Request() {
    return getRequestDetails().getEricIdentityType().equals("oauth2");
  }

  public static boolean isKeyRequest() {
    return getRequestDetails().getEricIdentityType().equals("key");
  }

  private static RequestDetails getRequestDetails() {
    return RequestDataContext.getInstance().getRequestDetails();
  }

  public static boolean hasAdminAcspSearchPermission() {
    return getRequestDetails().getEricAuthorisedRoles().contains(ACSP_SEARCH_ADMIN_SEARCH);
  }

  public static boolean ericAuthorisedTokenPermissionsAreValid( final AcspMembersService acspMembersService, final String userId ) {
    final var acspNumber = fetchRequestingUsersActiveAcspNumber();
    final var requestingUsersActiveMembershipOptional = acspMembersService.fetchActiveAcspMembership(userId, acspNumber);
    if ( requestingUsersActiveMembershipOptional.isEmpty() ) {
      return false;
    }
    final var requestingUsersActiveMembership = requestingUsersActiveMembershipOptional.get();

    final var currentUserRole = requestingUsersActiveMembership.getUserRole();
    final var sessionUserRole = fetchRequestingUsersRole();
    return currentUserRole.equals(sessionUserRole);
  }

  public static String fetchRequestingUsersActiveAcspNumber() {
    final var ericAuthorisedTokenPermissions = getRequestDetails().getEricAuthorisedTokenPermissions();
    final var matcher = ACSP_NUMBER_PATTERN.matcher(ericAuthorisedTokenPermissions);
    return matcher.find() ? matcher.group(1) : null;
  }

  public static boolean hasPermission( final String permission ) {
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

  public static String fetchRequestingUsersRole() {
    final var ericAuthorisedTokenPermissions = getRequestDetails().getEricAuthorisedTokenPermissions();
    if ( isKeyRequest() ) {
      return KEY_ROLE;
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_OWNERS) ) {
      return UserRoleEnum.OWNER.getValue();
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_ADMINS) ) {
      return UserRoleEnum.ADMIN.getValue();
    }
    if ( ericAuthorisedTokenPermissions.contains(ACSP_MEMBERS_READ_PERMISSION) ) {
      return UserRoleEnum.STANDARD.getValue();
    }
    if ( getRequestDetails().getEricAuthorisedRoles().contains(ACSP_SEARCH_ADMIN_SEARCH) ) {
      return ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
    }
    return null;

  }

  public static String getEricIdentity() {
    return getRequestDetails().getEricIdentity();
  }

  public static boolean requestingUserIsPermittedToRetrieveAcspData() {
    return getRequestDetails().getEricAuthorisedTokenPermissions().contains(Constants.ACSP_MEMBERS_READ);
  }
}
