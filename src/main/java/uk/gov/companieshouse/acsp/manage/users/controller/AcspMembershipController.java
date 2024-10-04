package uk.gov.companieshouse.acsp.manage.users.controller;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.fetchRequestingUsersRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToRemoveUsersWith;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.requestingUserIsPermittedToUpdateUsersWith;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
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

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";

    public AcspMembershipController( final AcspMembersService acspMembershipService ) {
        this.acspMembershipService = acspMembershipService;
    }

    @Override
    public ResponseEntity<AcspMembership> getAcspMembershipForAcspAndId( final String xRequestId, final String membershipId ) {

        LOG.infoContext( xRequestId, String.format( "Attempting to fetch membership %s", membershipId ), null );
        final var membership = acspMembershipService.fetchMembership( membershipId ).orElseThrow( () -> new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, String.format( "Could not find membership %s", membershipId ) ));
        LOG.infoContext( xRequestId, String.format( "Successfully fetched membership %s", membershipId ), null );

        return new ResponseEntity<>( membership, HttpStatus.OK );
    }

    private void throwBadRequestWhenActionIsNotPermittedByOAuth2User( final String requestingUserId, final AcspMembersDao membershipIdAssociation, final UserRoleEnum userRole, final UserStatusEnum userStatus ){
        final var targetUserId = membershipIdAssociation.getUserId();
        final var targetAcspNumber = membershipIdAssociation.getAcspNumber();
        final var targetUsersRole = UserRoleEnum.fromValue( membershipIdAssociation.getUserRole() );

        if ( !requestingUserIsActiveMemberOfAcsp( targetAcspNumber ) ){
            LOG.error( String.format( "Could not find %s's Acsp Membership at Acsp %s", requestingUserId, targetAcspNumber ) );
            throw new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( Objects.nonNull( userStatus ) && !requestingUserIsPermittedToRemoveUsersWith( targetUsersRole ) ){
            LOG.error( String.format( "User is not permitted to remove user %s", targetUserId ) );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( Objects.nonNull( userRole ) ){
            final var requestingUserIsNotPermittedToUpdateTargetUser = !requestingUserIsPermittedToUpdateUsersWith( targetUsersRole );
            final var requestingUserIsAdmin = UserRoleEnum.ADMIN.equals( fetchRequestingUsersRole() );
            final var attemptingToChangeTargetUsersRoleToOwner = UserRoleEnum.OWNER.equals( userRole );
            if ( requestingUserIsNotPermittedToUpdateTargetUser || ( requestingUserIsAdmin && attemptingToChangeTargetUsersRoleToOwner ) ){
                LOG.error( String.format( "User is not permitted to change role of user %s to %s", targetUserId, userRole.getValue() ) );
                throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
            }
        }

    }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForAcspAndId( final String xRequestId, final String membershipId, final RequestBodyPatch requestBody ) {

        LOG.infoContext( xRequestId, String.format( "Attempting to update Acsp Membership %s", membershipId ), null );

        if ( Objects.isNull( requestBody ) || ( Objects.isNull( requestBody.getUserStatus() ) && Objects.isNull( requestBody.getUserRole() ) ) ){
            LOG.error( "Request body is empty" );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }
        final var userStatus = requestBody.getUserStatus();

        final var userRole =
                Optional.ofNullable( requestBody.getUserRole() )
                        .map( RequestBodyPatch.UserRoleEnum::getValue )
                        .map( UserRoleEnum::fromValue )
                        .orElse( null );

        final var membershipIdAssociation =
        acspMembershipService.fetchMembershipDao( membershipId )
                .orElseThrow( () -> {
                    LOG.error( String.format( "Could not find Acsp Membership %s", membershipId ) );
                    return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                } );

        if ( UserRoleEnum.OWNER.getValue().equals( membershipIdAssociation.getUserRole() ) && acspMembershipService.fetchNumberOfActiveOwners( membershipIdAssociation.getAcspNumber() ) <= 1 ){
            LOG.error( String.format( "Acsp Membership %s is the last owner", membershipId ) );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        final var requestingUser = UserContext.getLoggedUser();
        final var requestingUserId = Optional.ofNullable( requestingUser ).map( User::getUserId ).orElse( null );
        if ( isOAuth2Request() ){
            throwBadRequestWhenActionIsNotPermittedByOAuth2User( requestingUserId, membershipIdAssociation, userRole, userStatus );
        }

        acspMembershipService.updateMembership( membershipId, userStatus, userRole, requestingUserId );

        LOG.infoContext( xRequestId, String.format( "Successfully updated Acsp Membership %s", membershipId ), null );

        return new ResponseEntity<>( HttpStatus.OK );
    }

}
