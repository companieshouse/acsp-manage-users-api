package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipsListMapper;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public AcspMembersService(
      final AcspMembersRepository acspMembersRepository,
      AcspMembershipListMapper acspMembershipsListMapper) {
    this.acspMembersRepository = acspMembersRepository;
    this.acspMembershipsListMapper = acspMembershipsListMapper;
  }

  @Transactional(readOnly = true)
  public AcspMembershipsList fetchAcspMemberships(final User user, final boolean includeRemoved) {
    LOG.debug(
        String.format(
            "Fetching ACSP memberships from the repository for user ID: %s, include removed: %b",
            user.getUserId(), includeRemoved));

    List<AcspMembersDao> acspMembers;
    if (includeRemoved) {
      acspMembers = acspMembersRepository.fetchAllAcspMembersByUserId(user.getUserId());
    } else {
      acspMembers = acspMembersRepository.fetchActiveAcspMembersByUserId(user.getUserId());
    }

    final var acspMembershipsList = new AcspMembershipsList();
    acspMembershipsList.setItems(acspMembershipsListMapper.daoToDto(acspMembers, user));
    return acspMembershipsList;
  }
}
