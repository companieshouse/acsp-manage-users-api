package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class AcspMembershipController implements AcspMembershipInterface {

    private final AcspMembersService acspMembershipService;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

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
    public ResponseEntity<Void> updateAcspMembershipForAcspAndId(@NotNull String s,
            @Pattern(regexp = "^[a-zA-Z0-9]*$") String s1,
            @Valid RequestBodyPatch requestBodyPatch) {
        return null;
    }

}
