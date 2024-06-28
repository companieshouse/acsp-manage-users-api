package uk.gov.companieshouse.acsp.manage.users.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembers;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;

@Component
public class AcspMembersMapper {

    private final BaseMapper baseMapper;
    private final MapperUtil mapperUtil;

    private static final String END_POINT_URL_TEMPLATE = "/acsp-members/acsps/%s";

    public AcspMembersMapper( final BaseMapper baseMapper, final MapperUtil mapperUtil ) {
        this.baseMapper = baseMapper;
        this.mapperUtil = mapperUtil;
    }

    public AcspMembers daoToDto( final Page<AcspMembersDao> acspMembersDaos, final AcspDataDao acspDataDao ){
        final var dtos =
        acspMembersDaos.map( baseMapper::daoToDto )
                       .map( mapperUtil::enrichAcspMembershipWithUserDetails )
                       .map( dto -> {
                           dto.setAcspName( acspDataDao.getAcspName() );
                           dto.setAcspStatus( AcspStatusEnum.fromValue( acspDataDao.getAcspStatus() ) );
                           return dto;
                       } );
        return mapperUtil.enrichWithMetadata( dtos, String.format( END_POINT_URL_TEMPLATE, acspDataDao.getId() ) );
    }

}
