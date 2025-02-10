package uk.gov.companieshouse.acsp.manage.users.model;

import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_TOKEN_PERMISSIONS;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public final class RequestContext {

    private static final ThreadLocal<Map<String, String>> requestDetailsThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<User> userContextThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> ericAuthorisedRolesThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> ericAuthorisedTokenPermissionsThreadLocal = new ThreadLocal<>();

    public static final class RequestDetailsContext {

        private static final String X_REQUEST_ID = "X-Request-Id";
        private static final String UNKNOWN = "unknown";
        private static final String OAUTH2_REQUEST_TYPE = "oauth2";

        public static void setRequestDetails( final HttpServletRequest request ){
            final var xRequestId = Optional.ofNullable( request )
                    .map( req -> req.getHeader( X_REQUEST_ID ) )
                    .orElse( UNKNOWN );

            final var ericIdentityType = Optional.ofNullable( request )
                    .map( req -> req.getHeader( ERIC_IDENTITY_TYPE ) )
                    .orElse( UNKNOWN );

            requestDetailsThreadLocal.set( Map.of( X_REQUEST_ID, xRequestId, ERIC_IDENTITY_TYPE, ericIdentityType ) );
        }

        public static String getXRequestId() {
            return Optional.of( requestDetailsThreadLocal )
                    .map( ThreadLocal::get )
                    .map( requestDetails -> requestDetails.get( X_REQUEST_ID ) )
                    .orElse( UNKNOWN );
        }

        public static boolean isOAuth2Request() {
            final var ericIdentityType = Optional.of( requestDetailsThreadLocal )
                    .map( ThreadLocal::get )
                    .map( requestDetails -> requestDetails.get( ERIC_IDENTITY_TYPE ) )
                    .orElse( UNKNOWN );
            return ericIdentityType.equals( OAUTH2_REQUEST_TYPE );
        }

    }

    public static final class UserContext {

        public static void setLoggedUser( final User user ) {
            userContextThreadLocal.set( user );
        }

        public static User getLoggedUser() {
            return userContextThreadLocal.get();
        }

    }

    public static final class EricAuthorisedRolesContext {

        private static final String ADMIN_ACSP_SEARCH_PERMISSION = "/admin/acsp/search";

        public static void setEricAuthorisedRoles( final HttpServletRequest request ){
            final var ericAuthorisedRoles = Optional.ofNullable( request )
                    .map( req -> req.getHeader( ERIC_AUTHORISED_ROLES ) )
                    .orElse( "" );
            ericAuthorisedRolesThreadLocal.set( ericAuthorisedRoles );
        }

        private static boolean hasPermission( final String permission ){
            return Optional.ofNullable( ericAuthorisedRolesThreadLocal.get() )
                    .map( roles -> roles.split( " " ) )
                    .map( Arrays::asList )
                    .orElse( List.of() )
                    .contains( permission );
        }

        public static boolean hasAdminAcspSearchPermission(){
            return hasPermission( ADMIN_ACSP_SEARCH_PERMISSION );
        }

    }

    public static final class EricAuthorisedTokenPermissionsContext {

        private static final Pattern ACSP_NUMBER_PATTERN = Pattern.compile( "(?<=^|\\s)acsp_number=([0-9A-Za-z-_]{0,32})(?=\\s|$)" );
        private static final String ACSP_MEMBERS_OWNERS = "acsp_members_owners=create,update,delete";
        private static final String ACSP_MEMBERS_ADMINS = "acsp_members_admins=create,update,delete";
        private static final String ACSP_MEMBERS_STANDARD = "acsp_members_standard=create,update,delete";
        private static final String ACSP_MEMBERS_READ_PERMISSION = "acsp_members=read";

        public static void setEricAuthorisedTokenPermissions( final HttpServletRequest request ){
            final var ericAuthorisedTokenPermissions = Optional.ofNullable( request )
                    .map( req -> req.getHeader( ERIC_AUTHORISED_TOKEN_PERMISSIONS ) )
                    .orElse( "" );
            ericAuthorisedTokenPermissionsThreadLocal.set( ericAuthorisedTokenPermissions );
        }

        public static boolean ericAuthorisedTokenPermissionsAreValid( final AcspMembersService acspMembersService, final String userId ){
            final var acspNumber = fetchRequestingUsersActiveAcspNumber();
            final var requestingUsersActiveMembershipOptional = acspMembersService.fetchActiveAcspMembership( userId, acspNumber );
            if ( requestingUsersActiveMembershipOptional.isEmpty() ){
                return false;
            }
            final var requestingUsersActiveMembership = requestingUsersActiveMembershipOptional.get();

            final var currentUserRole = UserRoleEnum.fromValue( requestingUsersActiveMembership.getUserRole() );
            final var sessionUserRole = fetchRequestingUsersRole();
            return currentUserRole.equals( sessionUserRole );
        }

        public static String fetchRequestingUsersActiveAcspNumber(){
            final var ericAuthorisedTokenPermissions = ericAuthorisedTokenPermissionsThreadLocal.get();
            final var matcher = ACSP_NUMBER_PATTERN.matcher( ericAuthorisedTokenPermissions );
            return matcher.find() ? matcher.group( 1 ) : null;
        }

        private static boolean hasPermission( final String permission ){
            return Optional.ofNullable( ericAuthorisedTokenPermissionsThreadLocal.get() )
                    .map( roles -> roles.split( " " ) )
                    .map( Arrays::asList )
                    .orElse( List.of() )
                    .contains( permission );
        }

        public static boolean requestingUserIsPermittedToRetrieveAcspData(){
            return hasPermission( ACSP_MEMBERS_READ_PERMISSION );
        }

        public static boolean requestingUserCanManageMembership( final UserRoleEnum role ){
            return switch ( role ){
                case UserRoleEnum.OWNER -> hasPermission( ACSP_MEMBERS_OWNERS );
                case UserRoleEnum.ADMIN -> hasPermission( ACSP_MEMBERS_ADMINS );
                case UserRoleEnum.STANDARD -> hasPermission( ACSP_MEMBERS_STANDARD );
            };
        }

        public static boolean requestingUserIsActiveMemberOfAcsp( final String acspNumber ){
            return acspNumber.equals( fetchRequestingUsersActiveAcspNumber() );
        }

        public static UserRoleEnum fetchRequestingUsersRole(){
            if ( requestingUserCanManageMembership( OWNER ) ){
                return OWNER;
            } else if ( requestingUserCanManageMembership( ADMIN ) ){
                return ADMIN;
            } else if ( requestingUserIsPermittedToRetrieveAcspData() ){
                return STANDARD;
            }
            return null;
        }

    }

    public static void clear(){
        requestDetailsThreadLocal.remove();
        userContextThreadLocal.remove();
        ericAuthorisedRolesThreadLocal.remove();
        ericAuthorisedTokenPermissionsThreadLocal.remove();
    }

}
