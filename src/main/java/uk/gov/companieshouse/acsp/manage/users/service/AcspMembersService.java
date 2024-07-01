package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.time.LocalDateTime;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

@Service
public class AcspMembersService {
    private final AcspMembersRepository acspMembersRepository;

    public AcspMembersService(AcspMembersRepository acspMembersRepository) {
        this.acspMembersRepository = acspMembersRepository;
    }

    public AcspMembersDao createAcspMembersWithOwnerRole(String acspNumber, String userId) {
        AcspMembersDao acspMembersDao = new AcspMembersDao();
        acspMembersDao.setAcspNumber(acspNumber);
        acspMembersDao.setUserId(userId);
        acspMembersDao.setUserRole(AcspMembership.UserRoleEnum.OWNER);
        acspMembersDao.setAddedAt(LocalDateTime.now());
        acspMembersDao.setEtag(generateEtag());
        return acspMembersRepository.save(acspMembersDao);
    }
}
