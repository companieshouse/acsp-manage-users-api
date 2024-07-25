package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
public class RequestContextUtilTest {

    @Test
    void isOAuth2RequestReturnsTrueIfEricIdentityTypeIsOAuth2(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY_TYPE, "oauth2" );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertTrue( isOAuth2Request() );
    }

    @Test
    void isOAuth2RequestReturnsFalseIfEricIdentityTypeIsNotOAuth2(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY_TYPE, "key" );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( isOAuth2Request() );
    }

}
