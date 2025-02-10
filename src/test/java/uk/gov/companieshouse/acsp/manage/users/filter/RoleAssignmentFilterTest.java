package uk.gov.companieshouse.acsp.manage.users.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RoleAssignmentFilterTest {

    private RoleAssignmentFilter roleAssignmentFilter;
    private UsersService usersService;
    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        usersService = Mockito.mock( UsersService.class );
        acspMembersService = Mockito.mock( AcspMembersService.class );
        roleAssignmentFilter = new RoleAssignmentFilter( usersService, acspMembersService );
    }

    @Test
    void doFilterInternalDoesNotAddAnyRolesWhenUnhandledExceptionIsThrown() throws ServletException, IOException {
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doThrow( new IllegalArgumentException( "Something odd happened here" ) ).when( usersService ).fetchUserDetails( user.getUserId() );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );

        final var roles =
                Optional.ofNullable( SecurityContextHolder.getContext() )
                        .map( SecurityContext::getAuthentication )
                        .map( Authentication::getAuthorities )
                        .map( grantedAuthorities -> grantedAuthorities.stream().map( GrantedAuthority::getAuthority ).toList() )
                        .orElse( List.of() );

        Assertions.assertTrue( roles.isEmpty() );
    }

    private static Stream<Arguments> doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesScenarios(){
        return Stream.of(
                Arguments.of( "oauth2" ),
                Arguments.of( "key" )
        );
    }

    @ParameterizedTest
    @MethodSource( "doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesScenarios" )
    void doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesTests( final String ericIdentityType ) throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity-Type",ericIdentityType );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalWithoutEricIdentityTypeDoesNotAddAnyRoles() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalWithMalformedEricIdentityTypeDoesNotAddAnyRoles() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type", "magic" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalWithAPIKeyRequestWithoutEricAuthorisedKeyRolesDoesNotAddAnyRoles() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","key" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalWithNonexistentRequestingUserDoesNotAddAnyRoles() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Roles", "/admin/acsp/search" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Could not find user" ) ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalDoesNotAddAnyRolesWhenDatabaseIsOutOfSyncWithSession() throws ServletException, IOException {
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();
        final var membership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM004" );

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( user.getUserId() );
        Mockito.doReturn( Optional.of( membership ) ).when( acspMembersService ).fetchActiveAcspMembership( membership.getUserId(), membership.getAcspNumber() );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalDoesNotAddAnyRolesWhenReadPermissionsAreMissing() throws ServletException, IOException {
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();
        final var membership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_number=COMA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( user.getUserId() );
        Mockito.doReturn( Optional.of( membership ) ).when( acspMembersService ).fetchActiveAcspMembership( membership.getUserId(), membership.getAcspNumber() );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertTrue( roles.isEmpty() );
    }

    @Test
    void doFilterInternalWithValidAPIKeyRequestAddsKeyRole() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","key" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertEquals( 1, roles.size() );
        Assertions.assertTrue( roles.contains( "ROLE_KEY" ) );
    }

    @Test
    void doFilterInternalWithValidAdminRequestAddsAdminRole() throws ServletException, IOException {
        final var user = testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Roles", "/admin/acsp/search" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( user.getUserId() );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertEquals( 1, roles.size() );
        Assertions.assertTrue( roles.contains( "ROLE_ADMIN_WITH_ACSP_SEARCH_PRIVILEGE" ) );
    }

    private static Stream<Arguments> doFilterInternalWithValidAcspRequestScenarios(){
        final var ownerUser = testDataManager.fetchUserDtos( "COMU002" ).getFirst();
        final var ownerMembership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        final var adminUser = testDataManager.fetchUserDtos( "COMU004" ).getFirst();
        final var adminMembership = testDataManager.fetchAcspMembersDaos( "COM004" ).getFirst();
        final var adminPermissions = testDataManager.fetchTokenPermissions( "COM004" );
        final var standardUser = testDataManager.fetchUserDtos( "COMU007" ).getFirst();
        final var standardMembership = testDataManager.fetchAcspMembersDaos( "COM007" ).getFirst();
        final var standardPermissions = testDataManager.fetchTokenPermissions( "COM007" );

        return Stream.of(
                Arguments.of( ownerUser, ownerMembership, ownerPermissions, "ROLE_ACSP_OWNER" ),
                Arguments.of( adminUser, adminMembership, adminPermissions, "ROLE_ACSP_ADMIN" ),
                Arguments.of( standardUser, standardMembership, standardPermissions, "ROLE_ACSP_STANDARD" )
        );
    }

    @ParameterizedTest
    @MethodSource( "doFilterInternalWithValidAcspRequestScenarios" )
    void doFilterInternalWithValidAcspRequestTests( final User user, final AcspMembersDao membership, final String ericAuthorisedTokenPermissions, final String expectedOutcome ) throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( user.getUserId() );
        Mockito.doReturn( Optional.of( membership ) ).when( acspMembersService ).fetchActiveAcspMembership( membership.getUserId(), membership.getAcspNumber() );

        roleAssignmentFilter.doFilterInternal( request, response, filterChain );
        final var roles = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList();

        Assertions.assertEquals( 1, roles.size() );
        Assertions.assertTrue( roles.contains( expectedOutcome ) );
    }

}
