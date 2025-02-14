package uk.gov.companieshouse.acsp.manage.users.filter;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.KEY_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.ericAuthorisedTokenPermissionsAreValid;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersSpringRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserIsPermittedToRetrieveAcspData;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.acsp.manage.users.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDataContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestDetails;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
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
            new PreAuthenticatedAuthenticationToken( UNKNOWN, UNKNOWN,
              Collections.singleton(new SimpleGrantedAuthority(String.format( "ROLE_%s", role ) )) ));
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) {

        try {
            if ( "/acsp-manage-users-api/healthcheck".equals( request.getRequestURI() ) ){
                filterChain.doFilter( request, response );
            }

            RequestDataContext.getInstance().setRequestDetails( new RequestDetails( request ) );

            final var userRole = fetchRequestingUsersSpringRole();

            LOGGER.debugContext( getXRequestId(), "Determining Spring role...", null );
            final var springRole = switch ( userRole ){
                case KEY_ROLE -> isValidAPIKeyRequest( request ) ? KEY_ROLE : UNKNOWN;
                case ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE -> isValidOAuth2Request( request ) ? ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE : UNKNOWN;
                default -> isValidOAuth2Request( request ) && ericAuthorisedTokenPermissionsAreValid( acspMembersService, getEricIdentity() ) && requestingUserIsPermittedToRetrieveAcspData() ? userRole : UNKNOWN;
            };
            LOGGER.debugContext( getXRequestId(), String.format( "Spring role is %s", userRole ), null );

            if ( UNKNOWN.equals( springRole ) ) {
                throw new ForbiddenRuntimeException( "Invalid role" );
            }
            setSpringRole( springRole );

            filterChain.doFilter( request, response );
        } catch ( Exception exception ) {
            LOGGER.errorContext( getXRequestId(), exception, null );
            RequestDataContext.getInstance().clear();
            UserContext.getInstance().clear();
            response.setStatus( 403 );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}