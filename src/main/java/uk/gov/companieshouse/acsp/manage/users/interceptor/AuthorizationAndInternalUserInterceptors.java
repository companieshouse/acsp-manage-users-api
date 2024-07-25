package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@Component
public class AuthorizationAndInternalUserInterceptors extends AuthorizationInterceptor {

    private final InternalUserInterceptor internalUserInterceptor;

    public AuthorizationAndInternalUserInterceptors( final UsersService usersService ) {
        super( usersService );
        this.internalUserInterceptor = new InternalUserInterceptor( StaticPropertyUtil.APPLICATION_NAMESPACE );
    }

    @Override
    public boolean preHandle( HttpServletRequest request, HttpServletResponse response, Object handler ) {
        final var ericIdentityType = request.getHeader( "Eric-Identity-Type" );

        if ( Objects.isNull( ericIdentityType ) ){
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
