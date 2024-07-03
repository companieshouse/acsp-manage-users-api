package uk.gov.companieshouse.acsp.manage.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembershipServiceTest {

    @InjectMocks
    AcspMembershipService acspMembershipService;

    @Mock
    AcspMembersRepository acspMembersRepository;
    @Mock
    AcspMembershipMapper acspMembershipMapper;

    private AcspMembersDao acspMember1;

    @BeforeEach
    public void setup() {
        acspMember1 = new AcspMembersDao();
        acspMember1.setId("acsp1");
        acspMember1.setAcspNumber("ACSP123");
        acspMember1.setUserId("user1");
        acspMember1.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
        acspMember1.setAddedAt(LocalDateTime.now().minusDays(30));
        acspMember1.setRemovedBy("Test1");
        acspMember1.setRemovedAt(LocalDateTime.now().minusDays(10));

        acspMembershipService = new AcspMembershipService(
                acspMembersRepository,
                acspMembershipMapper
        );
    }

    @Test
    void getAssociationByIdReturnsAssociationDtoWhenAssociationFound() {
        Mockito.when(acspMembersRepository.findById("acsp1")).thenReturn(Optional.of(acspMember1));

        var association = acspMembershipService.fetchAcspMembership("acsp1");
        Mockito.verify(acspMembershipMapper).daoToDto(acspMember1);

    }

    @Test
    void getAssociationByIdReturnsEmptyWhenAssociationNotFound() {
        Mockito.when(acspMembersRepository.findById("1111")).thenReturn(Optional.empty());

        var association = acspMembershipService.fetchAcspMembership("1111");
        assertTrue(association.isEmpty());

    }
}
