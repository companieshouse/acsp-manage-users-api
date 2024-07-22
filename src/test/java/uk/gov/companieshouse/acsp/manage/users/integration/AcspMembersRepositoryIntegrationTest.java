package uk.gov.companieshouse.acsp.manage.users.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void fetchAllAcspMembersByUserIdReturnsAllAcspMembersForProvidedUserId() {
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

  @Test
  void fetchActiveAcspMembersByUserIdReturnsActiveAcspMembersForProvidedUserId() {
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
