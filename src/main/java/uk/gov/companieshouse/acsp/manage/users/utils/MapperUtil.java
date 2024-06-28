package uk.gov.companieshouse.acsp.manage.users.utils;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembers;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembersLinks;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;

@Component
public class MapperUtil {

    private final UsersService usersService;

    private final AcspDataService acspDataService;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @Autowired
    public MapperUtil( final UsersService usersService, final AcspDataService acspDataService ) {
        this.usersService = usersService;
        this.acspDataService = acspDataService;
    }

    public AcspMembership enrichAcspMembershipWithUserDetails( final AcspMembership acspMembership ) {
        final var userId = acspMembership.getUserId();
        final var userDetails = usersService.fetchUserDetails( userId );
        final var userEmail = userDetails.getEmail();
        final var displayName =
                Optional.ofNullable( userDetails.getDisplayName() )
                        .orElse( DEFAULT_DISPLAY_NAME );
        acspMembership.setUserEmail( userEmail );
        acspMembership.setUserDisplayName( displayName );
        return acspMembership;
    }

    public AcspMembership enrichAcspMembershipWithAcspData( final AcspMembership acspMembership ) {
        final var acspNumber = acspMembership.getAcspNumber();
        final var acspData = acspDataService.fetchAcspData( acspNumber );
        final var acspName = acspData.getAcspName();
        final var acspStatus = acspData.getAcspStatus();
        acspMembership.setAcspName( acspName );
        acspMembership.setAcspStatus( AcspStatusEnum.fromValue( acspStatus ) );
        return acspMembership;
    }

    public AcspMembers enrichWithMetadata( final Page<AcspMembership> page, final String endpointUrl ) {
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var self = String.format( "%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex, itemsPerPage );
        final var next = page.isLast() ? "" : String.format( "%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex + 1, itemsPerPage );
        final var links = new AcspMembersLinks().self( self ).next( next );
        return new AcspMembers()
                .items( page.getContent() )
                .pageNumber( pageIndex )
                .itemsPerPage( itemsPerPage )
                .totalResults( (int) page.getTotalElements() )
                .totalPages( page.getTotalPages() )
                .links( links );
    }

}
