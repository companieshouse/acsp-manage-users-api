package uk.gov.companieshouse.acsp.manage.users.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.sdk.ApiClientService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

  @MockBean private AcspMembershipListMapper acspMembershipsListMapper;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId("COMU002");

    acspMembersRepository.deleteAll();

    AcspMembersDao activeMember =
        createAcspMembersDao(
            "1", "ACSP001", testUser.getUserId(), AcspMembership.UserRoleEnum.ADMIN, false);
    AcspMembersDao removedMember =
        createAcspMembersDao(
            "2", "ACSP002", testUser.getUserId(), AcspMembership.UserRoleEnum.STANDARD, true);

    acspMembersRepository.save(activeMember);
    acspMembersRepository.save(removedMember);

    when(acspMembershipsListMapper.daoToDto(anyList(), eq(testUser)))
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
                        return membership;
                      })
                  .toList();
            });
  }

  private AcspMembersDao createAcspMembersDao(
      String id,
      String acspNumber,
      String userId,
      AcspMembership.UserRoleEnum userRole,
      boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setAcspNumber(acspNumber);
    dao.setUserId(userId);
    dao.setUserRole(userRole.toString());
    dao.setCreatedAt(LocalDateTime.now());
    dao.setAddedAt(LocalDateTime.now());
    dao.setAddedBy("testAdder");
    if (removed) {
      dao.setRemovedAt(LocalDateTime.now());
      dao.setRemovedBy("testRemover");
    }
    dao.setEtag("testEtag");
    return dao;
  }

  @Test
  void fetchAcspMembershipsReturnsAcspMembershipsListWithAllAcspMembersIfIncludeRemovedTrue() {
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertEquals(2, result.getItems().size());
    assertTrue(result.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("ACSP001")));
    assertTrue(result.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("ACSP002")));
  }

  @Test
  void fetchAcspMembershipsReturnsAcspMembershipsListWithActiveAcspMembersIfIncludeRemovedFalse() {
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertEquals(1, result.getItems().size());
    assertTrue(result.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("ACSP001")));
  }

  @Test
  void
      fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedTrue() {
    testUser.setUserId("X666");
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertTrue(result.getItems().isEmpty());
  }

  @Test
  void
      fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedFalse() {
    testUser.setUserId("X666");
    AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertTrue(result.getItems().isEmpty());
  }
}
