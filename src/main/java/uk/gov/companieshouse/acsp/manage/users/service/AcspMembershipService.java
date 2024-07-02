package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Optional;

@Service
public class AcspMembershipService {

    private final AcspMembersRepository acspMembersRepository;
    private final AcspMembershipMapper acspMembershipMapper;

    public AcspMembershipService(AcspMembersRepository acspMembersRepository, AcspMembershipMapper acspMembershipMapper) {
        this.acspMembersRepository = acspMembersRepository;
        this.acspMembershipMapper = acspMembershipMapper;
    }

    public boolean memberIdExists(String id) {
        return acspMembersRepository.existsById(id);
    }

    public Optional<AcspMembership> fetchAcspMembership(String id) {
        return acspMembersRepository.fetchAcspMemberById(id)
                .map(acspMembershipMapper::daoToDto);
    }
}
