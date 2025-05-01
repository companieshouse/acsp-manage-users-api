package uk.gov.companieshouse.acsp.manage.users.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UserAuthenticationFilterTest {

    private UserAuthenticationFilter userAuthenticationFilter;
    private UsersService usersService;
    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        usersService = Mockito.mock( UsersService.class );
        acspMembersService = Mockito.mock( AcspMembersService.class );
        userAuthenticationFilter = new UserAuthenticationFilter( usersService, acspMembersService );
    }

    private ArgumentMatcher<Authentication> springRoleWasAssigned( final String springRole ){
        return authentication -> authentication.getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList()
                .contains( springRole );
    }

    @Test
    void doFilterInternalDoesNotAddAnyRolesWhenUnhandledExceptionIsThrown() {
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM002" );

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "COMU002" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU002" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU002" );
        Mockito.doThrow( new IllegalArgumentException( "Something odd happened here" ) ).when( acspMembersService ).fetchMembershipDaos( eq( "COMU002" ), anyString(), eq( false ) );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext, never() ).setAuthentication( any() );
    }

    private static Stream<Arguments> doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesScenarios(){
        return Stream.of(
                Arguments.of( "oauth2" ),
                Arguments.of( "key" )
        );
    }

    @ParameterizedTest
    @MethodSource( "doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesScenarios" )
    void doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesTests( final String ericIdentityType ) {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity-Type",ericIdentityType );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithoutEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithMalformedEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type", "magic" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithAPIKeyRequestWithoutEricAuthorisedKeyRolesDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","key" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalDoesNotAddAnyRolesWhenDatabaseIsOutOfSyncWithSession() {
        final var membership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        final var ericAuthorisedTokenPermissions = testDataManager.fetchTokenPermissions( "COM004" );

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "COMU002" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( testDataManager.fetchUserDtos( membership.getUserId() ).getFirst() ).when( usersService ).fetchUserDetails( membership.getUserId() );
        Mockito.doReturn(List.of( membership ) ).when( acspMembersService ).fetchMembershipDaos( eq( membership.getUserId() ), anyString(), eq( false ) );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithValidAPIKeyRequestAddsKeyRole() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","key" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_KEY" ) ) );
    }

    @Test
    void doFilterInternalWithValidAdminRequestAddsAdminRole() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Roles", "/admin/acsp/search" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_ADMIN_WITH_ACSP_SEARCH_PRIVILEGE" ) ) );
    }

    private static Stream<Arguments> doFilterInternalWithValidAcspRequestScenarios(){
        final var ownerMembership = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        final var ownerPermissions = testDataManager.fetchTokenPermissions( "COM002" );
        final var adminMembership = testDataManager.fetchAcspMembersDaos( "COM004" ).getFirst();
        final var adminPermissions = testDataManager.fetchTokenPermissions( "COM004" );
        final var standardMembership = testDataManager.fetchAcspMembersDaos( "COM007" ).getFirst();
        final var standardPermissions = testDataManager.fetchTokenPermissions( "COM007" );

        return Stream.of(
                Arguments.of( "COMU002", ownerMembership, ownerPermissions, "ROLE_ACSP_OWNER" ),
                Arguments.of( "COMU004", adminMembership, adminPermissions, "ROLE_ACSP_ADMIN" ),
                Arguments.of( "COMU007", standardMembership, standardPermissions, "ROLE_ACSP_STANDARD" )
        );
    }

    @ParameterizedTest
    @MethodSource( "doFilterInternalWithValidAcspRequestScenarios" )
    void doFilterInternalWithValidAcspRequestTests( final String userId, final AcspMembersDao membership, final String ericAuthorisedTokenPermissions, final String expectedOutcome ) {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", userId );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        Mockito.doReturn( testDataManager.fetchUserDtos( membership.getUserId() ).getFirst() ).when( usersService ).fetchUserDetails( membership.getUserId() );
        Mockito.doReturn(List.of( membership ) ).when( acspMembersService ).fetchMembershipDaos( eq( membership.getUserId() ), anyString(), eq( false ) );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( expectedOutcome ) ) );
    }

}
