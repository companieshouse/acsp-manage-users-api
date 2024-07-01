package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.UserRoleMapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

import java.time.LocalDateTime;
import java.util.Optional;
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
    ) {
        return acspMembersRepository.fetchAcspMembersByUserId(userId);
    }

    @Transactional(readOnly=true)
    public Optional<AcspMembersDao> fetchAcspMember(
            final String userId
    ) {
        return acspMembersRepository.fetchAcspMemberByUserId(userId);
    }

    @Transactional(readOnly=true)
    public Optional<AcspMembersDao> fetchAcspMemberByUserIdAndAcspNumber(
            final String userId,
            final String acspNumber
    ) {
        return acspMembersRepository.fetchAcspMemberByUserIdAndAcspNumber(userId, acspNumber);
    }

    public AcspMembersDao addAcspMember(
            final RequestBodyPost requestBodyPost,
            final String addedByUserId
    ) {
        final var now = LocalDateTime.now();
        AcspMembersDao newMembership = new AcspMembersDao();
        newMembership.setUserId(requestBodyPost.getUserId());
        newMembership.setAcspNumber(requestBodyPost.getAcspNumber());
        newMembership.setUserRole(
                UserRoleMapperUtil.mapToUserRoleEnum(requestBodyPost.getUserRole())
        );
        newMembership.setCreatedAt(now);
        newMembership.setAddedAt(now);
        newMembership.setAddedBy(addedByUserId);
        newMembership.setRemovedBy(null);
        newMembership.setRemovedAt(null);
        return acspMembersRepository.insert(newMembership);
    }

}
