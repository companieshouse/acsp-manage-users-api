package uk.gov.companieshouse.acsp.manage.users.filter;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.ericAuthorisedTokenPermissionsAreValid;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDataContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDetails;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil;
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
                final var userDetails = usersService.fetchUserDetails(getEricIdentity() );
                UserContext.getInstance().setRequestDetails( userDetails );
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

    private void setSpringRole( final String role ){
        LOGGER.debugContext( getXRequestId(), String.format( "Adding Spring roles: %s", role ), null );

        SecurityContextHolder
          .getContext()
          .setAuthentication(
            new PreAuthenticatedAuthenticationToken( UNDEFINED, UNDEFINED,
              Collections.singleton(new SimpleGrantedAuthority(String.format( "ROLE_%s", role ) )) ));
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) throws ServletException, IOException {
        try {
            RequestDataContext.getInstance().setRequestDetails(new RequestDetails( request) );

            final var userRole = fetchRequestingUsersRole();

            if ( KEY_ROLE.equals(userRole) && isValidAPIKeyRequest( request ) ){
                LOGGER.debugContext( getXRequestId(), "API Key Spring role will be added...", null );
                setSpringRole( KEY_ROLE );
                return;
            }
            if( ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE.equals(userRole) && isValidOAuth2Request( request )){
                LOGGER.debugContext( getXRequestId(), "Admin with ACSP Search privilege Spring role will be added...", null );
                setSpringRole( ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE );
                return;
            }
            if ( isValidOAuth2Request( request ) ){

                final var ericIdentity = RequestUtil.getEricIdentity();
                LOGGER.infoContext( getXRequestId(), "Calculating Spring role based on Eric-Authorised-Token-Permissions...", null );
                if ( ericAuthorisedTokenPermissionsAreValid( acspMembersService, ericIdentity ) && requestingUserIsPermittedToRetrieveAcspData() ) {
                    setSpringRole( userRole );
                } else {
                    LOGGER.errorContext( getXRequestId(), new Exception( "Session is out of sync with the database or requesting user does not have acsp_members=read permission." ), null );
                }

            }
        } catch ( Exception exception ){
            final var xRequestId = Optional.ofNullable( getRequestHeader( request, X_REQUEST_ID ) ).orElse( UNKNOWN );
            LOGGER.errorContext( xRequestId, "Unhandled exception was thrown in UserAuthenticationFilter", exception, null );
        }

        try {
            filterChain.doFilter( request, response );
        } finally {
            LOGGER.infoContext( getXRequestId(), "Clearing request context...", null );
        }
    }

}