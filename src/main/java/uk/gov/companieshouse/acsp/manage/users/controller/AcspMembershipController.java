package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.acsp.manage.users.model.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canChangeRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canRemoveMembership;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.ACTIVE;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.PENDING;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acspprofile.Status.CEASED;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;

@RestController
public class AcspMembershipController implements AcspMembershipInterface {

    private final AcspMembersService acspMembersService;
    private final EmailService emailService;
    private final UsersService usersService;
    private final AcspProfileService acspProfileService;

    public AcspMembershipController( final AcspMembersService acspMembersService, final EmailService emailService, final UsersService usersService, final AcspProfileService acspProfileService ) {
        this.acspMembersService = acspMembersService;
        this.emailService = emailService;
        this.usersService = usersService;
        this.acspProfileService = acspProfileService;
    }

    @Override
    public ResponseEntity<AcspMembership> getAcspMembershipForAcspAndId( final String xRequestId, final String membershipId ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with membership_id=%s", membershipId ), null );

        final var membership = acspMembersService
                .fetchMembership( membershipId )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find membership with id: %s", membershipId ) ) ) );

        if ( isOAuth2Request() && !isActiveMemberOfAcsp( membership.getAcspNumber() ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Requesting user is not an active member of Acsp %s", membership.getAcspNumber() ) ) );
        }

        return new ResponseEntity<>( membership, OK );
    }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForAcspAndId( final String xRequestId, final String targetMembershipId, final RequestBodyPatch requestBody ) {
        final var proposedUserStatus = Optional
                .ofNullable( requestBody )
                .map( RequestBodyPatch::getUserStatus )
                .map( userStatus -> switch ( userStatus ){
                    case APPROVED -> ACTIVE;
                    case REMOVED -> MembershipStatusEnum.REMOVED;
                } )
                .orElse( null );

        final var proposedUserRole = Optional
                .ofNullable( requestBody )
                .map( RequestBodyPatch::getUserRole )
                .map( RequestBodyPatch.UserRoleEnum::getValue )
                .map( UserRoleEnum::fromValue )
                .orElse( null );

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with membership_id=%s, user_status=%s, user_role=%s ", targetMembershipId, proposedUserStatus, proposedUserRole ), null );

        if ( Objects.isNull( proposedUserStatus ) && Objects.isNull( proposedUserRole ) ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Request body is empty" ) );
        }

        final var targetMembership = acspMembersService
                .fetchMembershipDao( targetMembershipId )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find Acsp Membership with id: %s", targetMembershipId ) ) ) );

        final var targetAcsp = acspProfileService.fetchAcspProfile( targetMembership.getAcspNumber() );

        final var targetUserIsLastOwner = !CEASED.equals( targetAcsp.getStatus() ) && OWNER.equals( targetMembership.getUserRole() ) && ACTIVE.getValue().equals( targetMembership.getStatus() ) && acspMembersService.fetchNumberOfActiveOwners( targetMembership.getAcspNumber() ) <= 1;
        final var requestingUserIsAttemptingToActivateWithoutAuthority = ACTIVE.equals( proposedUserStatus ) && ( !PENDING.getValue().equals( targetMembership.getStatus() ) || !isRequestingUser( targetMembership ) );
        final var requestingUserAttemptingToRemoveWithoutAuthority = AcspMembership.MembershipStatusEnum.REMOVED.equals( proposedUserStatus ) && ( !canRemoveMembership( targetMembership.getUserRole() ) || !isActiveMemberOfAcsp( targetMembership.getAcspNumber() ) );
        final var requestingUserAttemptingToChangeRoleWithoutAuthority = Objects.nonNull( proposedUserRole ) && ( !canChangeRole( targetMembership.getUserRole(), proposedUserRole ) || !isActiveMemberOfAcsp( targetMembership.getAcspNumber() ) );
        if ( targetUserIsLastOwner || ( isOAuth2Request() && ( requestingUserIsAttemptingToActivateWithoutAuthority || requestingUserAttemptingToRemoveWithoutAuthority || requestingUserAttemptingToChangeRoleWithoutAuthority ) ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "User is not permitted to carry out action" ) );
        }

        acspMembersService.updateMembership( targetMembershipId, proposedUserStatus, proposedUserRole, isOAuth2Request() ? getEricIdentity() : null );

        if ( isOAuth2Request() && Objects.nonNull( proposedUserRole ) ){
            final var requestingUserDisplayName = Optional.ofNullable( getUser().getDisplayName() ).orElse( getUser().getEmail() );
            final var targetUser = usersService.fetchUserDetails( targetMembership.getUserId() );
            emailService.sendYourRoleAtAcspHasChangedEmail( targetUser.getEmail(), requestingUserDisplayName, targetAcsp.getName(), proposedUserRole ).subscribe();
        }

        return new ResponseEntity<>( OK );
    }

}
