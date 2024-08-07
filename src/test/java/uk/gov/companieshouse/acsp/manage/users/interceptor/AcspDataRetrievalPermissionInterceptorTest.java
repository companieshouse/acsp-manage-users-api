package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.InvalidTokenPermissionException;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspDataRetrievalPermissionInterceptorTest {

    private AcspDataRetrievalPermissionInterceptor acspDataRetrievalPermissionInterceptor;

    @BeforeEach
    void setup(){
        acspDataRetrievalPermissionInterceptor = new AcspDataRetrievalPermissionInterceptor();
    }

    @Test
    void prehandleWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        HttpServletResponse response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle( request, response, null );

        assertFalse( acspDataRetrievalPermissionInterceptor.preHandle( request, response, null ) );
        assertEquals(403, response.getStatus() );
    }

    @Test
    void prehandleWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_members=read" );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        HttpServletResponse response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle( request, response, null );

        assertTrue( acspDataRetrievalPermissionInterceptor.preHandle( request, response, null ) );
        assertEquals( 200, response.getStatus() );
    }

}
