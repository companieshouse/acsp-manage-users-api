package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@Service
public class AcspMembershipService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipMapper acspMembershipMapper;

  public AcspMembershipService(final AcspMembersRepository acspMembersRepository, final AcspMembershipMapper acspMembershipMapper) {
    this.acspMembersRepository = acspMembersRepository;
    this.acspMembershipMapper = acspMembershipMapper;
  }
  public Optional<AcspMembership> fetchAcspMembership(String id) {
    return acspMembersRepository.findById(id)
            .map(acspMembershipMapper::daoToDto);
  }
}