package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@SpringBootTest
@Testcontainers(parallel = true)
@Tag("integration-test")
class AcspMembersRepositoryIntegrationTest {

  @Container @ServiceConnection
  private static MongoDBContainer container = new MongoDBContainer("mongo:5");

  @Autowired private MongoTemplate mongoTemplate;

  @MockBean private ApiClientUtil apiClientUtil;

  @MockBean private StaticPropertyUtil staticPropertyUtil;

  private final TestDataManager testDataManager = TestDataManager.getInstance();

  @Autowired private AcspMembersRepository acspMembersRepository;

  @BeforeEach
  void setUp() {
    acspMembersRepository.deleteAll();
  }

  @Nested
  class FindAllNotRemovedByAcspNumber {
    @Test
    void returnsNotRemovedMembersForGivenAcspNumber() {
      // Given
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllNotRemovedByAcspNumber(acspNumber, pageable);

      // Then
      assertEquals(3, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(member -> member.getAcspNumber().equals(acspNumber)));
      assertTrue(result.getContent().stream().allMatch(member -> member.getRemovedBy() == null));
    }
  }

  @Nested
  class FindAllByAcspNumber {
    @Test
    void returnsAllMembersForGivenAcspNumber() {
      // Given
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result = acspMembersRepository.findAllByAcspNumber(acspNumber, pageable);

      // Then
      assertEquals(6, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(member -> member.getAcspNumber().equals(acspNumber)));
    }
  }

  @Nested
  class FindAllNotRemovedByAcspNumberAndUserRole {
    @Test
    void returnsNotRemovedMembersForGivenAcspNumberAndUserRole() {
      // Given
      String acspNumber = "COMA001";
      String userRole = AcspMembership.UserRoleEnum.ADMIN.getValue();
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
              acspNumber, userRole, pageable);

      // Then
      assertEquals(2, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(
                  member ->
                      member.getAcspNumber().equals(acspNumber)
                          && member.getUserRole().equals(userRole)
                          && member.getRemovedBy() == null));
    }
  }

  @Nested
  class FindAllByAcspNumberAndUserRole {
    @Test
    void returnsAllMembersForGivenAcspNumberAndUserRole() {
      // Given
      String acspNumber = "COMA001";
      String userRole = AcspMembership.UserRoleEnum.ADMIN.getValue();
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllByAcspNumberAndUserRole(acspNumber, userRole, pageable);

      // Then
      assertEquals(3, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(
                  member ->
                      member.getAcspNumber().equals(acspNumber)
                          && member.getUserRole().equals(userRole)));
    }
  }

  @Nested
  class FetchAllAcspMembersByUserId {
    @Test
    void returnsAllAcspMembersForProvidedUserId() {
      // Given
      final var userId = "TSU002";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      final var result = acspMembersRepository.fetchAllAcspMembersByUserId(userId);

      // Then
      assertEquals(2, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("NF002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue())));
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("TS002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.REMOVED.getValue())));
    }
  }

  @Nested
  class FetchActiveAcspMembersByUserId {
    @Test
    void returnsActiveAcspMembersForProvidedUserId() {
      // Given
      final var userId = "TSU002";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      final var result = acspMembersRepository.fetchActiveAcspMembersByUserId(userId);

      // Then
      assertEquals(1, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("NF002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue())));
    }
  }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }
}
