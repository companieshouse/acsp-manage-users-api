package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canChangeRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canCreateMembership;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canRemoveMembership;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getActiveAcspRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getAdminPrivileges;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricAuthorisedKeyRoles;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestContextUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void getXRequestIdIsUnknownWhenXRequestIdIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertEquals( UNKNOWN, getXRequestId() );
    }

    @Test
    void getXRequestIdRetrievesXRequestId(){
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id","theId123");
        RequestContext.setRequestContext( new RequestContextDataBuilder().setXRequestId( request ).build() );
        Assertions.assertEquals( "theId123", getXRequestId() );
    }

    @Test
    void getEricIdentityIsUnknownWhenEricIdentityIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertEquals( UNKNOWN, getEricIdentity() );
    }

    @Test
    void getEricIdentityRetrievesEricIdentity(){
        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY,"COMU002" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).build() );
        Assertions.assertEquals( "COMU002", getEricIdentity() );
    }

    @Test
    void getEricIdentityTypeIsUnknownWhenEricIdentityTypeIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertEquals( UNKNOWN, getEricIdentityType() );
    }

    @Test
    void getEricIdentityTypeRetrievesEricIdentityType(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type","oauth2");
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentityType( request ).build() );
        Assertions.assertEquals( "oauth2", getEricIdentityType() );
    }

    @Test
    void getEricAuthorisedKeyRolesIsUnknownWhenEricAuthorisedKeyRolesIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertEquals( UNKNOWN, getEricAuthorisedKeyRoles() );
    }

    @Test
    void getEricAuthorisedKeyRolesRetrievesEricAuthorisedKeyRoles(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Key-Roles","*");
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricAuthorisedKeyRoles( request ).build() );
        Assertions.assertEquals( "*", getEricAuthorisedKeyRoles() );
    }

    static Stream<Arguments> getActiveAcspNumberTestData(){
        return Stream.of(
                Arguments.of( "", "unknown" ),
                Arguments.of( "acsp_number=TSA001", "TSA001" ),
                Arguments.of( "xacsp_number=TSA001", "unknown" ),
                Arguments.of( "acspx_number=TSA001", "unknown" ),
                Arguments.of( "acsp_numberx=TSA001", "unknown" ),
                Arguments.of( "acsp_number=$$$", "unknown" ),
                Arguments.of( "acsp_members_owner=create", "unknown" ),
                Arguments.of( "acsp_number=TSA001 acsp_members_owner=create", "TSA001" ),
                Arguments.of( "acsp_members_owner=create acsp_number=TSA001", "TSA001" ),
                Arguments.of( "acsp_members_owner=create acsp_number=TSA001 acsp_members_admin=create", "TSA001" )
        );
    }

    @ParameterizedTest
    @MethodSource( "getActiveAcspNumberTestData" )
    void getActiveAcspNumberRetrievesAcspNumberFromSession( final String ericAuthorisedTokenPermissions, final String expectedAcspNumber ){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspNumber( request ).build() );

        Assertions.assertEquals( expectedAcspNumber, getActiveAcspNumber() );
    }

    private static Stream<Arguments> getActiveAcspRoleScenarios(){
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        final var adminPermissions = testDataManager.fetchTokenPermissions( "COM004" );
        final var standardPermissions = testDataManager.fetchTokenPermissions( "COM007" );
        final var noPermissions = "";

        return Stream.of(
                Arguments.of( ownerPermissions, OWNER ),
                Arguments.of( adminPermissions, ADMIN ),
                Arguments.of( standardPermissions, STANDARD ),
                Arguments.of( noPermissions, null )
        );
    }

    @ParameterizedTest
    @MethodSource( "getActiveAcspRoleScenarios" )
    void getActiveAcspRoleTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum expectedOutcome ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspRole( request ).build() );

        Assertions.assertEquals( expectedOutcome, getActiveAcspRole() );
    }

    @Test
    void getAdminPrivilegesIsEmptySetWhenAdminPrivilegesIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertTrue( getAdminPrivileges().isEmpty() );
    }

    @Test
    void getAdminPrivilegesRetrievesAdminPrivileges(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Roles","/admin/acsp/search");
        RequestContext.setRequestContext( new RequestContextDataBuilder().setAdminPrivileges( request ).build() );
        Assertions.assertEquals( Set.of( "/admin/acsp/search" ), getAdminPrivileges() );
    }

    @Test
    void getUserNullWhenUserIsMissing(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertNull( getUser() );
    }

    @Test
    void getUserRetrievesUser(){
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();
        RequestContext.setRequestContext( new RequestContextDataBuilder().setUser( user ).build() );
        Assertions.assertEquals( user, getUser() );
    }

    @Test
    void isOAuth2RequestReturnsFalseWhenRequestTypeIsUnknown(){
        RequestContext.setRequestContext( new RequestContextDataBuilder().build() );
        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void isOAuth2RequestReturnsFalseWhenRequestTypeIsKey(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Identity-Type","key" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentityType( request ).build() );
        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void isOAuth2RequestReturnsTrueWhenRequestTypeIsOAuth2(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Identity-Type","oauth2" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentityType( request ).build() );
        Assertions.assertTrue( isOAuth2Request() );
    }

    @Test
    void isActiveMemberOfAcspWithNullReturnsFalse(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspNumber( request ).build() );
        Assertions.assertFalse( isActiveMemberOfAcsp( null ) );
    }

    @Test
    void isActiveMemberOfAcspReturnsFalseWhenSpecifiedAcspNumberDoesNotMatchSession(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspNumber( request ).build() );
        Assertions.assertFalse( isActiveMemberOfAcsp( "TSA001" ) );
    }

    @Test
    void isActiveMemberOfAcspReturnsTrueWhenSpecifiedAcspNumberMatchesSession(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspNumber( request ).build() );
        Assertions.assertTrue( isActiveMemberOfAcsp( "COMA001" ) );
    }




    @Test
    void canManageMembershipWithNullRoleThrowsNullPointerException(){
        final var request = new MockHttpServletRequest();
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ownerPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspRole( request ).build() );
        Assertions.assertThrows( NullPointerException.class, () -> canCreateMembership( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> canRemoveMembership( null ) );
    }

    private static Stream<Arguments> canManageMembershipScenarios(){
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        final var adminPermissions = testDataManager.fetchTokenPermissions( "COM004" );
        final var standardPermissions = testDataManager.fetchTokenPermissions( "COM007" );
        final var noPermissions = "";

        return Stream.of(
                Arguments.of( ownerPermissions, OWNER, true ),
                Arguments.of( ownerPermissions, ADMIN, true ),
                Arguments.of( ownerPermissions, STANDARD, true ),
                Arguments.of( adminPermissions, OWNER, false ),
                Arguments.of( adminPermissions, ADMIN, true ),
                Arguments.of( adminPermissions, STANDARD, true ),
                Arguments.of( standardPermissions, OWNER, false ),
                Arguments.of( standardPermissions, ADMIN, false ),
                Arguments.of( standardPermissions, STANDARD, false ),
                Arguments.of( noPermissions, OWNER, false ),
                Arguments.of( noPermissions, ADMIN, false ),
                Arguments.of( noPermissions, STANDARD, false )
        );
    }

    @ParameterizedTest
    @MethodSource( "canManageMembershipScenarios" )
    void canManageMembershipTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum targetRole, final boolean canManage ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspRole( request ).build() );
        Assertions.assertEquals( canManage, canCreateMembership( targetRole ) );
        Assertions.assertEquals( canManage, canRemoveMembership( targetRole ) );
    }

    private static Stream<Arguments> canChangeRoleScenarios(){
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        final var adminPermissions = testDataManager.fetchTokenPermissions( "COM004" );
        final var standardPermissions = testDataManager.fetchTokenPermissions( "COM007" );
        final var noPermissions = "";

        return Stream.of(
                Arguments.of( ownerPermissions, OWNER, OWNER, true ),
                Arguments.of( ownerPermissions, OWNER, ADMIN, true ),
                Arguments.of( ownerPermissions, OWNER, STANDARD, true ),
                Arguments.of( ownerPermissions, ADMIN, OWNER, true ),
                Arguments.of( ownerPermissions, ADMIN, ADMIN, true ),
                Arguments.of( ownerPermissions, ADMIN, STANDARD, true ),
                Arguments.of( ownerPermissions, STANDARD, OWNER, true ),
                Arguments.of( ownerPermissions, STANDARD, ADMIN, true ),
                Arguments.of( ownerPermissions, STANDARD, STANDARD, true ),
                Arguments.of( adminPermissions, OWNER, OWNER, false ),
                Arguments.of( adminPermissions, OWNER, ADMIN, false ),
                Arguments.of( adminPermissions, OWNER, STANDARD, false ),
                Arguments.of( adminPermissions, ADMIN, OWNER, false ),
                Arguments.of( adminPermissions, ADMIN, ADMIN, true ),
                Arguments.of( adminPermissions, ADMIN, STANDARD, true ),
                Arguments.of( adminPermissions, STANDARD, OWNER, false ),
                Arguments.of( adminPermissions, STANDARD, ADMIN, true ),
                Arguments.of( adminPermissions, STANDARD, STANDARD, true ),
                Arguments.of( standardPermissions, OWNER, OWNER, false ),
                Arguments.of( standardPermissions, OWNER, ADMIN, false ),
                Arguments.of( standardPermissions, OWNER, STANDARD, false ),
                Arguments.of( standardPermissions, ADMIN, OWNER, false ),
                Arguments.of( standardPermissions, ADMIN, ADMIN, false ),
                Arguments.of( standardPermissions, ADMIN, STANDARD, false ),
                Arguments.of( standardPermissions, STANDARD, OWNER, false ),
                Arguments.of( standardPermissions, STANDARD, ADMIN, false ),
                Arguments.of( standardPermissions, STANDARD, STANDARD, false ),
                Arguments.of( noPermissions, OWNER, OWNER, false ),
                Arguments.of( noPermissions, OWNER, ADMIN, false ),
                Arguments.of( noPermissions, OWNER, STANDARD, false ),
                Arguments.of( noPermissions, ADMIN, OWNER, false ),
                Arguments.of( noPermissions, ADMIN, ADMIN, false ),
                Arguments.of( noPermissions, ADMIN, STANDARD, false ),
                Arguments.of( noPermissions, STANDARD, OWNER, false ),
                Arguments.of( noPermissions, STANDARD, ADMIN, false ),
                Arguments.of( noPermissions, STANDARD, STANDARD, false )
        );
    }

    @ParameterizedTest
    @MethodSource( "canChangeRoleScenarios" )
    void canChangeRoleTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum from, final UserRoleEnum to, final boolean canChange ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setActiveAcspRole( request ).build() );
        Assertions.assertEquals( canChange, canChangeRole( from, to ) );
    }

}


