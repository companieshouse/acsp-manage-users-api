package uk.gov.companieshouse.acsp.manage.users.model;

import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedRolesContext.hasAdminAcspSearchPermission;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedRolesContext.setEricAuthorisedRoles;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.ericAuthorisedTokenPermissionsAreValid;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.fetchRequestingUsersActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.fetchRequestingUsersRole;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.requestingUserCanManageMembership;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.requestingUserIsActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.setEricAuthorisedTokenPermissions;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.UserContext.getLoggedUser;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.UserContext.setLoggedUser;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.clear;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.RequestDetailsContext.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.RequestDetailsContext.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.RequestDetailsContext.setRequestDetails;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestContextTest {

    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String UNKNOWN = "unknown";

    @BeforeEach
    void setup(){
        acspMembersService = Mockito.mock( AcspMembersService.class );
    }

    @Test
    void setRequestDetailsWithNullRequestSetsXRequestIdAndEricIdentityTypeToUnknown(){
        setRequestDetails( null );
        Assertions.assertEquals( UNKNOWN, getXRequestId() );
        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void setRequestDetailsWithRequestThatDoesNotHaveXRequestIdAndEricIdentityTypeSetsVariablesToUnknown(){
        setRequestDetails( new MockHttpServletRequest() );
        Assertions.assertEquals( UNKNOWN, getXRequestId() );
        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void setRequestDetailsWithRequestThatHasXRequestIdAndEricIdentityTypeSetsVariables(){
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity-Type","oauth2");

        setRequestDetails( request );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertTrue( isOAuth2Request() );
    }

    @Test
    void isOAuth2RequestWithAPIKeyRequestReturnsFalse(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type","key");

        setRequestDetails( request );
        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void setLoggedUserWithNullSetsUserToNull(){
        setLoggedUser( null );
        Assertions.assertNull( getLoggedUser() );
    }

    @Test
    void setLoggedUserSetsSpecifiedUser(){
        final var user = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
        setLoggedUser( user );
        Assertions.assertEquals( user, getLoggedUser() );
    }

    @Test
    void setEricAuthorisedRolesWillNullDoesNotSetPermission(){
        setEricAuthorisedRoles( null );
        Assertions.assertFalse( hasAdminAcspSearchPermission() );
    }

    @Test
    void setEricAuthorisedRolesWithRequestThatDoesNotHaveEricAuthorisedRolesDoesNotSetPermission(){
        setEricAuthorisedRoles( new MockHttpServletRequest() );
        Assertions.assertFalse( hasAdminAcspSearchPermission() );
    }

    private static Stream<Arguments> hasAdminAcspSearchPermissionScenarios(){
        return Stream.of(
                Arguments.of( "/admin/acsp/search", true ),
                Arguments.of( "/admin/some/other/permission", false ),
                Arguments.of( "/admin/some/other/permission /admin/acsp/search /admin/yet/another/permission", true ),
                Arguments.of( "/admin/some/other/permission /admin/yet/another/permission", false )
        );
    }

    @ParameterizedTest
    @MethodSource( "hasAdminAcspSearchPermissionScenarios" )
    void hasAdminAcspSearchPermissionTests( final String ericAuthorisedRoles, final boolean expectedOutcome ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "ERIC-Authorised-Roles", ericAuthorisedRoles );
        setEricAuthorisedRoles( request );
        Assertions.assertEquals( expectedOutcome, hasAdminAcspSearchPermission() );
    }

    @Test
    void setEricAuthorisedTokenPermissionsWithNullRequestDoesNotSetAnyPermissions(){
        setEricAuthorisedTokenPermissions( null );
        Assertions.assertFalse( requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void setEricAuthorisedTokenPermissionsWithRequestThatDoesNotHaveEricAuthorisedTokenPermissionsDoesNotSetAnyPermissions(){
        setEricAuthorisedTokenPermissions( new MockHttpServletRequest() );
        Assertions.assertFalse( requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void setEricAuthorisedTokenPermissionsSetsSpecifiedPermissions(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) );
        setEricAuthorisedTokenPermissions( request );
        Assertions.assertTrue( requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void ericAuthorisedTokenPermissionsAreValidWithNullOrMalformedOrNonexistentUserIdOrUserWithoutActiveMembershipReturnsFalse(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) );
        setEricAuthorisedTokenPermissions( request );

        Mockito.doReturn(Optional.empty() ).when( acspMembersService ).fetchActiveAcspMembership( null, "TSA001" );
        Mockito.doReturn(Optional.empty() ).when( acspMembersService ).fetchActiveAcspMembership( "$$$", "TSA001" );
        Mockito.doReturn(Optional.empty() ).when( acspMembersService ).fetchActiveAcspMembership( "404User", "TSA001" );
        Mockito.doReturn(Optional.empty() ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, null ) );
        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "$$$" ) );
        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "404User" ) );
        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "TSU001" ) );
    }

    @Test
    void ericAuthorisedTokenPermissionsAreValidWithSessionThatDoesNotMatchDatabaseReturnsFalse(){
        final var activeMembership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM004" ) );
        setEricAuthorisedTokenPermissions( request );

        Mockito.doReturn( Optional.of( activeMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "COMU002" ) );
    }

    @Test
    void ericAuthorisedTokenPermissionsAreValidWithSessionThatMatchesDatabaseReturnsTrue(){
        final var activeMembership = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) );
        setEricAuthorisedTokenPermissions( request );

        Mockito.doReturn( Optional.of( activeMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

        Assertions.assertTrue( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "TSU001" ) );
    }

    static Stream<Arguments> fetchRequestingUsersActiveAcspNumberTestData(){
        return Stream.of(
                Arguments.of( "", null ),
                Arguments.of( "acsp_number=TSA001", "TSA001" ),
                Arguments.of( "xacsp_number=TSA001", null ),
                Arguments.of( "acspx_number=TSA001", null ),
                Arguments.of( "acsp_numberx=TSA001", null ),
                Arguments.of( "acsp_number=$$$", null ),
                Arguments.of( "acsp_members_owner=create", null ),
                Arguments.of( "acsp_number=TSA001 acsp_members_owner=create", "TSA001" ),
                Arguments.of( "acsp_members_owner=create acsp_number=TSA001", "TSA001" ),
                Arguments.of( "acsp_members_owner=create acsp_number=TSA001 acsp_members_admin=create", "TSA001" )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchRequestingUsersActiveAcspNumberTestData" )
    void fetchRequestingUsersActiveAcspNumberRetrievesAcspNumberFromSession( final String ericAuthorisedTokenPermissions, final String expectedAcspNumber ){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );

        Assertions.assertEquals( expectedAcspNumber, fetchRequestingUsersActiveAcspNumber() );
    }

    private static Stream<Arguments> requestingUserIsPermittedToRetrieveAcspDataScenarios(){
        return Stream.of(
                Arguments.of( "acsp_members=read", true ),
                Arguments.of( "god_mode=active", false ),
                Arguments.of( "god_mode=active acsp_members=read code=create", true ),
                Arguments.of( "god_mode=active code=create", false )
        );
    }

    @ParameterizedTest
    @MethodSource( "requestingUserIsPermittedToRetrieveAcspDataScenarios" )
    void requestingUserIsPermittedToRetrieveAcspDataTests( final String ericAuthorisedTokenPermissions, final boolean expectedOutcome ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );
        Assertions.assertEquals( expectedOutcome, requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void requestingUserCanManageMembershipWithNullRoleThrowsNullPointerException(){
        final var request = new MockHttpServletRequest();
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ownerPermissions );
        setEricAuthorisedTokenPermissions( request );
        Assertions.assertThrows( NullPointerException.class, () -> requestingUserCanManageMembership( null ) );
    }

    private static Stream<Arguments> requestingUserCanManageMembershipScenarios(){
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
    @MethodSource( "requestingUserCanManageMembershipScenarios" )
    void requestingUserCanManageMembershipTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum targetRole, final boolean canManage ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );
        Assertions.assertEquals( canManage, requestingUserCanManageMembership( targetRole ) );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> requestingUserIsActiveMemberOfAcsp( null ) );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspReturnsFalseWhenSpecifiedAcspNumberDoesNotMatchSession(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );

        Assertions.assertFalse( requestingUserIsActiveMemberOfAcsp( "TSA001" ) );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspReturnsTrueWhenSpecifiedAcspNumberMatchesSession(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );

        Assertions.assertTrue( requestingUserIsActiveMemberOfAcsp( "COMA001" ) );
    }

    private static Stream<Arguments> fetchRequestingUsersRoleScenarios(){
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
    @MethodSource( "fetchRequestingUsersRoleScenarios" )
    void fetchRequestingUsersRoleTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum expectedOutcome ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        setEricAuthorisedTokenPermissions( request );

        Assertions.assertEquals( expectedOutcome, fetchRequestingUsersRole() );
    }

    @Test
    void clearDestroysAllThreadLocal(){
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123") ;
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "ERIC-Authorised-Roles", "/admin/acsp/search" );
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) );

        setRequestDetails( request );
        setLoggedUser( user );
        setEricAuthorisedRoles( request );
        setEricAuthorisedTokenPermissions( request );

        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( user, getLoggedUser() );
        Assertions.assertTrue( hasAdminAcspSearchPermission() );
        Assertions.assertTrue( requestingUserIsPermittedToRetrieveAcspData() );

        clear();

        Assertions.assertEquals( UNKNOWN, getXRequestId() );
        Assertions.assertNull( getLoggedUser() );
        Assertions.assertFalse( hasAdminAcspSearchPermission() );
        Assertions.assertFalse( requestingUserIsPermittedToRetrieveAcspData() );
    }

}
