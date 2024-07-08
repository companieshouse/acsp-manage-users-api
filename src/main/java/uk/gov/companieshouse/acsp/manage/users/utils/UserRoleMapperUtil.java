package uk.gov.companieshouse.acsp.manage.users.utils;

import java.util.EnumMap;
import java.util.Map;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

public class UserRoleMapperUtil {

  private UserRoleMapperUtil(
      // Empty constructor as only static methods in class
      ) {}

  private static final Map<AcspMembership.UserRoleEnum, RequestBodyPost.UserRoleEnum>
      acspToRequestBodyMap = new EnumMap<>(AcspMembership.UserRoleEnum.class);
  private static final Map<RequestBodyPost.UserRoleEnum, AcspMembership.UserRoleEnum>
      requestBodyToAcspMap = new EnumMap<>(RequestBodyPost.UserRoleEnum.class);

  static {
    acspToRequestBodyMap.put(AcspMembership.UserRoleEnum.OWNER, RequestBodyPost.UserRoleEnum.OWNER);
    acspToRequestBodyMap.put(AcspMembership.UserRoleEnum.ADMIN, RequestBodyPost.UserRoleEnum.ADMIN);
    acspToRequestBodyMap.put(
        AcspMembership.UserRoleEnum.STANDARD, RequestBodyPost.UserRoleEnum.STANDARD);

    requestBodyToAcspMap.put(RequestBodyPost.UserRoleEnum.OWNER, AcspMembership.UserRoleEnum.OWNER);
    requestBodyToAcspMap.put(RequestBodyPost.UserRoleEnum.ADMIN, AcspMembership.UserRoleEnum.ADMIN);
    requestBodyToAcspMap.put(
        RequestBodyPost.UserRoleEnum.STANDARD, AcspMembership.UserRoleEnum.STANDARD);
  }

  public static AcspMembership.UserRoleEnum mapToUserRoleEnum(
      RequestBodyPost.UserRoleEnum requestBodyPostUserRole) {
    return requestBodyToAcspMap.get(requestBodyPostUserRole);
  }

  public static boolean hasPermissionToAddUser(
      AcspMembership.UserRoleEnum requestingUserRole, AcspMembership.UserRoleEnum inviteeUserRole) {
    if (requestingUserRole == AcspMembership.UserRoleEnum.OWNER) {
      return true;
    } else if (requestingUserRole == AcspMembership.UserRoleEnum.ADMIN) {
      return inviteeUserRole != AcspMembership.UserRoleEnum.OWNER;
    } else {
      return false;
    }
  }

  public static boolean hasPermissionToAddUser(
      RequestBodyPost.UserRoleEnum requestingUserRole,
      AcspMembership.UserRoleEnum inviteeUserRole) {
    return hasPermissionToAddUser(mapToUserRoleEnum(requestingUserRole), inviteeUserRole);
  }

  public static boolean hasPermissionToAddUser(
      AcspMembership.UserRoleEnum requestingUserRole,
      RequestBodyPost.UserRoleEnum inviteeUserRole) {
    return hasPermissionToAddUser(requestingUserRole, mapToUserRoleEnum(inviteeUserRole));
  }
}
