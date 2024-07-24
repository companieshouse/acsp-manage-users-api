package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Component
public class AcspMembershipMapper {

    private final BaseMapper baseMapper;

    private final MapperUtil mapperUtil;

    public AcspMembershipMapper( final BaseMapper baseMapper, final MapperUtil mapperUtil ) {
        this.baseMapper = baseMapper;
        this.mapperUtil = mapperUtil;
    }

    public AcspMembership daoToDto( final AcspMembersDao acspMembersDao ){
        return Optional.ofNullable( acspMembersDao )
                .map( baseMapper::daoToDto )
                .map( mapperUtil::enrichAcspMembershipWithUserDetails )
                .map( mapperUtil::enrichAcspMembershipWithAcspData )
                .orElse( null );
    }

}