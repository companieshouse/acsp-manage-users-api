package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipsListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipListMapper;
  private final AcspMembershipsListMapper acspMembershipsListMapper;
  private final AcspMembershipMapper acspMembershipMapper;

    private static final Logger LOG =
            LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public AcspMembersService(
      final AcspMembersRepository acspMembersRepository,
      final AcspMembershipListMapper acspMembershipListMapper,
      final AcspMembershipsListMapper acspMembershipsListMapper,
      final AcspMembershipMapper acspMembershipMapper
      ) {
      this.acspMembersRepository = acspMembersRepository;
      this.acspMembershipListMapper = acspMembershipListMapper;
      this.acspMembershipsListMapper = acspMembershipsListMapper;
      this.acspMembershipMapper = acspMembershipMapper;
  }

  public Page<AcspMembersDao> findAllByAcspNumber(
      final String acspNumber,
      final boolean includeRemoved,
      final int pageIndex,
      final int itemsPerPage) {
    final Pageable pageable = PageRequest.of(pageIndex, itemsPerPage);
    if (includeRemoved) {
      return acspMembersRepository.findAllByAcspNumber(acspNumber, pageable);
    } else {
      return acspMembersRepository.findAllNotRemovedByAcspNumber(acspNumber, pageable);
    }
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
    acspMembershipsList.setItems(acspMembershipListMapper.daoToDto(acspMembers, user));
    return acspMembershipsList;
  }

  public AcspMembershipsList mapToAcspMembershipsList(
      Page<AcspMembersDao> acspMembersDaos, AcspDataDao acspDataDao) {
    return
        acspMembershipsListMapper.daoToDto(acspMembersDaos, acspDataDao);
  }

    @Transactional( readOnly = true )
    public Optional<AcspMembership> fetchMembership( final String membershipId ){
       return acspMembersRepository.findById( membershipId ).map( acspMembershipMapper::daoToDto );
    }

}
