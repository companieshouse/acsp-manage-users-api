package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsListLinks;

@Component
public class AcspMembershipCollectionMappers {

    private final AcspMembershipMapper baseMapper;

    private static final String END_POINT_URL_TEMPLATE = "/acsps/%s/memberships";

    public AcspMembershipCollectionMappers( final AcspMembershipMapper baseMapper ) {
        this.baseMapper = baseMapper;
    }

    private AcspMembershipsList enrichWithMetadata( final Page<AcspMembership> page, final String endpointUrl ) {
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var self = String.format( "%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex, itemsPerPage );
        final var next = page.isLast() ? "" : String.format( "%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex + 1, itemsPerPage );
        final var previous = page.isFirst() ? "" : String.format( "%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex - 1, itemsPerPage );
        final var links = new AcspMembershipsListLinks().self( self ).next( next ).previous( previous );

        return new AcspMembershipsList()
                .items( page.getContent() )
                .pageNumber( pageIndex )
                .itemsPerPage( itemsPerPage )
                .totalResults( (int) page.getTotalElements() )
                .totalPages( page.getTotalPages() )
                .links( links );
    }

    public List<AcspMembership> daoToDto( final List<AcspMembersDao> acspMembers, final User userData, final AcspDataDao acspData ){
        return acspMembers.stream()
                .map( dao -> baseMapper.daoToDto( dao, userData, acspData ) )
                .collect( Collectors.toList() );
    }

    public AcspMembershipsList daoToDto( final Page<AcspMembersDao> acspMembers, final User userData, final AcspDataDao acspData ){
        if ( Objects.isNull( acspData ) ){
            throw new IllegalArgumentException( "AcspData cannot be null." );
        }
        final var acspMemberships = acspMembers.map( dao -> baseMapper.daoToDto( dao, userData, acspData ) );
        return enrichWithMetadata( acspMemberships, String.format( END_POINT_URL_TEMPLATE, acspData.getId() ) );
    }

}