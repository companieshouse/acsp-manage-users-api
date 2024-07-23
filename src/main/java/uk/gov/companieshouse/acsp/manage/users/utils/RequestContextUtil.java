package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestContextUtil {

    public static String getEricIdentityType(){
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ( (ServletRequestAttributes) requestAttributes );
        final var httpServletRequest = servletRequestAttributes.getRequest();
        return httpServletRequest.getHeader( ERIC_IDENTITY_TYPE );
    }

}
