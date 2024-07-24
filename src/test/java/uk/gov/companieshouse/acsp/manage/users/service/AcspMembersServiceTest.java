package uk.gov.companieshouse.acsp.manage.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipsListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembersServiceTest {

  @Mock private AcspMembersRepository acspMembersRepository;

  @Mock private AcspMembershipListMapper acspMembershipListMapper;

  @Mock private AcspMembershipsListMapper acspMembershipsListMapper;

  @Mock
  private AcspMembershipMapper acspMembershipMapper;

  @InjectMocks private AcspMembersService acspMembersService;

  private TestDataManager testDataManager;
  private User testUser;
  private List<AcspMembersDao> testActiveAcspMembersDaos;
  private List<AcspMembersDao> testAllAcspMembersDaos;
  private List<AcspMembership> testAcspMemberships;

  @BeforeEach
  void setUp() {
    testDataManager = TestDataManager.getInstance();
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

  @Nested
  class FetchAcspMemberships {
    @Test
    void fetchAcspMembershipsReturnsAllAcspMembersIfIncludeRemovedTrue() {
      when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(testAllAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testAllAcspMembersDaos, testUser))
          .thenReturn(testAcspMemberships);

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      assertSame(testAcspMemberships, result.getItems());
      verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(testAllAcspMembersDaos, testUser);
    }

    @Test
    void fetchAcspMembershipsReturnsActiveAcspMembersIfIncludeRemovedFalse() {
      when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(testActiveAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
          .thenReturn(Collections.singletonList(testAcspMemberships.get(0)));

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

      assertNotNull(result);
      assertEquals(1, result.getItems().size());
      assertSame(testAcspMemberships.get(0), result.getItems().get(0));
      verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
    }

    @Test
    void fetchAcspMembershipsReturnsEmptyListIfNoMemberships() {
      when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(Collections.emptyList());
      when(acspMembershipListMapper.daoToDto(Collections.emptyList(), testUser))
          .thenReturn(Collections.emptyList());

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertNotNull(result);
      assertTrue(result.getItems().isEmpty());
      verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(Collections.emptyList(), testUser);
    }
  }

  @Nested
  class FindAllByAcspNumberAndRole {
    private AcspDataDao acspDataDao;
    private Pageable pageable;
    private Page<AcspMembersDao> pageResult;

    @BeforeEach
    void setUp() {
      acspDataDao = new AcspDataDao();
      acspDataDao.setId("ACSP001");
      pageable = PageRequest.of(0, 10);
      pageResult = new PageImpl<>(testAllAcspMembersDaos);
    }

    @Test
    void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedTrue() {
      when(acspMembersRepository.findAllByAcspNumberAndUserRole("ACSP001", "standard", pageable))
          .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "ACSP001", acspDataDao, "standard", true, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllByAcspNumberAndUserRole("ACSP001", "standard", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }

    @Test
    void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedFalse() {
      when(acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
              "ACSP001", "standard", pageable))
          .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "ACSP001", acspDataDao, "standard", false, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository)
          .findAllNotRemovedByAcspNumberAndUserRole("ACSP001", "standard", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }

    @Test
    void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedTrue() {
      when(acspMembersRepository.findAllByAcspNumber("ACSP001", pageable)).thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("ACSP001", acspDataDao, null, true, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllByAcspNumber("ACSP001", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }

    @Test
    void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedFalse() {
      when(acspMembersRepository.findAllNotRemovedByAcspNumber("ACSP001", pageable))
          .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("ACSP001", acspDataDao, null, false, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllNotRemovedByAcspNumber("ACSP001", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }
  }

  @Nested
  class FetchMembership {
    @Test
    void fetchMembershipWithNullMembershipIdThrowsIllegalArgumentException() {
      Mockito.doThrow(new IllegalArgumentException("Cannot be null"))
          .when(acspMembersRepository)
          .findById(isNull());
      assertThrows(IllegalArgumentException.class, () -> acspMembersService.fetchMembership(null));
    }

    @Test
    void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional() {
      when(acspMembersRepository.findById("$$$")).thenReturn(Optional.empty());

      Optional<AcspMembership> result = acspMembersService.fetchMembership("$$$");

      assertFalse(result.isPresent());
      verify(acspMembersRepository).findById("$$$");
    }

    @Test
    void fetchMembershipRetrievesMembership() {
      AcspMembersDao acspMemberDao = testDataManager.fetchAcspMembersDaos("TS001").get(0);
      AcspMembership expectedMembership = new AcspMembership();

      when(acspMembersRepository.findById("TS001")).thenReturn(Optional.of(acspMemberDao));
      when(acspMembershipMapper.daoToDto(acspMemberDao)).thenReturn(expectedMembership);

      Optional<AcspMembership> result = acspMembersService.fetchMembership("TS001");

      assertTrue(result.isPresent());
      assertSame(expectedMembership, result.get());
      verify(acspMembersRepository).findById("TS001");
      verify(acspMembershipMapper).daoToDto(acspMemberDao);
    }
  }
}
