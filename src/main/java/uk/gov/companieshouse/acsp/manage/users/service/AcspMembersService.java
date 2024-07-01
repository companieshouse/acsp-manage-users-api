package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipListMapper;

  public AcspMembersService(
      final AcspMembersRepository acspMembersRepository,
      final AcspMembershipListMapper acspMembershipListMapper) {
    this.acspMembersRepository = acspMembersRepository;
    this.acspMembershipListMapper = acspMembershipListMapper;
  }

  @Transactional(readOnly = true)
  public List<AcspMembership> fetchAcspMemberships(
      final String userId, final boolean excludeRemoved) {
    return acspMembershipListMapper.daoToDto(
        acspMembersRepository
            .fetchAcspMembersByUserId(userId)
            .filter(acspMembersDao -> !excludeRemoved || !acspMembersDao.hasBeenRemoved())
            .toList(),
        UserContext.getLoggedUser());
  }
}
