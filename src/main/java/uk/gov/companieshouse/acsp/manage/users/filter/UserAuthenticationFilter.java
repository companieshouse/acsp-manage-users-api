package uk.gov.companieshouse.acsp.manage.users.filter;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.KEY_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.UNKNOWN_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.OAUTH2;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;

public class UserAuthenticationFilter extends OncePerRequestFilter {

    private final UsersService usersService;
    private final AcspMembersService acspMembersService;

    private static final String ACSP_SEARCH_ADMIN_SEARCH = "/admin/acsp/search";
    private static final String KEY = "key";


    public UserAuthenticationFilter( final UsersService usersService, final AcspMembersService acspMembersService ){
        this.usersService = usersService;
        this.acspMembersService = acspMembersService;
    }

    private RequestContextData buildRequestContextData( final HttpServletRequest request ){
        return new RequestContextDataBuilder()
                .setXRequestId( request )
                .setEricIdentity( request )
                .setEricIdentityType( request )
                .setEricAuthorisedKeyRoles( request )
                .setActiveAcspNumber( request )
                .setActiveAcspRole( request )
                .setAdminPrivileges( request )
                .build();
    }

    private SpringRole computeSpringRole( final RequestContextData requestContextData ){
        LOGGER.infoContext( requestContextData.getXRequestId(), "Checking if this is a valid API Key Request...", null );
        if ( isValidAPIKeyRequest( requestContextData ) ) {
            return KEY_ROLE;
        }
        LOGGER.debugContext( requestContextData.getXRequestId(), "Confirmed this is not a valid API Key Request. Checking if this is a valid OAuth2 Request...", null );
        if ( !isValidOAuth2Request( requestContextData ) ) {
            return UNKNOWN_ROLE;
        }
        LOGGER.debugContext( requestContextData.getXRequestId(), "Confirmed this is a valid OAuth2 Request.", null );
        if ( !requestContextData.getActiveAcspNumber().equals( UNKNOWN ) ){
            return getAcspMemberRole( requestContextData );
        }
        if ( requestContextData.getAdminPrivileges().contains( ACSP_SEARCH_ADMIN_SEARCH ) ) {
            return ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
        }
        return BASIC_OAUTH_ROLE;
    }

    private boolean isValidAPIKeyRequest( final RequestContextData requestContextData ) {
        return !requestContextData.getEricIdentity().equals( UNKNOWN ) && requestContextData.getEricIdentityType().equals( KEY ) && requestContextData.getEricAuthorisedKeyRoles().equals( "*" );
    }

    private boolean isValidOAuth2Request( final RequestContextData requestContextData ) {
        return !requestContextData.getEricIdentity().equals( UNKNOWN ) && requestContextData.getEricIdentityType().equals( OAUTH2 );
    }

    private SpringRole getAcspMemberRole( final RequestContextData requestContextData ) {
        LOGGER.debugContext( requestContextData.getXRequestId(), "Confirmed this request is from an Acsp Member. Checking session validity...", null );
        final var user = usersService.fetchUserDetails( requestContextData.getEricIdentity() );

        return Optional.ofNullable( acspMembersService.fetchMembershipDaos( requestContextData.getEricIdentity(), user.getEmail(), false ) )
                .filter( memberships -> !memberships.isEmpty() )
                .map( List::getFirst )
                .filter( membership -> requestContextData.getActiveAcspNumber().equals( membership.getAcspNumber() ) )
                .map( AcspMembersDao::getUserRole )
                .filter( databaseUserRole -> databaseUserRole.equals( requestContextData.getActiveAcspRole() ) )
                .map( SpringRole::fromUserRoleEnum )
                .orElse( UNKNOWN_ROLE );
    }

    private void setSpringRole( final RequestContextData requestContextData, final String role ){
        LOGGER.debugContext( requestContextData.getXRequestId(), String.format( "Adding Spring role: %s", role ), null );
        SecurityContextHolder.getContext().setAuthentication( new PreAuthenticatedAuthenticationToken( UNKNOWN, UNKNOWN, Collections.singleton( new SimpleGrantedAuthority( String.format( "ROLE_%s", role ) ) ) ) );
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) {
        try {
            final var requestContextData = buildRequestContextData( request );
            setSpringRole( requestContextData, computeSpringRole( requestContextData ).getValue() );
            filterChain.doFilter( request, response );
        } catch ( Exception exception ) {
            LOGGER.errorContext( getRequestHeader( request, X_REQUEST_ID ), exception, null );
            response.setStatus( 403 );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}