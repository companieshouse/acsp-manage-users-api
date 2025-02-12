package uk.gov.companieshouse.acsp.manage.users.model;

import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public final class RequestContext {

    private static final RequestContextThreadLocal requestDetailsThreadLocal = new RequestContextThreadLocal();

    public static void setRequestDetails( final HttpServletRequest request ){
        requestDetailsThreadLocal.setRequestDetails( request );
    }

    public static final class RequestDetailsContext {

        private static final String OAUTH2_REQUEST_TYPE = "oauth2";

        public static String getXRequestId() {
            return requestDetailsThreadLocal.get().getXRequestId();
        }

        public static String getEricIdentity(){
            return requestDetailsThreadLocal.get().getEricIdentity();
        }

        public static boolean isOAuth2Request() {
            return requestDetailsThreadLocal.get().getEricIdentityType().equals( OAUTH2_REQUEST_TYPE );
        }

    }

    public static final class EricAuthorisedRolesContext {

        public static boolean hasAdminAcspSearchPermission(){
            return requestDetailsThreadLocal.get().getEricAuthorisedRoles().contains( "/admin/acsp/search" );
        }

    }

    public static final class EricAuthorisedTokenPermissionsContext {

        private static final Pattern ACSP_NUMBER_PATTERN = Pattern.compile( "(?<=^|\\s)acsp_number=([0-9A-Za-z-_]{0,32})(?=\\s|$)" );
        private static final String ACSP_MEMBERS_OWNERS = "acsp_members_owners=create,update,delete";
        private static final String ACSP_MEMBERS_ADMINS = "acsp_members_admins=create,update,delete";
        private static final String ACSP_MEMBERS_STANDARD = "acsp_members_standard=create,update,delete";
        private static final String ACSP_MEMBERS_READ_PERMISSION = "acsp_members=read";

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
            final var ericAuthorisedTokenPermissions = requestDetailsThreadLocal.get().getEricAuthorisedTokenPermissions();
            final var matcher = ACSP_NUMBER_PATTERN.matcher( ericAuthorisedTokenPermissions );
            return matcher.find() ? matcher.group( 1 ) : null;
        }

        private static boolean hasPermission( final String permission ){
            return Optional.ofNullable( requestDetailsThreadLocal.get() )
                    .map( RequestContextData::getEricAuthorisedTokenPermissions )
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

    public static final class UserContext {

        public static void setLoggedUser( final User user ) {
            requestDetailsThreadLocal.setLoggedUser( user );
        }

        public static User getLoggedUser() {
            return requestDetailsThreadLocal.get().getUser();
        }

    }

    public static void clear(){
        requestDetailsThreadLocal.remove();
    }

}
