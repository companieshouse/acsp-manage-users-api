package uk.gov.companieshouse.acsp.manage.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
@Tag("unit-test")
class AcspMembersServiceTest {

  @Mock private AcspMembersRepository acspMembersRepository;

  @Mock private AcspMembershipListMapper acspMembershipListMapper;

  @InjectMocks private AcspMembersService acspMembersService;

  private User testUser;
  private List<AcspMembersDao> testActiveAcspMembersDaos;
  private List<AcspMembersDao> testAllAcspMembersDaos;
  private List<AcspMembership> testAcspMemberships;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId("user123");

    testActiveAcspMembersDaos = Arrays.asList(createAcspMembersDao("1", false));
    testAllAcspMembersDaos =
        Arrays.asList(createAcspMembersDao("1", false), createAcspMembersDao("2", true));

    testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
  }

  @Test
  void fetchAcspMemberships_includeRemoved_returnsAllMemberships() {
    when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(testAllAcspMembersDaos);
    when(acspMembershipListMapper.daoToDto(testAllAcspMembersDaos, testUser))
        .thenReturn(testAcspMemberships);

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertEquals(2, result.size());
    verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(testAllAcspMembersDaos, testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
  }

  @Test
  void fetchAcspMemberships_excludeRemoved_returnsOnlyActiveMemberships() {
    when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(testActiveAcspMembersDaos);
    when(acspMembershipListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
        .thenReturn(Arrays.asList(testAcspMemberships.get(0)));

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertEquals(1, result.size());
    verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
  }

  @Test
  void fetchAcspMemberships_noMemberships_returnsEmptyList() {
    when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList());
    when(acspMembershipListMapper.daoToDto(Arrays.asList(), testUser)).thenReturn(Arrays.asList());

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertTrue(result.isEmpty());
    verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipListMapper).daoToDto(Arrays.asList(), testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
  }

  private AcspMembersDao createAcspMembersDao(String id, boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setRemovedBy(removed ? "remover" : null);
    return dao;
  }
}