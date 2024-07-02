package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipListMapper;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public AcspMembersService(
      final AcspMembersRepository acspMembersRepository,
      final AcspMembershipListMapper acspMembershipListMapper) {
    this.acspMembersRepository = acspMembersRepository;
    this.acspMembershipListMapper = acspMembershipListMapper;
  }

  @Transactional(readOnly = true)
  public List<AcspMembership> fetchAcspMemberships(final User user, final boolean includeRemoved) {
    LOG.debug(
        String.format(
            "Fetching ACSP memberships from the repository for user ID: %s, include removed: %b",
            user.getUserId(), includeRemoved));
    return acspMembershipListMapper.daoToDto(
        acspMembersRepository
            .fetchAcspMembersByUserId(user.getUserId())
            .filter(acspMembersDao -> includeRemoved || !acspMembersDao.beenRemoved())
            .collect(Collectors.toList()),
        user);
  }
}
