package uk.gov.companieshouse.acsp.manage.users.controller;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canChangeRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.canRemoveMembership;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acspprofile.Status.CEASED;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
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
                .orElseThrow( () -> new NotFoundRuntimeException( String.format( "Could not find membership with id: %s", membershipId ), new Exception( String.format( "Could not find membership with id: %s", membershipId ) ) ) );
        LOG.infoContext( xRequestId, String.format( "Successfully fetched membership with id: %s", membershipId ), null );

        if ( isOAuth2Request() && !isActiveMemberOfAcsp( membership.getAcspNumber() ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Requesting user is not an active member of Acsp %s", membership.getAcspNumber() ) ) );
        }

        return new ResponseEntity<>( membership, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForAcspAndId( final String xRequestId, final String membershipId, final RequestBodyPatch requestBody ) {
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

        LOG.infoContext( getXRequestId(), String.format( "Received request with membership_id=%s, user_status=%s, user_role=%s ", membershipId, proposedUserStatus, proposedUserRole ), null );

        if ( Objects.isNull( proposedUserStatus ) && Objects.isNull( proposedUserRole ) ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Request body is empty" ) );
        }

        LOG.debugContext( getXRequestId(), String.format( "Attempting to fetch membership for id: %s", membershipId ), null );
        final var targetMembership = acspMembershipService
                .fetchMembershipDao( membershipId )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find Acsp Membership with id: %s", membershipId ) ) ) );

        final var targetAcsp = acspProfileService.fetchAcspProfile( targetMembership.getAcspNumber() );

        final var isLastOwner = !targetAcsp.getStatus().equals( CEASED ) && OWNER.equals( targetMembership.getUserRole() ) && acspMembershipService.fetchNumberOfActiveOwners( targetMembership.getAcspNumber() ) <= 1;
        final var userIsNotActiveMemberOfTargetAcsp = !isActiveMemberOfAcsp( targetMembership.getAcspNumber() );
        final var userAttemptingToRemoveWithoutAuthority = Objects.nonNull( proposedUserStatus ) && !canRemoveMembership( targetMembership.getUserRole() );
        final var userAttemptingToChangeRoleWithoutAuthority = Objects.nonNull( proposedUserRole ) && !canChangeRole( targetMembership.getUserRole(), proposedUserRole );
        if ( isLastOwner || ( isOAuth2Request() && ( userIsNotActiveMemberOfTargetAcsp || userAttemptingToRemoveWithoutAuthority || userAttemptingToChangeRoleWithoutAuthority ) ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "User is not permitted to carry out action" ) );
        }

        LOG.debugContext( getXRequestId(), String.format( "Attempting to update membership for id: %s", membershipId ), null );
        acspMembershipService.updateMembership( membershipId, proposedUserStatus, proposedUserRole, isOAuth2Request() ? getEricIdentity() : null );
        LOG.infoContext( getXRequestId(), String.format( "Successfully updated Acsp Membership with id: %s", membershipId ), null );

        if ( isOAuth2Request() && Objects.nonNull( proposedUserRole ) ){
            final var requestingUserDisplayName = Optional.ofNullable( getUser().getDisplayName() ).orElse( getUser().getEmail() );
            final var targetUser = usersService.fetchUserDetails( targetMembership.getUserId() );
            emailService.sendYourRoleAtAcspHasChangedEmail( getXRequestId(), targetUser.getEmail(), requestingUserDisplayName, targetAcsp.getName(), proposedUserRole );
        }

        return new ResponseEntity<>( HttpStatus.OK );
    }

}
