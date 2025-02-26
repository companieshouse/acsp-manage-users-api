package uk.gov.companieshouse.acsp.manage.users.filter;

import static uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext.setRequestContext;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.KEY_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole.UNKNOWN_ROLE;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getActiveAcspRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getAdminPrivileges;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricAuthorisedKeyRoles;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.OAUTH2;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Objects;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.enums.SpringRole;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

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
        User user = null;
        if ( OAUTH2.equals( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ) ){
            try { user = usersService.fetchUserDetails( getRequestHeader( request, ERIC_IDENTITY ) ); }
            catch ( NotFoundRuntimeException ignored ) {}
        }

        return new RequestContextDataBuilder()
                .setXRequestId( request )
                .setEricIdentity( request )
                .setEricIdentityType( request )
                .setEricAuthorisedKeyRoles( request )
                .setActiveAcspNumber( request )
                .setActiveAcspRole( request )
                .setAdminPrivileges( request )
                .setUser( user )
                .build();
    }

    private SpringRole computeSpringRole(){
        LOGGER.infoContext( getXRequestId(), "Checking if this is a valid API Key Request...", null );
        final var isValidAPIKeyRequest = !getEricIdentity().equals( UNKNOWN ) && getEricIdentityType().equals( KEY ) && getEricAuthorisedKeyRoles().equals( "*" );
        if ( isValidAPIKeyRequest ){
            LOGGER.debugContext( getXRequestId(), "Confirmed this is a valid API Key Request.", null );
            return KEY_ROLE;
        }
        LOGGER.debugContext( getXRequestId(), "Confirmed this is not a valid API Key Request. Checking if this is a valid OAuth2 Request...", null );
        final var isValidOAuth2Request = !getEricIdentity().equals( UNKNOWN ) && isOAuth2Request() && Objects.nonNull( getUser() );
        if ( !isValidOAuth2Request ){
            LOGGER.debugContext( getXRequestId(), "Confirmed this is not a valid OAuth2 Request.", null );
            return UNKNOWN_ROLE;
        }
        LOGGER.debugContext( getXRequestId(), "Confirmed this is a valid OAuth2 Request.", null );

        if ( !getActiveAcspNumber().equals( UNKNOWN ) ){
            LOGGER.debugContext( getXRequestId(), "Confirmed this request is from an Acsp Member. Checking session validity...", null );
            final var springRole = acspMembersService.fetchActiveAcspMembership( getEricIdentity(), getActiveAcspNumber() )
                    .map( AcspMembersDao::getUserRole )
                    .filter( databaseUserRole -> databaseUserRole.equals( getActiveAcspRole() ) )
                    .map( SpringRole::fromUserRoleEnum )
                    .orElse( UNKNOWN_ROLE );
            LOGGER.debugContext( getXRequestId(), springRole.equals( UNKNOWN_ROLE ) ? "Confirmed session is invalid." : "Confirmed session is valid.", null );
           return springRole;
        }

        if ( getAdminPrivileges().contains( ACSP_SEARCH_ADMIN_SEARCH ) ){
            LOGGER.debugContext( getXRequestId(), String.format( "Confirmed that this request has %s privilege.", ACSP_SEARCH_ADMIN_SEARCH ), null );
            return ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE;
        }

        return BASIC_OAUTH_ROLE;
    }

    private void setSpringRole( final String role ){
        LOGGER.debugContext( getXRequestId(), String.format( "Adding Spring role: %s", role ), null );
        SecurityContextHolder.getContext().setAuthentication( new PreAuthenticatedAuthenticationToken( UNKNOWN, UNKNOWN, Collections.singleton( new SimpleGrantedAuthority( String.format( "ROLE_%s", role ) ) ) ) );
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) {
        try {
            setRequestContext( buildRequestContextData( request ) );
            setSpringRole( computeSpringRole().getValue() );
            filterChain.doFilter( request, response );
        } catch ( Exception exception ) {
            LOGGER.errorContext( getXRequestId(), exception, null );
            RequestContext.clear();
            response.setStatus( 403 );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}