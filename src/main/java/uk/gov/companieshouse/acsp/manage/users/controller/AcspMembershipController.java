package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
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

        final var requestingUser = UserContext.getLoggedUser();
        final var requestingUserId = Optional.ofNullable( requestingUser ).map( User::getUserId ).orElse( null );

        acspMembershipService.updateMembership( membershipId, userStatus, userRole, requestingUserId );

        LOG.infoContext( xRequestId, String.format( "Successfully updated Acsp Membership %s", membershipId ), null );

        return new ResponseEntity<>( HttpStatus.OK );
    }

}
