package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration-test")
class AcspMembersServiceIntegrationTest {

  @Container @ServiceConnection
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
  }

  @Autowired private AcspMembersService acspMembersService;

  @Autowired private AcspMembersRepository acspMembersRepository;

  @MockBean private AcspMembershipListMapper acspMembershipListMapper;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId("testUser123");

    acspMembersRepository.deleteAll();

    AcspMembersDao activeMember =
        createAcspMembersDao("1", "ACSP001", testUser.getUserId(), UserRoleEnum.ADMIN, false);
    AcspMembersDao removedMember =
        createAcspMembersDao("2", "ACSP002", testUser.getUserId(), UserRoleEnum.STANDARD, true);

    acspMembersRepository.save(activeMember);
    acspMembersRepository.save(removedMember);

    when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser)))
        .thenAnswer(
            invocation -> {
              List<AcspMembersDao> daos = invocation.getArgument(0);
              return daos.stream()
                  .map(
                      dao -> {
                        AcspMembership membership = new AcspMembership();
                        membership.setAcspNumber(dao.getAcspNumber());
                        membership.setUserRole(dao.getUserRole());
                        return membership;
                      })
                  .toList();
            });
  }

  @Test
  void fetchAcspMemberships_includeRemoved_returnsAllMemberships() {
    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(m -> m.getAcspNumber().equals("ACSP001")));
    assertTrue(result.stream().anyMatch(m -> m.getAcspNumber().equals("ACSP002")));
  }

  @Test
  void fetchAcspMemberships_excludeRemoved_returnsOnlyActiveMemberships() {
    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

    assertEquals(1, result.size());
    assertEquals("ACSP001", result.getFirst().getAcspNumber());
  }

  @Test
  void fetchAcspMemberships_noMemberships_returnsEmptyList() {
    User newUser = new User();
    newUser.setUserId("newUser456");

    List<AcspMembership> result = acspMembersService.fetchAcspMemberships(newUser, true);

    assertTrue(result.isEmpty());
  }

  @Test
  void fetchAcspMemberships_nullUser_throwsIllegalArgumentException() {
    assertThrows(
        NullPointerException.class, () -> acspMembersService.fetchAcspMemberships(null, true));
  }

  @Test
  void fetchAcspMemberships_userWithNoMemberships_returnsEmptyList() {
    User userWithNoMemberships = new User();
    userWithNoMemberships.setUserId("noMembershipsUser");

    List<AcspMembership> result =
        acspMembersService.fetchAcspMemberships(userWithNoMemberships, true);

    assertTrue(result.isEmpty());
  }

  private AcspMembersDao createAcspMembersDao(
      String id, String acspNumber, String userId, UserRoleEnum userRole, boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setAcspNumber(acspNumber);
    dao.setUserId(userId);
    dao.setUserRole(userRole);
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
}
