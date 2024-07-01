package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.UserRoleMapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AcspMembersService {

    private final AcspMembersRepository acspMembersRepository;
    private final AcspMembershipListMapper acspMembershipListMapper;

    public AcspMembersService(
            final AcspMembersRepository acspMembersRepository,
            final AcspMembershipListMapper acspMembershipListMapper
    ) {
        this.acspMembersRepository = acspMembersRepository;
        this.acspMembershipListMapper = acspMembershipListMapper;
    }

    @Transactional(readOnly=true)
    public List<AcspMembership> fetchAcspMemberships(
            final String userId,
            final boolean excludeRemoved
    ) {
        return acspMembershipListMapper.daoToDto(
                acspMembersRepository.fetchAcspMembersByUserId(userId).filter(
                        acspMembersDao -> !excludeRemoved || !acspMembersDao.hasBeenRemoved()
                ).toList(),
                UserContext.getLoggedUser()
        );
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
