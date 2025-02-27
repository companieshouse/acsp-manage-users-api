package uk.gov.companieshouse.acsp.manage.users.membership.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.acsp.manage.users.common.model.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.canChangeRole;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.canRemoveMembership;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.isActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acspprofile.Status.CEASED;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.ascpprofile.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.email.EmailService;
import uk.gov.companieshouse.acsp.manage.users.membership.MembershipService;
import uk.gov.companieshouse.acsp.manage.users.user.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;

@RestController
public class AcspMembershipExternalController implements AcspMembershipInterface {

    private final MembershipService acspMembersService;
    private final EmailService emailService;
    private final UsersService usersService;
    private final AcspProfileService acspProfileService;

    public AcspMembershipExternalController( final MembershipService acspMembersService, final EmailService emailService, final UsersService usersService, final AcspProfileService acspProfileService ) {
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

        final var targetUserIsLastOwner = !targetAcsp.getStatus().equals( CEASED ) && OWNER.equals( targetMembership.getUserRole() ) && acspMembersService.fetchNumberOfActiveOwners( targetMembership.getAcspNumber() ) <= 1;
        final var requestingUserIsNotActiveMemberOfTargetAcsp = !isActiveMemberOfAcsp( targetMembership.getAcspNumber() );
        final var requestingUserAttemptingToRemoveWithoutAuthority = Objects.nonNull( proposedUserStatus ) && !canRemoveMembership( targetMembership.getUserRole() );
        final var requestingUserAttemptingToChangeRoleWithoutAuthority = Objects.nonNull( proposedUserRole ) && !canChangeRole( targetMembership.getUserRole(), proposedUserRole );
        if ( targetUserIsLastOwner || ( isOAuth2Request() && ( requestingUserIsNotActiveMemberOfTargetAcsp || requestingUserAttemptingToRemoveWithoutAuthority || requestingUserAttemptingToChangeRoleWithoutAuthority ) ) ){
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
