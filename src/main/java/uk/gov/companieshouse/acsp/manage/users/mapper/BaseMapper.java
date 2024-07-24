package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Mapper( componentModel = "spring" )
public abstract class BaseMapper {

    protected OffsetDateTime localDateTimeToOffsetDateTime( final LocalDateTime localDateTime ) {
        return Objects.isNull( localDateTime ) ? null : OffsetDateTime.of( localDateTime, ZoneOffset.UTC );
    }

    @Mapping( target = "userRole", expression = "java(AcspMembership.UserRoleEnum.fromValue(acspMembersDao.getUserRole()))" )
    @Mapping( target = "membershipStatus", expression = "java(AcspMembership.MembershipStatusEnum.fromValue(acspMembersDao.getStatus()))" )
    public abstract AcspMembership daoToDto( final AcspMembersDao acspMembersDao );

}