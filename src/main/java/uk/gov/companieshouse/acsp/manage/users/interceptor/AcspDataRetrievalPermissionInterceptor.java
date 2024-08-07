package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToRetrieveAcspData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class AcspDataRetrievalPermissionInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        if ( !isOAuth2Request() ){
            return true;
        }

        if ( requestingUserIsPermittedToRetrieveAcspData() ){
            return true;
        }

        LOGGER.debugRequest( request, "User does not have permission to access Acsp data", null );
        response.setStatus( 403 );
        return false;
    }

}
