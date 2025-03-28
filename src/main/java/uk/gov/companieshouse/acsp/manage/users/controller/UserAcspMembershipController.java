package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

@RestController
public class UserAcspMembershipController implements UserAcspMembershipInterface {

    private final AcspMembersService acspMembersService;

    public UserAcspMembershipController( final AcspMembersService acspMembersService ) {
        this.acspMembersService = acspMembersService;
    }

    @Override
    public ResponseEntity<AcspMembershipsList> getAcspMembershipsForUserId( final String xRequestId, final String ericIdentity, final Boolean includeRemoved ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, include_removed=%b", getEricIdentity(), includeRemoved ), null );
        final var acspMemberships = acspMembersService.fetchMemberships( getUser(), includeRemoved, null );
        return new ResponseEntity<>( acspMemberships, OK );
    }

}
