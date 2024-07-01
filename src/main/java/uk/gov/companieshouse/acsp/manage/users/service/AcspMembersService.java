package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;

import java.util.stream.Stream;

@Service
public class AcspMembersService {

    private final AcspMembersRepository acspMembersRepository;

    public AcspMembersService(
            final AcspMembersRepository acspMembersRepository
    ) {
        this.acspMembersRepository = acspMembersRepository;
    }

    @Transactional(readOnly=true)
    public Stream<AcspMembersDao> fetchAcspMembers(
            final String userId
    ){
        return acspMembersRepository.fetchAcspMembersByUserId(userId);
    }

}
