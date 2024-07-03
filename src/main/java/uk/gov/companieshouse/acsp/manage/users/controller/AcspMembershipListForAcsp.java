package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipListForAcspInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembers;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class AcspMembershipListForAcsp implements AcspMembershipListForAcspInterface {

    private final UsersService usersService;
    private final AcspDataService acspDataService;
    private final AcspMembersService acspMembersService;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private static final String PAGE_INDEX_WAS_LESS_THEN_0 = "pageIndex was less then 0";
    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    private static final String ITEMS_PER_PAGE_WAS_LESS_THEN_0 = "itemsPerPage was less then 0";

    public AcspMembershipListForAcsp( final UsersService usersService, final AcspDataService acspDataService, final AcspMembersService acspMembersService) {
        this.usersService = usersService;
        this.acspDataService = acspDataService;
        this.acspMembersService = acspMembersService;
    }

    @Override
    public ResponseEntity<AcspMembers> getMembersForAcsp( final String acspNumber, final String xRequestId, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage, final String userEmail, final String role ) {

        LOG.infoContext( xRequestId, String.format( "Attempting to fetch members for Acsp %s", acspNumber ), null );

        final var roleIsValid =
        Optional.ofNullable( role )
                .map( theRole -> Arrays.stream( UserRoleEnum.values() )
                        .map( UserRoleEnum::getValue )
                        .anyMatch( validRole -> validRole.equals(theRole) ) )
                .orElse( true );

        if ( !roleIsValid ){
            LOG.error( String.format( "Role was invalid: %s", role ) );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( pageIndex < 0 ) {
            LOG.error( PAGE_INDEX_WAS_LESS_THEN_0 );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( itemsPerPage <= 0 ) {
            LOG.error( ITEMS_PER_PAGE_WAS_LESS_THEN_0 );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        String userId = null;
        if ( Objects.nonNull( userEmail ) ) {
            final var usersList =
            Optional.ofNullable( usersService.searchUserDetails( List.of( userEmail ) ) )
                    .filter( users -> !users.isEmpty() )
                    .orElseThrow( () -> {
                        LOG.error( String.format( "User %s was not found", userEmail ) );
                        return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                    } );
            userId = usersList.getFirst().getUserId();
        }

        final var acspData = acspDataService.fetchAcspData( acspNumber );

        final var acspMembers = acspMembersService.fetchAcspMembers( acspData, includeRemoved, userId, role, pageIndex, itemsPerPage );

        LOG.infoContext( xRequestId, String.format( "Successfully fetched members for Acsp %s", acspNumber ), null );

        return new ResponseEntity<>( acspMembers, HttpStatus.OK );
    }

}
