package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipLinks;

@Mapper( componentModel = "spring" )
public abstract class BaseMapper {

    private static final String DEFAULT_KIND = "acsp-membership";

    protected OffsetDateTime localDateTimeToOffsetDateTime( final LocalDateTime localDateTime ) {
        return Objects.isNull( localDateTime ) ? null : OffsetDateTime.of( localDateTime, ZoneOffset.UTC );
    }

    @AfterMapping
    protected void enrichAcspMembershipWithKind( final @MappingTarget AcspMembership acspMembership ){
        acspMembership.setKind( DEFAULT_KIND );
    }

    @AfterMapping
    protected void enrichAcspMembershipWithLinks( final @MappingTarget AcspMembership acspMembership ){
        final var acspMembershipId = acspMembership.getId();
        final var self = String.format( "/%s", acspMembershipId );
        final var links = new AcspMembershipLinks().self( self );
        acspMembership.setLinks( links );
    }

    public abstract AcspMembership daoToDto( final AcspMembersDao acspMembersDao );

}
