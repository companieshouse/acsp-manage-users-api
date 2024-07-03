package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class UserAcspMembershipIntegrationTest {

  @Container @ServiceConnection
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5");

  @Autowired MongoTemplate mongoTemplate;

  @Autowired MockMvc mockMvc;

  @Autowired AcspMembersRepository acspMembersRepository;

  @Autowired AcspDataRepository acspDataRepository;

  @MockBean UsersService usersService;

  private final LocalDateTime now = LocalDateTime.now();
  private final String userId = "user1";

  @BeforeEach
  void setup() {
    User user = new User();
    user.setUserId(userId);
    user.setEmail("user1@example.com");
    user.setDisplayName("Test User");
    when(usersService.fetchUserDetails(userId)).thenReturn(user);

    AcspMembersDao activeMembership = new AcspMembersDao();
    activeMembership.setId("1");
    activeMembership.setUserId(userId);
    activeMembership.setAcspNumber("ACSP001");
    activeMembership.setUserRole(UserRoleEnum.ADMIN);
    activeMembership.setCreatedAt(now.minusDays(5));
    activeMembership.setAddedAt(now.minusDays(5));
    activeMembership.setAddedBy("admin1");
    activeMembership.setEtag("etag1");

    AcspMembersDao removedMembership = new AcspMembersDao();
    removedMembership.setId("2");
    removedMembership.setUserId(userId);
    removedMembership.setAcspNumber("ACSP002");
    removedMembership.setUserRole(UserRoleEnum.STANDARD);
    removedMembership.setCreatedAt(now.minusDays(10));
    removedMembership.setAddedAt(now.minusDays(10));
    removedMembership.setAddedBy("admin2");
    removedMembership.setRemovedAt(now.minusDays(1));
    removedMembership.setRemovedBy("admin3");
    removedMembership.setEtag("etag2");

    acspMembersRepository.saveAll(List.of(activeMembership, removedMembership));

    AcspDataDao acspData1 = new AcspDataDao();
    acspData1.setId("ACSP001");
    acspData1.setAcspName("ACSP 1 Ltd");
    acspData1.setAcspStatus("active");

    AcspDataDao acspData2 = new AcspDataDao();
    acspData2.setId("ACSP002");
    acspData2.setAcspName("ACSP 2 Ltd");
    acspData2.setAcspStatus("suspended");

    acspDataRepository.saveAll(List.of(acspData1, acspData2));
  }

  @Test
  void getAcspMembershipForUserIdWithoutIncludeRemovedReturnsOnlyActive() throws Exception {
    mockMvc
        .perform(
            get("/acsp-members")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Identity", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("1"))
        .andExpect(jsonPath("$[0].acsp_number").value("ACSP001"))
        .andExpect(jsonPath("$[0].user_role").value("admin"))
        .andExpect(jsonPath("$[0].added_at").exists())
        .andExpect(jsonPath("$[0].added_by").value("admin1"))
        .andExpect(jsonPath("$[0].removed_at").doesNotExist())
        .andExpect(jsonPath("$[0].removed_by").doesNotExist())
        .andExpect(jsonPath("$[0].user_email").value("user1@example.com"))
        .andExpect(jsonPath("$[0].user_display_name").value("Test User"))
        .andExpect(jsonPath("$[0].acsp_name").value("ACSP 1 Ltd"))
        .andExpect(jsonPath("$[0].acsp_status").value("active"));

    verify(usersService, times(1)).fetchUserDetails(userId);
  }

  @Test
  void getAcspMembershipForUserIdWithIncludeRemovedReturnsAllMemberships() throws Exception {
    mockMvc
        .perform(
            get("/acsp-members?include_removed=true")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Identity", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("1"))
        .andExpect(jsonPath("$[0].acsp_number").value("ACSP001"))
        .andExpect(jsonPath("$[0].user_role").value("admin"))
        .andExpect(jsonPath("$[0].removed_at").doesNotExist())
        .andExpect(jsonPath("$[0].user_email").value("user1@example.com"))
        .andExpect(jsonPath("$[0].user_display_name").value("Test User"))
        .andExpect(jsonPath("$[0].acsp_name").value("ACSP 1 Ltd"))
        .andExpect(jsonPath("$[0].acsp_status").value("active"))
        .andExpect(jsonPath("$[1].id").value("2"))
        .andExpect(jsonPath("$[1].acsp_number").value("ACSP002"))
        .andExpect(jsonPath("$[1].user_role").value("standard"))
        .andExpect(jsonPath("$[1].removed_at").exists())
        .andExpect(jsonPath("$[1].removed_by").value("admin3"))
        .andExpect(jsonPath("$[1].user_email").value("user1@example.com"))
        .andExpect(jsonPath("$[1].user_display_name").value("Test User"))
        .andExpect(jsonPath("$[1].acsp_name").value("ACSP 2 Ltd"))
        .andExpect(jsonPath("$[1].acsp_status").value("suspended"));

    verify(usersService, times(1)).fetchUserDetails(userId);
  }

  @Test
  void getAcspMembershipForUserIdWithoutRequiredHeadersReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/acsp-members")).andExpect(status().isUnauthorized());

    verify(usersService, never()).fetchUserDetails(any());
  }

  @Test
  void getAcspMembershipForNonExistentUserReturnsEmptyList() throws Exception {
    String nonExistentUserId = "non-existent-user";
    when(usersService.fetchUserDetails(nonExistentUserId)).thenReturn(new User());

    mockMvc
        .perform(
            get("/acsp-members")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Identity", nonExistentUserId))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.length()").value(0));

    verify(usersService, times(1)).fetchUserDetails(nonExistentUserId);
  }

  @Test
  void getAcspMembershipForUserIdWithInvalidIncludeRemovedParamReturnsBadRequest()
      throws Exception {
    mockMvc
        .perform(
            get("/acsp-members?include_removed=invalid")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Identity", userId))
        .andExpect(status().isBadRequest());
  }

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
    mongoTemplate.dropCollection(AcspDataDao.class);
  }
}
