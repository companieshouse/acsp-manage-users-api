package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipCollectionMappers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Testcontainers
@Tag("integration-test")
class AcspMembersServiceIntegrationTest {

  @Container @ServiceConnection
  private static MongoDBContainer container = new MongoDBContainer("mongo:5");

  @Autowired private MongoTemplate mongoTemplate;

  @MockBean private ApiClientService apiClientService;

  @MockBean private InternalApiClient internalApiClient;

  @MockBean StaticPropertyUtil staticPropertyUtil;

  private final TestDataManager testDataManager = TestDataManager.getInstance();

  @Autowired private AcspMembersService acspMembersService;

  @Autowired private AcspMembersRepository acspMembersRepository;

  @MockBean private AcspMembershipCollectionMappers acspMembershipCollectionMappers;

  @MockBean private UsersService usersService;

  @MockBean private AcspDataService acspDataService;

  private User testUser;

  @BeforeEach
  public void setup() {
    testUser = testDataManager.fetchUserDtos("COMU002").get(0);

    acspMembersRepository.deleteAll();

    List<AcspMembersDao> testMembers =
        testDataManager.fetchAcspMembersDaos(
            "COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
    acspMembersRepository.saveAll(testMembers);

    when(acspMembershipCollectionMappers.daoToDto(anyList(), any(), any()))
        .thenAnswer(
            invocation -> {
              List<AcspMembersDao> daos = invocation.getArgument(0);
              return daos.stream()
                  .map(
                      dao -> {
                        AcspMembership membership = new AcspMembership();
                        membership.setAcspNumber(dao.getAcspNumber());
                        membership.setUserRole(
                            AcspMembership.UserRoleEnum.fromValue(dao.getUserRole()));
                        membership.setMembershipStatus(
                            AcspMembership.MembershipStatusEnum.fromValue(dao.getStatus()));
                        return membership;
                      })
                  .toList();
            });
  }

  @Nested
  class FetchAcspMembershipsTests {
    @Test
    void fetchAcspMembershipsReturnsAcspMembershipsListWithAllAcspMembersIfIncludeRemovedTrue() {
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertEquals(1, result.getItems().size());
      assertTrue(result.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("COMA001")));
    }

    @Test
    void
        fetchAcspMembershipsReturnsAcspMembershipsListWithActiveAcspMembersIfIncludeRemovedFalse() {
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

      assertEquals(1, result.getItems().size());
      assertTrue(result.getItems().stream().allMatch(m -> m.getAcspNumber().equals("COMA001")));
    }

    @Test
    void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedTrue() {
      User nonExistentUser = testDataManager.fetchUserDtos("TSU001").get(0);
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(nonExistentUser, true);

      assertTrue(result.getItems().isEmpty());
    }

    @Test
    void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedFalse() {
      User nonExistentUser = testDataManager.fetchUserDtos("TSU001").get(0);
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(nonExistentUser, false);

      assertTrue(result.getItems().isEmpty());
    }
  }

  @Nested
  class FindAllByAcspNumberAndRoleTests {

    private AcspDataDao acspDataDao;

    @BeforeEach
    void setUp() {
      acspDataDao = new AcspDataDao();
      acspDataDao.setId("COMA001");

      when(acspMembershipCollectionMappers.daoToDto(any(Page.class), any(), any(AcspDataDao.class)))
          .thenAnswer(
              invocation -> {
                Page<AcspMembersDao> daos = invocation.getArgument(0);
                AcspMembershipsList list = new AcspMembershipsList();
                list.setItems(
                    daos.getContent().stream()
                        .map(
                            dao -> {
                              AcspMembership membership = new AcspMembership();
                              membership.setAcspNumber(dao.getAcspNumber());
                              membership.setUserRole(
                                  AcspMembership.UserRoleEnum.fromValue(dao.getUserRole()));
                              membership.setMembershipStatus(
                                  AcspMembership.MembershipStatusEnum.fromValue(dao.getStatus()));
                              return membership;
                            })
                        .toList());
                return list;
              });
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedTrue() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "COMA001", acspDataDao, "standard", true, 0, 10);

      assertFalse(result.getItems().isEmpty());
      assertTrue(
          result.getItems().stream()
              .allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedFalse() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "COMA001", acspDataDao, "standard", false, 0, 10);

      assertFalse(result.getItems().isEmpty());
      assertTrue(
          result.getItems().stream()
              .allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedTrue() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("COMA001", acspDataDao, null, true, 0, 10);

      assertFalse(result.getItems().isEmpty());
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedFalse() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("COMA001", acspDataDao, null, false, 0, 10);

      assertFalse(result.getItems().isEmpty());
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsEmptyListForNonExistentAcsp() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "NON_EXISTENT", acspDataDao, null, true, 0, 10);

      assertTrue(result.getItems().isEmpty());
    }
  }

  @Nested
  class FetchMembership {
    @Test
    void fetchMembershipWithNullMembershipIdThrowsIllegalArguemntException() {
      Assertions.assertThrows(
          IllegalArgumentException.class, () -> acspMembersService.fetchMembership(null));
    }

    @Test
    void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional() {
      Assertions.assertFalse(acspMembersService.fetchMembership("$$$").isPresent());
    }

    @Test
    void fetchMembershipRetrievesMembership() {
      acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));

      Mockito.doReturn(testDataManager.fetchUserDtos("TSU001").getFirst())
          .when(usersService)
          .fetchUserDetails("TSU001");
      Mockito.doReturn(testDataManager.fetchAcspDataDaos("TSA001").getFirst())
          .when(acspDataService)
          .fetchAcspData("TSA001");

      Assertions.assertTrue(acspMembersService.fetchMembership("TS001").isPresent());
    }
  }

  @Nested
  class FetchAcspMembershipsByAcspNumberTests {

    @Test
    void fetchAcspMemberships_WithoutRemovedMemberships_ReturnsEmptyList() {
      var user = testDataManager.fetchUserDtos("COMU003").get(0);
      var acspMembersList = acspMembersService.fetchAcspMemberships(user, false, "COMA001");

      assertTrue(acspMembersList.getItems().isEmpty());
    }

    @Test
    void fetchAcspMemberships_WithRemovedMemberships_ReturnsRemovedMembership() {
      var user = testDataManager.fetchUserDtos("COMU003").get(0);
      var acspMembersList = acspMembersService.fetchAcspMemberships(user, true, "COMA001");

      assertFalse(acspMembersList.getItems().isEmpty());
      var firstMembership = acspMembersList.getItems().getFirst();
      assertEquals("COMA001", firstMembership.getAcspNumber());
      assertEquals(
          AcspMembership.MembershipStatusEnum.REMOVED, firstMembership.getMembershipStatus());
    }

    @Test
    void fetchAcspMemberships_WithActiveMembership_ReturnsActiveMembership() {
      var user = testDataManager.fetchUserDtos("COMU002").get(0);
      var acspMembersList = acspMembersService.fetchAcspMemberships(user, true, "COMA001");

      assertFalse(acspMembersList.getItems().isEmpty());
      var firstMembership = acspMembersList.getItems().getFirst();
      assertEquals("COMA001", firstMembership.getAcspNumber());
      assertEquals(
          AcspMembership.MembershipStatusEnum.ACTIVE, firstMembership.getMembershipStatus());
    }
  }

  @Test
  void fetchMembershipDaoWithNullMembershipIdThrowsIllegalArgumentException() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> acspMembersService.fetchMembershipDao(null));
  }

  @Test
  void fetchMembershipDaoWithMalformedOrNonExistentMembershipIdReturnsEmptyOptional() {
    Assertions.assertFalse(acspMembersService.fetchMembershipDao("£££").isPresent());
    Assertions.assertFalse(acspMembersService.fetchMembershipDao("TS001").isPresent());
  }

  @Test
  void fetchMembershipDaoRetrievesMembership() {
    acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));
    Assertions.assertEquals("TS001", acspMembersService.fetchMembershipDao("TS001").get().getId());
  }

  @Test
  void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero() {
    Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners(null));
    Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners("£££"));
    Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners("TS001"));
  }

  @Test
  void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp() {
    acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "COM001"));
    Assertions.assertEquals(1, acspMembersService.fetchNumberOfActiveOwners("COMA001"));
  }

  @Test
  void
      fetchActiveAcspMembershipWithNullOrMalformedOrNonexistentUserIdOrAcspNumberReturnsEmptyOptional() {
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership(null, "TSA001").isPresent());
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership("£££", "TSA001").isPresent());
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership("TSU001", null).isPresent());
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership("TSU001", "£££").isPresent());
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership("TSU001", "TSA001").isPresent());
  }

  @Test
  void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional() {
    acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS002"));
    Assertions.assertFalse(
        acspMembersService.fetchActiveAcspMembership("TSU002", "TSA001").isPresent());
  }

  @Test
  void fetchActiveAcspMembershipRetrievesMembership() {
    acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));
    Assertions.assertEquals(
        "TS001", acspMembersService.fetchActiveAcspMembership("TSU001", "TSA001").get().getId());
  }

  @Test
  void updateMembershipWithNullMembershipIdThrowsIllegalArgumentException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            acspMembersService.updateMembership(
                null, UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
  }

  @Test
  void
      updateMembershipWithMalformedOrNonexistentMembershipIdThrowsInternalServerErrorRuntimeException() {
    Assertions.assertThrows(
        InternalServerErrorRuntimeException.class,
        () ->
            acspMembersService.updateMembership(
                "£££", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
    Assertions.assertThrows(
        InternalServerErrorRuntimeException.class,
        () ->
            acspMembersService.updateMembership(
                "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
  }

  @Test
  void updateMembershipWithNullUserStatusAndNullUserRoleOnlyUpdatesEtag() {
    final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
    acspMembersRepository.insert(originalDao);

    acspMembersService.updateMembership("TS001", null, null, "TSU002");
    final var updatedDao = acspMembersRepository.findById("TS001").get();

    Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
    Assertions.assertEquals(originalDao.getUserRole(), updatedDao.getUserRole());
    Assertions.assertEquals(originalDao.getStatus(), updatedDao.getStatus());
    Assertions.assertEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
    Assertions.assertEquals(originalDao.getRemovedBy(), updatedDao.getRemovedBy());
  }

  @Test
  void updateMembershipWithNullUserStatusAndNotNullUserRoleOnlyUpdatesEtagAndRole() {
    final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
    acspMembersRepository.insert(originalDao);

    acspMembersService.updateMembership("TS001", null, UserRoleEnum.STANDARD, "TSU002");
    final var updatedDao = acspMembersRepository.findById("TS001").get();

    Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
    Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
    Assertions.assertEquals(originalDao.getStatus(), updatedDao.getStatus());
    Assertions.assertEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
    Assertions.assertEquals(originalDao.getRemovedBy(), updatedDao.getRemovedBy());
  }

  @Test
  void
      updateMembershipWithNotNullUserStatusAndNullUserRoleOnlyUpdatesEtagAndStatusAndRemovedAtAndRemovedBy() {
    final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
    acspMembersRepository.insert(originalDao);

    acspMembersService.updateMembership("TS001", UserStatusEnum.REMOVED, null, "TSU002");
    final var updatedDao = acspMembersRepository.findById("TS001").get();

    Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
    Assertions.assertEquals(originalDao.getUserRole(), updatedDao.getUserRole());
    Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
    Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
    Assertions.assertEquals("TSU002", updatedDao.getRemovedBy());
  }

  @Test
  void updateMembershipWithNotNullUserStatusAndNotNullUserRoleOnlyUpdatesEverything() {
    final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
    acspMembersRepository.insert(originalDao);

    acspMembersService.updateMembership(
        "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002");
    final var updatedDao = acspMembersRepository.findById("TS001").get();

    Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
    Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
    Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
    Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
    Assertions.assertEquals("TSU002", updatedDao.getRemovedBy());
  }

  @Test
  void updateMembershipWithNullRequestingUserIdSetsRemovedByToNull() {
    final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
    acspMembersRepository.insert(originalDao);

    acspMembersService.updateMembership(
        "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, null);
    final var updatedDao = acspMembersRepository.findById("TS001").get();

    Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
    Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
    Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
    Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
    Assertions.assertNull(updatedDao.getRemovedBy());
  }

  @Nested
  class AddAcspMember {
    @Test
    void addAcspMemberReturnsAddedAcspMembersDao() {
      // Given
      final var user = testDataManager.fetchUserDtos("TSU001").getFirst();
      final var acspNumber = "TS001";
      final var userRole = AcspMembership.UserRoleEnum.ADMIN;
      final var addedByUserId = "COMU001";
      // When
      final var result =
          acspMembersService.addAcspMember(user.getUserId(), acspNumber, userRole, addedByUserId);
      // Then
      assertEquals(user.getUserId(), result.getUserId());
      assertEquals(acspNumber, result.getAcspNumber());
      assertEquals(userRole.getValue(), result.getUserRole());
      assertEquals(addedByUserId, result.getAddedBy());
      assertEquals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue(), result.getStatus());
      assertFalse(result.getEtag().isEmpty());
    }
  }

  @Nested
  class AddAcspMembership {
    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @Test
    void addAcspMembershipReturnsAddedAcspMembership() {
      // Given
      final var user = testDataManager.fetchUserDtos("TSU001").getFirst();
      final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
      final var acspNumber = "TS001";
      final var userRole = AcspMembership.UserRoleEnum.ADMIN;
      final var addedByUserId = "COMU001";
      // When
      final var result =
          acspMembersService.addAcspMembership(user, acspData, acspNumber, userRole, addedByUserId);
      // Then
      assertEquals(user.getUserId(), result.getUserId());
      assertEquals(user.getEmail(), result.getUserEmail());
      assertEquals(DEFAULT_DISPLAY_NAME, result.getUserDisplayName());
      assertEquals(userRole, result.getUserRole());
      assertEquals(addedByUserId, result.getAddedBy());
      assertEquals(acspNumber, result.getAcspNumber());
    }
  }

  @AfterEach
  public void after() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
  }
}
