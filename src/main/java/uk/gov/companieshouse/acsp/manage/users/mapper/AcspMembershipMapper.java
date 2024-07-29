package uk.gov.companieshouse.acsp.manage.users.mapper;

import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Component
public class AcspMembershipMapper {

  private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

  private final BaseMapper baseMapper;

  private final MapperUtil mapperUtil;

  public AcspMembershipMapper(final BaseMapper baseMapper, final MapperUtil mapperUtil) {
    this.baseMapper = baseMapper;
    this.mapperUtil = mapperUtil;
  }

  public AcspMembership daoToDto(final AcspMembersDao acspMembersDao) {
    return Optional.ofNullable(acspMembersDao)
        .map(baseMapper::daoToDto)
        .map(mapperUtil::enrichAcspMembershipWithUserDetails)
        .map(mapperUtil::enrichAcspMembershipWithAcspData)
        .orElse(null);
  }

  public AcspMembership daoToDto(AcspMembersDao acspMembersDao, User user, AcspDataDao acspData) {
    final var acspMembership =
        Optional.ofNullable(acspMembersDao).map(baseMapper::daoToDto).orElse(null);
    if (acspMembership != null) {
      acspMembership.setAcspName(acspData.getAcspName());
      acspMembership.setAcspStatus(
          AcspMembership.AcspStatusEnum.fromValue(acspData.getAcspStatus()));
      acspMembership.setUserDisplayName(
          Optional.ofNullable(user.getDisplayName()).orElse(DEFAULT_DISPLAY_NAME));
      acspMembership.setUserEmail(user.getEmail());
    }

    return acspMembership;
  }
}
