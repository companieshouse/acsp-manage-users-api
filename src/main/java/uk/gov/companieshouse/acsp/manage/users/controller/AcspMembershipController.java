package uk.gov.companieshouse.acsp.manage.users.controller;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersAcspRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.fetchRequestingUsersActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserCanManageMembership;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestUtil.requestingUserIsActiveMemberOfAcsp;
import static uk.gov.companieshouse.api.acspprofile.Status.CEASED;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class AcspMembershipController implements AcspMembershipInterface {

    private final AcspMembersService acspMembershipService;
    private final EmailService emailService;
    private final UsersService usersService;
    private final AcspProfileService acspProfileService;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";

    public AcspMembershipController( final AcspMembersService acspMembershipService, final EmailService emailService, final UsersService usersService, final AcspProfileService acspProfileService ) {
        this.acspMembershipService = acspMembershipService;
        this.emailService = emailService;
        this.usersService = usersService;
        this.acspProfileService = acspProfileService;
    }

    @Override
    public ResponseEntity<AcspMembership> getAcspMembershipForAcspAndId( final String xRequestId, final String membershipId ) {

        LOG.infoContext( xRequestId, String.format( "Received request with membership_id=%s", membershipId ), null );

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch membership for id: %s", membershipId), null );
        final var membership = acspMembershipService
                .fetchMembership( membershipId )
                .orElseThrow( () -> {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Could not find membership with id: %s", membershipId ) ), null );
                    return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, String.format( "Could not find membership with id: %s", membershipId ) );
                } );
        LOG.infoContext( xRequestId, String.format( "Successfully fetched membership with id: %s", membershipId ), null );

        if ( isOAuth2Request() && !membership.getAcspNumber().equals( fetchRequestingUsersActiveAcspNumber() ) ){
            LOG.errorContext( getXRequestId(), new Exception( String.format( "Requesting user is not an active member of Acsp %s", membership.getAcspNumber() ) ), null );
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        return new ResponseEntity<>( membership, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForAcspAndId( final String xRequestId, final String membershipId, final RequestBodyPatch requestBody ) {

        if ( Objects.isNull( requestBody ) || ( Objects.isNull( requestBody.getUserStatus() ) && Objects.isNull( requestBody.getUserRole() ) ) ){
            LOG.errorContext( xRequestId, new Exception( "Request body is empty" ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }
        final var userStatus = requestBody.getUserStatus();

        final var userRole =
                Optional.ofNullable( requestBody.getUserRole() )
                        .map( RequestBodyPatch.UserRoleEnum::getValue )
                        .map( UserRoleEnum::fromValue )
                        .orElse( null );

        LOG.infoContext( xRequestId, String.format( "Received request with membership_id=%s, user_status=%s, user_role=%s ", membershipId, userStatus, userRole ), null );

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch membership for id: %s", membershipId ), null );
        final var membershipIdAssociation =
        acspMembershipService.fetchMembershipDao( membershipId )
                .orElseThrow( () -> {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Could not find Acsp Membership with id: %s", membershipId ) ), null );
                    return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                } );

        final var targetAcsp = acspProfileService.fetchAcspProfile( membershipIdAssociation.getAcspNumber() );
        final var isLastOwner = UserRoleEnum.OWNER.getValue().equals( membershipIdAssociation.getUserRole() ) && acspMembershipService.fetchNumberOfActiveOwners( membershipIdAssociation.getAcspNumber() ) <= 1;
        if ( isLastOwner && !targetAcsp.getStatus().equals( CEASED ) ){
            LOG.errorContext( xRequestId, new Exception( String.format( "Acsp Membership with %s is the last owner", membershipId ) ), null );
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        final var requestingUser = UserContext.getInstance().getRequestDetails();
        final var requestingUserId = Optional.ofNullable( requestingUser ).map( User::getUserId ).orElse( null );
        if ( isOAuth2Request() ){
            throwForbiddenWhenActionIsNotPermittedByOAuth2User( requestingUserId, membershipIdAssociation, userRole, userStatus );
        }

        LOG.debugContext( xRequestId, String.format( "Attempting to update membership for id: %s", membershipId ), null );
        acspMembershipService.updateMembership( membershipId, userStatus, userRole, requestingUserId );

        LOG.infoContext( xRequestId, String.format( "Successfully updated Acsp Membership with id: %s", membershipId ), null );

        if ( isOAuth2Request() && Objects.nonNull( userRole ) ){
          assert requestingUser != null;
          final var requestingUserDisplayName = Optional.ofNullable( requestingUser.getDisplayName() ).orElse( requestingUser.getEmail() );
            final var targetUser = usersService.fetchUserDetails( membershipIdAssociation.getUserId() );
            emailService.sendYourRoleAtAcspHasChangedEmail( xRequestId, targetUser.getEmail(), requestingUserDisplayName, targetAcsp.getName(), userRole );
        }

        return new ResponseEntity<>( HttpStatus.OK );
    }

    private void throwForbiddenWhenActionIsNotPermittedByOAuth2User( final String requestingUserId, final AcspMembersDao membershipIdAssociation, final UserRoleEnum userRole, final UserStatusEnum userStatus ){
        final var targetUserId = membershipIdAssociation.getUserId();
        final var targetAcspNumber = membershipIdAssociation.getAcspNumber();
        final var targetUsersRole = UserRoleEnum.fromValue( membershipIdAssociation.getUserRole() );

        if ( !requestingUserIsActiveMemberOfAcsp( targetAcspNumber ) ){
            LOG.errorContext( getXRequestId(), new Exception( String.format( "Could not find user %s's Acsp Membership at Acsp %s", requestingUserId, targetAcspNumber ) ), null );
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( Objects.nonNull( userStatus ) && !requestingUserCanManageMembership( targetUsersRole ) ){
            LOG.errorContext( getXRequestId(), new Exception( String.format( "User %s is not permitted to remove user %s", requestingUserId, targetUserId ) ), null );
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( Objects.nonNull( userRole ) ){
            final var requestingUserIsNotPermittedToUpdateTargetUser = !requestingUserCanManageMembership( targetUsersRole );
            final var requestingUserIsAdmin = UserRoleEnum.ADMIN.equals( fetchRequestingUsersAcspRole() );
            final var attemptingToChangeTargetUsersRoleToOwner = UserRoleEnum.OWNER.equals( userRole );
            if ( requestingUserIsNotPermittedToUpdateTargetUser || ( requestingUserIsAdmin && attemptingToChangeTargetUsersRoleToOwner ) ){
                LOG.errorContext( getXRequestId(), new Exception( String.format( "User %s is not permitted to change role of user %s to %s", requestingUserId, targetUserId, userRole.getValue() ) ), null );
                throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
            }
        }

    }

}
