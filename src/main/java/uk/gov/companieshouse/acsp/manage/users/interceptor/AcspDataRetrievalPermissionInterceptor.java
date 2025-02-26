package uk.gov.companieshouse.acsp.manage.users.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

@Component
public class AcspDataRetrievalPermissionInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private static final String X_REQUEST_ID = "X-Request-Id";

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        if ( !isOAuth2Request() || requestingUserIsPermittedToRetrieveAcspData() ){
            return true;
        }

        final var xRequestId = request.getHeader( X_REQUEST_ID );
        final var ericIdentity = request.getHeader( ERIC_IDENTITY );
        LOGGER.errorContext( xRequestId, new Exception( String.format( "%s user does not have permission to access Acsp data", ericIdentity ) ), null );
        response.setStatus( 403 );
        return false;
    }

}
