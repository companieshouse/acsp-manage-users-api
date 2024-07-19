package uk.gov.companieshouse.acsp.manage.users.service;

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
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembersServiceTest {

  @Mock private AcspMembersRepository acspMembersRepository;

  @Mock private AcspMembershipListMapper acspMembershipsListMapper;

  @InjectMocks private AcspMembersService acspMembersService;

  private User testUser;
  private List<AcspMembersDao> testActiveAcspMembersDaos;
  private List<AcspMembersDao> testAllAcspMembersDaos;
  private List<AcspMembership> testAcspMemberships;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId("COMU002");
    testActiveAcspMembersDaos = List.of(createAcspMembersDao("1", false));
    testAllAcspMembersDaos =
        Arrays.asList(createAcspMembersDao("1", false), createAcspMembersDao("2", true));
    testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
  }

  private AcspMembersDao createAcspMembersDao(String id, boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setRemovedBy(removed ? "remover" : null);
    return dao;
  }

  @Test
  void fetchAcspMembershipsReturnsAcspMembershipsListWithAllAcspMembersIfIncludeRemovedTrue() {
    // Given
    when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(testAllAcspMembersDaos);
    when(acspMembershipsListMapper.daoToDto(testAllAcspMembersDaos, testUser))
        .thenReturn(testAcspMemberships);
    // When
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);
    // Then
    assertNotNull(result);
    assertEquals(2, result.getItems().size());
    assertSame(testAcspMemberships, result.getItems());
    verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipsListMapper).daoToDto(testAllAcspMembersDaos, testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipsListMapper);
  }

  @Test
  void fetchAcspMembershipsReturnsAcspMembershipsListWithActiveAcspMembersIfIncludeRemovedFalse() {
    // Given
    when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(testActiveAcspMembersDaos);
    when(acspMembershipsListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
        .thenReturn(Collections.singletonList(testAcspMemberships.getFirst()));
    // When
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);
    // Then
    assertNotNull(result);
    assertEquals(1, result.getItems().size());
    assertSame(testAcspMemberships.getFirst(), result.getItems().getFirst());
    verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipsListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipsListMapper);
  }

  @Test
  void
      fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedTrue() {
    // Given
    when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Collections.emptyList());
    when(acspMembershipsListMapper.daoToDto(Collections.emptyList(), testUser))
        .thenReturn(Collections.emptyList());
    // When
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);
    // Then
    assertNotNull(result);
    assertTrue(result.getItems().isEmpty());
    verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipsListMapper).daoToDto(Collections.emptyList(), testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipsListMapper);
  }

  @Test
  void
      fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedFalse() {
    // Given
    when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
        .thenReturn(Collections.emptyList());
    when(acspMembershipsListMapper.daoToDto(Collections.emptyList(), testUser))
        .thenReturn(Collections.emptyList());
    // When
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);
    // Then
    assertNotNull(result);
    assertTrue(result.getItems().isEmpty());
    verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
    verify(acspMembershipsListMapper).daoToDto(Collections.emptyList(), testUser);
    verifyNoMoreInteractions(acspMembersRepository, acspMembershipsListMapper);
  }
}
