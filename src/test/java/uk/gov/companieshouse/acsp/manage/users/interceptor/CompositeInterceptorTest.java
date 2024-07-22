package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class CompositeInterceptorTest {

    @Mock
    private UsersService usersService;

    private AuthorizationInterceptor authorizationInterceptor;

    private InternalUserInterceptor internalUserInterceptor;

    private CompositeInterceptor compositeInterceptor;

    @BeforeEach
    void setup(){
        authorizationInterceptor = new AuthorizationInterceptor( usersService );
        internalUserInterceptor = new InternalUserInterceptor();
        compositeInterceptor = new CompositeInterceptor( authorizationInterceptor, internalUserInterceptor );
    }

    @Test
    void preHandleWithoutEricHeadersReturnsUnauthorised() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse( compositeInterceptor.preHandle(request, response, null) );
        assertEquals(401, response.getStatus() );
    }

    @Test
    void preHandleWithMalformedRequestReturnsForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "$$$");
        request.addHeader( "ERIC-Identity-Type", "oauth2" );

        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found" ) ).when( usersService ).fetchUserDetails( "$$$" );

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse( compositeInterceptor.preHandle(request, response, null) );
        assertEquals(403, response.getStatus() );
    }

    @Test
    void preHandleWithWellFormedOAuth2RequestSucceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        final var bruce = new User()
                .userId( "111" )
                .email( "bruce.wayne@gotham.city" )
                .displayName( "Batman" );

        Mockito.doReturn( bruce ).when( usersService ).fetchUserDetails( "111" );

        HttpServletResponse response = new MockHttpServletResponse();
        assertTrue( compositeInterceptor.preHandle(request, response, null) );
    }

    @Test
    void preHandleWithWellFormedApiKeyRequestSucceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles","*");

        HttpServletResponse response = new MockHttpServletResponse();
        assertTrue( compositeInterceptor.preHandle(request, response, null) );
    }

}
