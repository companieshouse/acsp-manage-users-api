package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AuthorizationInterceptorTest {

    AuthorizationInterceptor interceptor;

    @Mock
    UsersService usersService;

    @Mock
    InternalUserInterceptor internalUserInterceptor;

    @InjectMocks
    AuthorizationInterceptor authorizationInterceptor;

    @BeforeEach
    void setup(){
         interceptor = new AuthorizationInterceptor(usersService, internalUserInterceptor);
    }

    @Test
    void preHandleWithoutHeadersReturns401() throws IOException {

        HttpServletRequest request = new MockHttpServletRequest();

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithoutEricIdentityReturns401() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type", "oauth2");

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithoutEricIdentityTypeReturns401() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "abcd123456");

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithIncorrectEricIdentityTypeReturns401() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "abcd123456");
        request.addHeader("Eric-Identity-Type", "key");

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithMalformedOrNonexistentEricIdentityReturn403() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "$$$");
        request.addHeader( "ERIC-Identity-Type", "oauth2" );

        HttpServletResponse response = new MockHttpServletResponse();

        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found" ) ).when( usersService ).fetchUserDetails( anyString() );

        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleShouldReturnTrueWhenAuthHeaderAndAuthHeaderTypeOauthAreProvided() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        final var bruce = new User()
                .userId( "111" )
                .email( "bruce.wayne@gotham.city" )
                .displayName( "Batman" );

        Mockito.doReturn( bruce ).when( usersService ).fetchUserDetails( "111" );

        HttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, null));
    }

    @Test
    void preHandleShouldReturnTrueWhenInternalUser() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();

        when(internalUserInterceptor.preHandle(request, response, null)).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, null));
    }

}