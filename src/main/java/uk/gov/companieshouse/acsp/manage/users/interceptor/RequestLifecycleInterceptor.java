package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.OAUTH2;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext.setRequestContext;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.logging.util.RequestLogger;

@Component
public class RequestLifecycleInterceptor implements HandlerInterceptor, RequestLogger {

    private final UsersService usersService;

    public RequestLifecycleInterceptor( final UsersService usersService ) {
        this.usersService = usersService;
    }

    private void setupRequestContext( final HttpServletRequest request, final User user ){
        final var requestContextData = new RequestContextDataBuilder()
                .setXRequestId( request )
                .setEricIdentity( request )
                .setEricIdentityType( request )
                .setEricAuthorisedKeyRoles( request )
                .setActiveAcspNumber( request )
                .setActiveAcspRole( request )
                .setAdminPrivileges( request )
                .setUser( user )
                .build();

        setRequestContext( requestContextData );
    }

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        logStartRequestProcessing( request, LOGGER );
        User user = null;
        if ( OAUTH2.equals( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ) ){
            try {
                user = usersService.fetchUserDetails( getRequestHeader( request, ERIC_IDENTITY ) );
            } catch ( NotFoundRuntimeException exception ) {
                LOGGER.debugContext( getRequestHeader( request, X_REQUEST_ID ), String.format( "Unable to find user %s", getRequestHeader( request, ERIC_IDENTITY ) ), null );
                response.setStatus( 403 );
                return false;
            }
        }
        setupRequestContext( request, user );
        return true;
    }

    @Override
    public void postHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler, final ModelAndView modelAndView ) {
        logEndRequestProcessing( request, response, LOGGER );
    }

    @Override
    public void afterCompletion( final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception exception ) {
        RequestContext.clear();
    }

}
