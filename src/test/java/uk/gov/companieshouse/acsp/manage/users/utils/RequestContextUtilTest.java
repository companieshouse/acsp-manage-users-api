package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToCreateMembershipWith;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToRemoveUsersWith;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToRetrieveAcspData;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToUpdateUsersWith;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.InvalidTokenPermissionException;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RequestContextUtilTest {

    @Test
    void isOAuth2RequestReturnsTrueIfEricIdentityTypeIsOAuth2(){
        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY_TYPE, "oauth2" );

        final var requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertTrue( isOAuth2Request() );
    }

    @Test
    void isOAuth2RequestReturnsFalseIfEricIdentityTypeIsNotOAuth2(){
        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY_TYPE, "key" );

        final var requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( isOAuth2Request() );
    }

    @Test
    void requestingUserIsPermittedToRetrieveAcspDataWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_members=read" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertTrue( requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void requestingUserIsPermittedToRetrieveAcspDataWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToRetrieveAcspData() );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsActiveMemberOfAcsp( "WITA001" ) );
    }

    @Test
    void requestingUserIsActiveMemberOfAcspWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_id=WITA001" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertTrue( requestingUserIsActiveMemberOfAcsp( "WITA001" ) );
    }

    @Test
    void requestingUserIsPermittedToCreateMembershipWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToCreateMembershipWith( UserRoleEnum.STANDARD ) );
    }

    @Test
    void requestingUserIsPermittedToCreateMembershipWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_members_standard=create" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertTrue( requestingUserIsPermittedToCreateMembershipWith( UserRoleEnum.STANDARD ) );
    }

    @Test
    void requestingUserIsPermittedToUpdateUsersWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToUpdateUsersWith( UserRoleEnum.STANDARD ) );
    }

    @Test
    void requestingUserIsPermittedToUpdateUsersWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_members_admin=update" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToUpdateUsersWith( UserRoleEnum.ADMIN ) );
    }

    @Test
    void requestingUserIsPermittedToRemoveUsersWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToRemoveUsersWith( UserRoleEnum.OWNER ) );
    }

    @Test
    void requestingUserIsPermittedToRemoveUsersWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "Eric-Authorised-Token-Permissions", "acsp_members_owner=delete" );
        new TokenPermissionsInterceptor().preHandle( request, null, null );

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes( request );
        RequestContextHolder.setRequestAttributes( requestAttributes );

        Assertions.assertFalse( requestingUserIsPermittedToRemoveUsersWith( UserRoleEnum.OWNER ) );
    }

}
