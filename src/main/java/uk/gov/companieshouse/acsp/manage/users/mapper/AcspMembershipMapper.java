package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;

@Mapper( componentModel = "spring" )
public abstract class AcspMembershipMapper {

    @Autowired
    protected UsersService usersService;

    @Autowired
    protected AcspDataService acspDataService;

    private final String DEFAULT_DISPLAY_NAME = "Not Provided";

    protected OffsetDateTime localDateTimeToOffsetDateTime( final LocalDateTime localDateTime ) {
        return Objects.isNull( localDateTime ) ? null : OffsetDateTime.of( localDateTime, ZoneOffset.UTC );
    }

    @AfterMapping
    protected void enrichWithUserDetails( @MappingTarget final AcspMembership acspMembership, @Context User userDetails ){
        if ( Objects.isNull( userDetails ) ){
            userDetails = usersService.fetchUserDetails( acspMembership.getUserId() );
        }
        acspMembership.setUserEmail( userDetails.getEmail() );
        acspMembership.setUserDisplayName( Objects.isNull( userDetails.getDisplayName() ) ? DEFAULT_DISPLAY_NAME : userDetails.getDisplayName() );
    }

    @AfterMapping
    protected void enrichWithAcspDetails( @MappingTarget final AcspMembership acspMembership, @Context AcspDataDao acspDetails ){
        if ( Objects.isNull( acspDetails ) ){
            acspDetails = acspDataService.fetchAcspData( acspMembership.getAcspNumber() );
        }
        acspMembership.setAcspName( acspDetails.getAcspName() );
        acspMembership.setAcspStatus( AcspStatusEnum.fromValue( acspDetails.getAcspStatus() ) );
    }

    @Mapping( target = "userRole", expression = "java(AcspMembership.UserRoleEnum.fromValue(acspMembersDao.getUserRole()))" )
    @Mapping( target = "membershipStatus", expression = "java(AcspMembership.MembershipStatusEnum.fromValue(acspMembersDao.getStatus()))" )
    public abstract AcspMembership daoToDto( final AcspMembersDao acspMembersDao, @Context final User user, @Context final AcspDataDao acspData );

}