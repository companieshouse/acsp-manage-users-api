package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.Permission.Key.ACSP_ID;
import static uk.gov.companieshouse.api.util.security.Permission.Key.ACSP_MEMBERS;
import static uk.gov.companieshouse.api.util.security.Permission.Key.ACSP_MEMBERS_ADMINS;
import static uk.gov.companieshouse.api.util.security.Permission.Key.ACSP_MEMBERS_OWNERS;
import static uk.gov.companieshouse.api.util.security.Permission.Key.ACSP_MEMBERS_STANDARD;
import static uk.gov.companieshouse.api.util.security.Permission.Value.CREATE;
import static uk.gov.companieshouse.api.util.security.Permission.Value.DELETE;
import static uk.gov.companieshouse.api.util.security.Permission.Value.READ;
import static uk.gov.companieshouse.api.util.security.Permission.Value.UPDATE;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.util.security.TokenPermissions;

public class RequestContextUtil {

    private static final String OAUTH2_REQUEST_TYPE = "oauth2";
    private static final String TOKEN_PERMISSIONS = "token_permissions";

    private RequestContextUtil() {
        throw new IllegalStateException( "Utility class" );
    }

    private static String getEricIdentityType() {
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ( (ServletRequestAttributes) requestAttributes );
        final var httpServletRequest = Objects.requireNonNull( servletRequestAttributes ).getRequest();
        return httpServletRequest.getHeader( ERIC_IDENTITY_TYPE );
    }

    public static boolean isOAuth2Request() {
        return getEricIdentityType().equals( OAUTH2_REQUEST_TYPE );
    }

    private static TokenPermissions getTokenPermissions(){
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ( (ServletRequestAttributes) requestAttributes );
        final var httpServletRequest = Objects.requireNonNull( servletRequestAttributes ).getRequest();
        return (TokenPermissions) httpServletRequest.getAttribute( TOKEN_PERMISSIONS );
    }

    public static boolean requestingUserIsPermittedToRetrieveAcspData(){
        final var tokenPermissions = getTokenPermissions();
        return tokenPermissions.hasPermission( ACSP_MEMBERS, READ );
    }

    public static boolean requestingUserIsActiveMemberOfAcsp( final String acspNumber ){
        final var tokenPermissions = getTokenPermissions();
        return tokenPermissions.hasPermission( ACSP_ID, acspNumber );
    }

    private static boolean requestingUserIsPermittedToPerformActionOnUserWithRole( final String action, final UserRoleEnum role ){
        final var tokenPermissions = getTokenPermissions();
        return switch ( role ){
            case UserRoleEnum.OWNER -> tokenPermissions.hasPermission( ACSP_MEMBERS_OWNERS, action );
            case UserRoleEnum.ADMIN -> tokenPermissions.hasPermission( ACSP_MEMBERS_ADMINS, action );
            case UserRoleEnum.STANDARD -> tokenPermissions.hasPermission( ACSP_MEMBERS_STANDARD, action );
        };
    }

    public static boolean requestingUserIsPermittedToCreateMembershipWith( final UserRoleEnum role ){
        return requestingUserIsPermittedToPerformActionOnUserWithRole( CREATE, role );
    }

    public static boolean requestingUserIsPermittedToUpdateUsersWith( final UserRoleEnum role ){
        return requestingUserIsPermittedToPerformActionOnUserWithRole( UPDATE, role );
    }

    public static boolean requestingUserIsPermittedToRemoveUsersWith( final UserRoleEnum role ){
        return requestingUserIsPermittedToPerformActionOnUserWithRole( DELETE, role );
    }

}
