package uk.gov.companieshouse.acsp.manage.users.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

class UserRoleMapperUtilTest {

  @ParameterizedTest
  @CsvSource({
    "OWNER, OWNER, true",
    "OWNER, ADMIN, true",
    "OWNER, STANDARD, true",
    "ADMIN, OWNER, false",
    "ADMIN, ADMIN, true",
    "ADMIN, STANDARD, true",
    "STANDARD, OWNER, false",
    "STANDARD, ADMIN, false",
    "STANDARD, STANDARD, false"
  })
  void testHasPermissionToAddUserAcspMembershipRoles(
      AcspMembership.UserRoleEnum requestingUserRole,
      AcspMembership.UserRoleEnum inviteeUserRole,
      boolean expectedResult) {
    boolean result = UserRoleMapperUtil.hasPermissionToAddUser(requestingUserRole, inviteeUserRole);
    assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @CsvSource({
    "OWNER, OWNER, true",
    "OWNER, ADMIN, true",
    "OWNER, STANDARD, true",
    "ADMIN, OWNER, false",
    "ADMIN, ADMIN, true",
    "ADMIN, STANDARD, true",
    "STANDARD, OWNER, false",
    "STANDARD, ADMIN, false",
    "STANDARD, STANDARD, false"
  })
  void testHasPermissionToAddUserMixedRolesAcspRequestBody(
      AcspMembership.UserRoleEnum requestingUserRole,
      RequestBodyPost.UserRoleEnum inviteeUserRole,
      boolean expectedResult) {
    boolean result = UserRoleMapperUtil.hasPermissionToAddUser(requestingUserRole, inviteeUserRole);
    assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @CsvSource({
    "OWNER, OWNER, true",
    "OWNER, ADMIN, true",
    "OWNER, STANDARD, true",
    "ADMIN, OWNER, false",
    "ADMIN, ADMIN, true",
    "ADMIN, STANDARD, true",
    "STANDARD, OWNER, false",
    "STANDARD, ADMIN, false",
    "STANDARD, STANDARD, false"
  })
  void testHasPermissionToAddUserMixedRolesRequestBodyAcsp(
      RequestBodyPost.UserRoleEnum requestingUserRole,
      AcspMembership.UserRoleEnum inviteeUserRole,
      boolean expectedResult) {
    boolean result = UserRoleMapperUtil.hasPermissionToAddUser(requestingUserRole, inviteeUserRole);
    assertEquals(expectedResult, result);
  }
}
