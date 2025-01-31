package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.util.security.AuthorisationUtil.getAuthorisedIdentityType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class CompositeAuthorizationInterceptor implements HandlerInterceptor {

    private final UsersService usersService;
    private final InternalUserInterceptor internalUserInterceptor;
    private final boolean checkOAuth2Requests;
    private final boolean checkAPIKeyRequests;
    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private static final String OAUTH2 = "oauth2";
    private static final String KEY = "key";

    public CompositeAuthorizationInterceptor( final boolean checkOAuth2Requests, final boolean checkAPIKeyRequests, final UsersService usersService ) {
        this.checkOAuth2Requests = checkOAuth2Requests;
        this.checkAPIKeyRequests = checkAPIKeyRequests;
        this.usersService = usersService;
        this.internalUserInterceptor = new InternalUserInterceptor( StaticPropertyUtil.APPLICATION_NAMESPACE );
    }

    private boolean isValidOAuth2Request( final HttpServletRequest request, final HttpServletResponse response ){
        final var ericIdentity = AuthorisationUtil.getAuthorisedIdentity( request );

        if ( Objects.isNull( ericIdentity ) ) {
            LOGGER.debugRequest( request, "Invalid user", null );
            response.setStatus( 401 );
            return false;
        }

        try {
            final var userDetails = usersService.fetchUserDetails( ericIdentity );
            LOGGER.infoContext( ericIdentity, "User details fetched and stored in context : " + ericIdentity, null );
            UserContext.setLoggedUser( userDetails );
            return true;
        } catch ( NotFoundRuntimeException exception ) {
            LOGGER.debugRequest( request, "Could not find user with identity [" + ericIdentity + "]", null );
            response.setStatus( 403 );
            return false;
        }
    }

    private boolean isValidAPIKeyRequest( final HttpServletRequest request, final HttpServletResponse response, final Object handler ){
        try {
            return internalUserInterceptor.preHandle( request, response, handler );
        } catch ( IOException exception ){
            response.setStatus( 401 );
            return false;
        }
    }

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        final var ericIdentityType = getAuthorisedIdentityType( request );
        if ( checkOAuth2Requests && OAUTH2.equals( ericIdentityType ) ){
            return isValidOAuth2Request( request, response );
        } else if ( checkAPIKeyRequests && KEY.equals( ericIdentityType ) ){
            return isValidAPIKeyRequest( request, response, handler );
        }
        LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Invalid ericIdentityType provided: %s", ericIdentityType ) ), null );
        response.setStatus( 401 );
        return false;
    }

    @Override
    public void afterCompletion( final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception exception ) {
        UserContext.clear();
    }

}
