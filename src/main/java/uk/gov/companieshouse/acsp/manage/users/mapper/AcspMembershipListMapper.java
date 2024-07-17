package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Component
public class AcspMembershipListMapper {

    private final BaseMapper baseMapper;

    private final MapperUtil mapperUtil;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    public AcspMembershipListMapper( final BaseMapper baseMapper, final MapperUtil mapperUtil ) {
        this.baseMapper = baseMapper;
        this.mapperUtil = mapperUtil;
    }

    public List<AcspMembership> daoToDto( final List<AcspMembersDao> acspMembersDaos, final User userData ){
        return acspMembersDaos.stream()
                .map( baseMapper::daoToDto )
                .map( mapperUtil::enrichAcspMembershipWithAcspData )
                .map( dto -> {
                    dto.setUserEmail( userData.getEmail() );
                    dto.setUserDisplayName( Optional.ofNullable( userData.getDisplayName() ).orElse( DEFAULT_DISPLAY_NAME ) );
                    return dto; } )
                .collect( Collectors.toList() );
    }

}