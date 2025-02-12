package uk.gov.companieshouse.acsp.manage.users.filter;

import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedRolesContext.hasAdminAcspSearchPermission;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.ericAuthorisedTokenPermissionsAreValid;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.fetchRequestingUsersActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.fetchRequestingUsersRole;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.EricAuthorisedTokenPermissionsContext.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.RequestDetailsContext.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.UserContext.setLoggedUser;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.clear;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.RequestDetailsContext.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.model.RequestContext.setRequestDetails;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class UserAuthenticationFilter extends OncePerRequestFilter {

    private final UsersService usersService;
    private final AcspMembersService acspMembersService;
    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private static final String KEY = "key";
    private static final String UNDEFINED = "undefined";
    private static final String KEY_ROLE = "KEY";
    private static final String ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE = "ADMIN_WITH_ACSP_SEARCH_PRIVILEGE";
    private static final String ADMIN_ACSP_SEARCH_PERMISSION = "/admin/acsp/search";
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String UNKNOWN = "unknown";

    public UserAuthenticationFilter( final UsersService usersService, final AcspMembersService acspMembersService ){
        this.usersService = usersService;
        this.acspMembersService = acspMembersService;
    }

    private boolean isValidAPIKeyRequest( final HttpServletRequest request ){
        LOGGER.infoContext( getXRequestId(), "Checking if this is a valid API Key Request...", null );
        final var ericIdentity = AuthorisationUtil.getAuthorisedIdentity( request );
        final var ericIdentityType = AuthorisationUtil.getAuthorisedIdentityType( request );
        final var hasInternalUserRole = AuthorisationUtil.hasInternalUserRole( request );
        if ( Objects.nonNull( ericIdentity ) && KEY.equals( ericIdentityType ) && hasInternalUserRole ){
            LOGGER.debugContext( getXRequestId(), "Confirmed this is a valid API Key Request.", null );
            return true;
        }
        LOGGER.debugContext( getXRequestId(), "Confirmed this is not a valid API Key Request.", null );
        return false;
    }

    private boolean isValidOAuth2Request( final HttpServletRequest request ){
        LOGGER.infoContext( getXRequestId(), "Checking if this is a valid OAuth2 Request...", null );
        if ( AuthorisationUtil.isOauth2User( request ) ){
            try {
                final var userDetails = usersService.fetchUserDetails( getEricIdentity() );
                setLoggedUser( userDetails );
                LOGGER.debugContext( getXRequestId(), "Confirmed this is a valid OAuth2 Request.", null );
                return true;
            } catch ( NotFoundRuntimeException exception ) {
                LOGGER.debugContext( getXRequestId(), String.format( "Confirmed this is not a valid OAuth2 Request, because requesting user %s was not found.", getEricIdentity() ), null );
                return false;
            }
        }
        LOGGER.debugContext( getXRequestId(), "Confirmed this is not a valid OAuth2 Request.", null );
        return false;
    }

    private void setSpringRoles( final List<String> roles ){
        LOGGER.debugContext( getXRequestId(), String.format( "Adding Spring roles: %s", String.join( ", ", roles ) ), null );
        final var simpleGrantedAuthorities = roles.stream()
                .map( role -> String.format( "ROLE_%s", role ) )
                .map( SimpleGrantedAuthority::new )
                .toList();
        SecurityContextHolder.getContext().setAuthentication( new PreAuthenticatedAuthenticationToken( UNDEFINED, UNDEFINED, simpleGrantedAuthorities ) );
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) throws ServletException, IOException {
        try {
            setRequestDetails( request );

            final var springRoles = new LinkedList<String>();
            if ( isValidAPIKeyRequest( request ) ){
                LOGGER.debugContext( getXRequestId(), "API Key Spring role will be added...", null );
                springRoles.add( KEY_ROLE );
            } else if ( isValidOAuth2Request( request ) ){

                LOGGER.infoContext( getXRequestId(), "Calculating Spring roles based on Eric-Authorised-Roles...", null );
                if ( hasAdminAcspSearchPermission() ){
                    LOGGER.debugContext( getXRequestId(), String.format( "Found %s permission in Eric-Authorised-Roles... relevant Spring role will be added...", ADMIN_ACSP_SEARCH_PERMISSION ), null );
                    springRoles.add( ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE );
                }

                final var requestingUserIsAcspMember = Objects.nonNull( fetchRequestingUsersActiveAcspNumber() );
                if ( requestingUserIsAcspMember ){
                    final var ericIdentity = AuthorisationUtil.getAuthorisedIdentity( request );
                    LOGGER.infoContext( getXRequestId(), "Calculating Spring role based on Eric-Authorised-Token-Permissions...", null );
                    if ( ericAuthorisedTokenPermissionsAreValid( acspMembersService, ericIdentity ) && requestingUserIsPermittedToRetrieveAcspData() ) {
                        final var role = fetchRequestingUsersRole();
                        if ( Objects.nonNull( role ) ){
                            LOGGER.debugContext( getXRequestId(), String.format( "Deduced user has %s role... adding relevant Spring role...", role.getValue() ), null );
                            springRoles.add( String.format( "ACSP_%s", role.getValue().toUpperCase() ) );
                        }
                    } else {
                        LOGGER.errorContext( getXRequestId(), new Exception( "Session is out of sync with the database or requesting user does not have acsp_members=read permission." ), null );
                    }
                }
            }
            setSpringRoles( springRoles );
        } catch ( Exception exception ){
            final var xRequestId = Optional.ofNullable( getRequestHeader( request, X_REQUEST_ID ) ).orElse( UNKNOWN );
            LOGGER.errorContext( xRequestId, "Unhandled exception was thrown in UserAuthenticationFilter", exception, null );
        }

        try {
            filterChain.doFilter( request, response );
        } finally {
            LOGGER.infoContext( getXRequestId(), "Clearing request context...", null );
            clear();
        }
    }

}