package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getActiveAcspRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricAuthorisedKeyRoles;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentityType;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestLifecycleInterceptorTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private RequestLifecycleInterceptor requestLifecycleInterceptor;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void preHandleWithKeyRequestSetsRequestContextWithoutUserAndReturnsTrue(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "COMU002" );
        request.addHeader( "Eric-Identity-Type", "key" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );

        final var response = new MockHttpServletResponse();

        final var result = requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertTrue( result );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( "COMU002", getEricIdentity() );
        Assertions.assertEquals( "key", getEricIdentityType() );
        Assertions.assertEquals( "*", getEricAuthorisedKeyRoles() );
        Assertions.assertNull( getUser() );
        Assertions.assertEquals( 200, response.getStatus() );
    }

    @Test
    void preHandleWithOAuth2RequestSetsRequestContextWithUserAndReturnsTrue(){
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type", "oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) );

        final var response = new MockHttpServletResponse();

        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( user.getUserId() );

        final var result = requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertTrue( result );
        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( user.getUserId(), getEricIdentity() );
        Assertions.assertEquals( "oauth2", getEricIdentityType() );
        Assertions.assertEquals( OWNER, getActiveAcspRole() );
        Assertions.assertEquals( user, getUser() );
        Assertions.assertEquals( 200, response.getStatus() );
    }

    @Test
    void preHandleWithOAuth2RequestWithNonexistentUserDoesNotSetRequestContextAndReturnsFalse(){
        final var user = testDataManager.fetchUserDtos( "COMU002" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", user.getUserId() );
        request.addHeader( "Eric-Identity-Type", "oauth2" );
        request.addHeader( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) );

        final var response = new MockHttpServletResponse();

        Mockito.doThrow( new NotFoundRuntimeException( "Could not find user", new Exception( "Could not find user" ) ) ).when( usersService ).fetchUserDetails( user.getUserId() );

        final var result = requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertFalse( result );
        Assertions.assertEquals( "unknown", getXRequestId() );
        Assertions.assertEquals( "unknown", getEricIdentity() );
        Assertions.assertEquals( "unknown", getEricIdentityType() );
        Assertions.assertNull( getActiveAcspRole() );
        Assertions.assertNull( getUser() );
        Assertions.assertEquals( 403, response.getStatus() );
    }

    @Test
    void afterCompletionClearsRequestContext(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "COMU002" );
        request.addHeader( "Eric-Identity-Type", "key" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );

        final var response = new MockHttpServletResponse();

        requestLifecycleInterceptor.preHandle( request, response, null );

        Assertions.assertEquals( "theId123", getXRequestId() );
        Assertions.assertEquals( "COMU002", getEricIdentity() );
        Assertions.assertEquals( "key", getEricIdentityType() );
        Assertions.assertEquals( "*", getEricAuthorisedKeyRoles() );

        requestLifecycleInterceptor.afterCompletion( request, response, null, null );

        Assertions.assertEquals( "unknown", getXRequestId() );
        Assertions.assertEquals( "unknown", getEricIdentity() );
        Assertions.assertEquals( "unknown", getEricIdentityType() );
        Assertions.assertEquals( "unknown", getEricAuthorisedKeyRoles() );
    }

    @AfterEach
    void teardown(){
        RequestContext.clear();
    }

}
