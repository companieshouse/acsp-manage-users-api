package uk.gov.companieshouse.acsp.manage.users.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

@Component
public class AuthorizationAndInternalUserInterceptors extends AuthorizationInterceptor {

    private final InternalUserInterceptor internalUserInterceptor;

    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private static final String X_REQUEST_ID = "X-Request-Id";

    public AuthorizationAndInternalUserInterceptors( final UsersService usersService ) {
        super( usersService );
        this.internalUserInterceptor = new InternalUserInterceptor( StaticPropertyUtil.APPLICATION_NAMESPACE );
    }

    @Override
    public boolean preHandle( HttpServletRequest request, HttpServletResponse response, Object handler ) {
        final var xRequestId = request.getHeader( X_REQUEST_ID );
        final var ericIdentityType = request.getHeader( ERIC_IDENTITY_TYPE );

        if ( Objects.isNull( ericIdentityType ) ){
            LOGGER.errorContext( xRequestId, new Exception( "ERIC-Identity-Type not provided" ), null );
            response.setStatus(401);
            return false;
        }

        if ( ericIdentityType.equals( "oauth2" ) ){
            return super.preHandle( request, response, handler );
        } else if ( ericIdentityType.equals( "key" ) ){
            try {
                return internalUserInterceptor.preHandle( request, response, handler );
            } catch ( IOException e ){
                response.setStatus(401);
                return false;
            }
        }

        LOGGER.errorContext( xRequestId, new Exception( String.format( "Invalid ericIdentityType provided: %s", ericIdentityType ) ), null );
        response.setStatus(401);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if ( isOAuth2Request() ) {
            UserContext.clear();
        }
    }

}
