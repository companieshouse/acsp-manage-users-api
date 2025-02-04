package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class CompositeAuthorizationInterceptorTest {

    @Mock
    private UsersService usersService;

    private CompositeAuthorizationInterceptor authorizationInterceptor;

    private CompositeAuthorizationInterceptor authorizationAndInternalUserInterceptors;

    @BeforeEach
    void setup(){
        authorizationInterceptor = new CompositeAuthorizationInterceptor( true, false, usersService );
        authorizationAndInternalUserInterceptors = new CompositeAuthorizationInterceptor( true, true, usersService );
    }

    @Test
    void preHandleWithoutEricHeadersReturnsUnauthorised() {
        final var request = new MockHttpServletRequest();

        final var response = new MockHttpServletResponse();
        assertFalse( authorizationInterceptor.preHandle( request, response, null ) );
        assertEquals(401, response.getStatus() );
    }

    @Test
    void preHandleWithRequestForAuthorizationTypeThatIsUnspecifiedReturnsFalse() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles","*");

        final var response = new MockHttpServletResponse();
        assertFalse( authorizationInterceptor.preHandle( request, response, null ) );
    }

    @Test
    void preHandleWithOAuth2RequestWithoutEricIdentityReturnsUnauthorised() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity-type", "oauth2");

        final var response = new MockHttpServletResponse();
        assertFalse( authorizationInterceptor.preHandle( request, response, null ) );
        assertEquals(401, response.getStatus() );
    }

    @Test
    void preHandleWithOAuth2RequestWithNonexistentUserReturnsForbidden() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Not found" ) ).when( usersService ).fetchUserDetails( "111" );

        final var response = new MockHttpServletResponse();
        assertFalse( authorizationInterceptor.preHandle( request, response, null ) );
        assertEquals(403, response.getStatus() );
    }

    @Test
    void preHandleWithOAuth2RequestWithExistingUserReturnsTrue() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        final var bruce = new User()
                .userId( "111" )
                .email( "bruce.wayne@gotham.city" )
                .displayName( "Batman" );

        Mockito.doReturn( bruce ).when( usersService ).fetchUserDetails( "111" );

        final var response = new MockHttpServletResponse();
        assertTrue( authorizationInterceptor.preHandle( request, response, null ) );
    }

    @Test
    void preHandleWithMalformedApiKeyRequestReturnsFalse() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles","*");

        final var response = new MockHttpServletResponse();
        assertFalse( authorizationAndInternalUserInterceptors.preHandle( request, response, null ) );
    }

    @Test
    void preHandleWithWellFormedApiKeyRequestReturnsTrue() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles","*");

        final var response = new MockHttpServletResponse();
        assertTrue( authorizationAndInternalUserInterceptors.preHandle( request, response, null ) );
    }

}
