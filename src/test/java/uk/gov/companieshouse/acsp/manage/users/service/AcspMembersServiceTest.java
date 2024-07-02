package uk.gov.companieshouse.acsp.manage.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@ExtendWith(MockitoExtension.class)
class AcspMembersServiceTest {

  @Mock private AcspMembersRepository acspMembersRepository;

  @Mock private AcspMembershipListMapper acspMembershipListMapper;

  @InjectMocks private AcspMembersService acspMembersService;

  private User testUser;
  private List<AcspMembersDao> testAcspMembersDaos;
  private List<AcspMembership> testAcspMemberships;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId("user123");

    testAcspMembersDaos =
        Arrays.asList(createAcspMembersDao("1", false), createAcspMembersDao("2", true));

    testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
  }

  @Test
  void fetchAcspMemberships_includeRemoved_returnsAllMemberships() {
    when(acspMembersRepository.fetchAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Stream.of(testAcspMembersDaos.get(0), testAcspMembersDaos.get(1)));
    when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser)))
        .thenReturn(testAcspMemberships);

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertEquals(2, result.size());
    verify(acspMembersRepository).fetchAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(anyList(), eq(testUser));
  }

  @Test
  void fetchAcspMemberships_excludeRemoved_returnsOnlyActiveMemberships() {
    when(acspMembersRepository.fetchAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Stream.of(testAcspMembersDaos.get(0), testAcspMembersDaos.get(1)));
    when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser)))
        .thenReturn(Arrays.asList(testAcspMemberships.get(0)));

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertEquals(1, result.size());
    verify(acspMembersRepository).fetchAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(anyList(), eq(testUser));
  }

  @Test
  void fetchAcspMemberships_noMemberships_returnsEmptyList() {
    when(acspMembersRepository.fetchAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Stream.empty());
    when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser))).thenReturn(Arrays.asList());

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertTrue(result.isEmpty());
    verify(acspMembersRepository).fetchAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(anyList(), eq(testUser));
  }

  private AcspMembersDao createAcspMembersDao(String id, boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setRemovedBy(removed ? "remover" : null);
    return dao;
  }
}
