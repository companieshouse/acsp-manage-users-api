package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_ADMIN_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_OWNER_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_SEARCH_ADMIN_SEARCH;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ACSP_STANDARD_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersAcspRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersSpringRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.isKeyRequest;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.ericAuthorisedTokenPermissionsAreValid;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserIsActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserCanManageMembership;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_TOKEN_PERMISSIONS;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.catalina.connector.Request;
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
import uk.gov.companieshouse.acsp.manage.users.model.RequestDataContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDetails;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestUtilTest {

    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String UNKNOWN = "unknown";

    @BeforeEach
    void setup(){
        acspMembersService = Mockito.mock( AcspMembersService.class );
    }

    @Test
    void isOAuth2RequestAndIsKeyRequestReturnFalseWhenRequestTypeIsUnknown(){
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( new MockHttpServletRequest() ) );
        Assertions.assertFalse( isOAuth2Request() );
        Assertions.assertFalse( isKeyRequest() );
    }

    @Test
    void isOAuth2RequestReturnsTrueAndIsKeyRequestReturnFalseWhenRequestTypeIsOAuth2(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type","oauth2");
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertTrue( isOAuth2Request() );
        Assertions.assertFalse( isKeyRequest() );
    }

    @Test
    void isOAuth2RequestReturnsFalseAndIsKeyRequestReturnTrueWhenRequestTypeIsKey(){
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type","key");
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertFalse( isOAuth2Request() );
        Assertions.assertTrue( isKeyRequest() );
    }

    @Test
    void ericAuthorisedTokenPermissionsAreValidWithNullOrMalformedOrNonexistentUserIdOrUserWithoutActiveMembershipReturnsFalse(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

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
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

        Mockito.doReturn( Optional.of( activeMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

        Assertions.assertFalse( ericAuthorisedTokenPermissionsAreValid( acspMembersService, "COMU002" ) );
    }

    @Test
    void ericAuthorisedTokenPermissionsAreValidWithSessionThatMatchesDatabaseReturnsTrue(){
        final var activeMembership = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

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
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

        Assertions.assertEquals( expectedAcspNumber, fetchRequestingUsersActiveAcspNumber() );
    }

    @Test
    void getXRequestIdIsUnknownWhenXRequestIdIsMissing(){
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( new MockHttpServletRequest() ) );
        Assertions.assertEquals( UNKNOWN, getXRequestId() );
    }

    @Test
    void getXRequestIdRetrievesXRequestId(){
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id","theId123");
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertEquals( "theId123", getXRequestId() );
    }

    private static Stream<Arguments> fetchRequestingUsersSpringRoleScenarios(){
        return Stream.of(
                Arguments.of( ERIC_IDENTITY_TYPE, KEY, KEY_ROLE ),
                Arguments.of( ERIC_AUTHORISED_TOKEN_PERMISSIONS, testDataManager.fetchTokenPermissions( "COM002" ), ACSP_OWNER_ROLE ),
                Arguments.of( ERIC_AUTHORISED_TOKEN_PERMISSIONS, testDataManager.fetchTokenPermissions( "COM004" ), ACSP_ADMIN_ROLE ),
                Arguments.of( ERIC_AUTHORISED_TOKEN_PERMISSIONS, testDataManager.fetchTokenPermissions( "COM007" ), ACSP_STANDARD_ROLE ),
                Arguments.of( ERIC_AUTHORISED_ROLES, ACSP_SEARCH_ADMIN_SEARCH, ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE ),
                Arguments.of( "X-Request-Id", "theId123", BASIC_OAUTH_ROLE )

        );
    }

    @ParameterizedTest
    @MethodSource( "fetchRequestingUsersSpringRoleScenarios" )
    void fetchRequestingUsersSpringRoleReturnsAcspOwnerRoleWhenUserIsAcspOwner( final String headerKey, final String headerValue, final String expectedSpringRole ){
        final var request = new MockHttpServletRequest();
        request.addHeader( headerKey, headerValue );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertEquals( expectedSpringRole, fetchRequestingUsersSpringRole() );
    }

    private static Stream<Arguments> fetchRequestingUsersAcspRoleScenarios(){
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
    @MethodSource( "fetchRequestingUsersAcspRoleScenarios" )
    void fetchRequestingUsersAcspRoleTests( final String requestingUsersEricAuthorisedTokenPermissions, final UserRoleEnum expectedOutcome ){
        final var request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", requestingUsersEricAuthorisedTokenPermissions );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

        Assertions.assertEquals( expectedOutcome, fetchRequestingUsersAcspRole() );
    }

    @Test
    void getEricIdentityIsUnknownWhenEricIdentityIsMissing(){
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( new MockHttpServletRequest() ) );
        Assertions.assertEquals( UNKNOWN, getEricIdentity() );
    }

    @Test
    void getEricIdentityRetrievesgetEricIdentity(){
        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY,"COMU002");
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertEquals( "COMU002", getEricIdentity() );
    }

    @Test
    void requestingUserIsPermittedToRetrieveAcspDataWithoutEricAuthorisedTokenPermissionsReturnsFalse(){
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( new MockHttpServletRequest() ) );
        Assertions.assertFalse( requestingUserIsPermittedToRetrieveAcspData() );
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
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertEquals( expectedOutcome, requestingUserIsPermittedToRetrieveAcspData() );
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
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

        Assertions.assertFalse( requestingUserIsActiveMemberOfAcsp( "TSA001" ) );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspReturnsTrueWhenSpecifiedAcspNumberMatchesSession(){
        final var request = new MockHttpServletRequest();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

        Assertions.assertTrue( requestingUserIsActiveMemberOfAcsp( "COMA001" ) );
    }

    @Test
    void requestingUserCanManageMembershipWithNullRoleThrowsNullPointerException(){
        final var request = new MockHttpServletRequest();
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ownerPermissions );
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
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
        RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );
        Assertions.assertEquals( canManage, requestingUserCanManageMembership( targetRole ) );
    }

}


