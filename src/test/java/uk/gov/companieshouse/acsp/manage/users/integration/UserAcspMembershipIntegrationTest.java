package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.http.MediaType;
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
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
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
  private final String userId = "standardUser";
  private final String adminUserId = "adminUser";
  private final String ownerUserId = "ownerUser";

  @BeforeEach
  void setup() {
    User user = new User();
    user.setUserId(userId);
    user.setEmail("standardUser@example.com");
    user.setDisplayName("Test User");
    when(usersService.fetchUserDetails(userId)).thenReturn(user);

    User user123 = new User();
    user123.setUserId("user123");
    user123.setEmail("user123@test.com");
    user123.setDisplayName("Test User");
    when(usersService.fetchUserDetails("user123")).thenReturn(user123);

    User adminUser = new User();
    adminUser.setUserId(adminUserId);
    adminUser.setEmail("admin@example.com");
    adminUser.setDisplayName("Admin User");
    when(usersService.fetchUserDetails(adminUserId)).thenReturn(adminUser);

    User ownerUser = new User();
    ownerUser.setUserId(ownerUserId);
    ownerUser.setEmail("owner@example.com");
    ownerUser.setDisplayName("Owner User");
    when(usersService.fetchUserDetails(ownerUserId)).thenReturn(ownerUser);

    setupTestData();
  }

  private void setupTestData() {

    AcspMembersDao activeMembership = new AcspMembersDao();
    activeMembership.setId("1");
    activeMembership.setUserId(userId);
    activeMembership.setAcspNumber("ACSP001");
    activeMembership.setUserRole(UserRoleEnum.STANDARD);
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

    AcspMembersDao adminMembership = new AcspMembersDao();
    adminMembership.setId("3");
    adminMembership.setUserId(adminUserId);
    adminMembership.setAcspNumber("ACSP001");
    adminMembership.setUserRole(UserRoleEnum.ADMIN);
    adminMembership.setCreatedAt(now.minusDays(15));
    adminMembership.setAddedAt(now.minusDays(15));
    adminMembership.setAddedBy("owner1");
    adminMembership.setEtag("etag3");

    AcspMembersDao ownerMembership = new AcspMembersDao();
    ownerMembership.setId("4");
    ownerMembership.setUserId(ownerUserId);
    ownerMembership.setAcspNumber("ACSP001");
    ownerMembership.setUserRole(UserRoleEnum.OWNER);
    ownerMembership.setCreatedAt(now.minusDays(20));
    ownerMembership.setAddedAt(now.minusDays(20));
    ownerMembership.setAddedBy("system");
    ownerMembership.setEtag("etag4");

    acspMembersRepository.saveAll(
        List.of(activeMembership, removedMembership, adminMembership, ownerMembership));

    AcspMembersDao acspMembersDao = new AcspMembersDao();
    acspMembersDao.setId("acsp1");
    acspMembersDao.setUserId("user123");
    acspMembersDao.setAcspNumber("ACSP123");
    acspMembersDao.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
    acspMembersDao.setCreatedAt(now.minusDays(10));
    acspMembersDao.setAddedAt(now.minusDays(10));
    acspMembersDao.setAddedBy("admin1");
    acspMembersDao.setEtag("etag");

    acspMembersRepository.saveAll(List.of(activeMembership, removedMembership, acspMembersDao));

    AcspDataDao acspData = new AcspDataDao();
    acspData.setId("ACSP123");
    acspData.setAcspName("ACSP123");
    acspData.setAcspStatus("active");

    AcspDataDao activeAcsp = new AcspDataDao();
    activeAcsp.setId("ACSP001");
    activeAcsp.setAcspName("ACSP 1 Ltd");
    activeAcsp.setAcspStatus("active");

    AcspDataDao suspendedAcsp = new AcspDataDao();
    suspendedAcsp.setId("ACSP002");
    suspendedAcsp.setAcspName("ACSP 2 Ltd");
    suspendedAcsp.setAcspStatus("suspended");

    AcspDataDao deauthorisedAcsp = new AcspDataDao();
    deauthorisedAcsp.setId("ACSP003");
    deauthorisedAcsp.setAcspName("ACSP 3 Ltd");
    deauthorisedAcsp.setAcspStatus("deauthorised");

    acspDataRepository.saveAll(List.of(acspData, activeAcsp, suspendedAcsp, deauthorisedAcsp));
  }

  @Nested
  @DisplayName("GET /acsp-members Tests")
  class GetAcspMembershipTests {

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
          .andExpect(jsonPath("$[0].user_role").value("standard"))
          .andExpect(jsonPath("$[0].added_at").exists())
          .andExpect(jsonPath("$[0].added_by").value("admin1"))
          .andExpect(jsonPath("$[0].removed_at").doesNotExist())
          .andExpect(jsonPath("$[0].removed_by").doesNotExist())
          .andExpect(jsonPath("$[0].user_email").value("standardUser@example.com"))
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
          .andExpect(jsonPath("$[0].user_role").value("standard"))
          .andExpect(jsonPath("$[0].removed_at").doesNotExist())
          .andExpect(jsonPath("$[0].user_email").value("standardUser@example.com"))
          .andExpect(jsonPath("$[0].user_display_name").value("Test User"))
          .andExpect(jsonPath("$[0].acsp_name").value("ACSP 1 Ltd"))
          .andExpect(jsonPath("$[0].acsp_status").value("active"))
          .andExpect(jsonPath("$[1].id").value("2"))
          .andExpect(jsonPath("$[1].acsp_number").value("ACSP002"))
          .andExpect(jsonPath("$[1].user_role").value("standard"))
          .andExpect(jsonPath("$[1].removed_at").exists())
          .andExpect(jsonPath("$[1].removed_by").value("admin3"))
          .andExpect(jsonPath("$[1].user_email").value("standardUser@example.com"))
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

    @Test
    void getAcspMembershipForAcspIdWithoutXRequestIdReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members")
                  .header("ERIC-Identity", "user1")
                  .header("ERIC-Identity-Type", "oauth2"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipForMalformedMembershipIdReturnsBadRequest() throws Exception {
      final var response =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "$$$")
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user123")
                      .header("ERIC-Identity-Type", "oauth2"))
              .andExpect(status().isBadRequest())
              .andReturn()
              .getResponse();
    }

    @Test
    void getAcspMembershipForAcspIdWithoutRequiredHeadersReturnsBadRequest() throws Exception {
      mockMvc.perform(get("/acsp-members")).andExpect(status().isUnauthorized());
    }

    @Test
    void getAcspMembershipForExistingMembershipIdShouldReturnData() throws Exception {
      final var response =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "acsp1")
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user123")
                      .header("ERIC-Identity-Type", "oauth2"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();

      final var objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      final var responseMembership =
          objectMapper.readValue(response.getContentAsByteArray(), AcspMembership.class);

      Assertions.assertEquals("acsp1", responseMembership.getId());
      Assertions.assertEquals("user123", responseMembership.getUserId());
      Assertions.assertEquals("ACSP123", responseMembership.getAcspNumber());
      Assertions.assertEquals("admin1", responseMembership.getAddedBy());
    }

    @Test
    void getAcspMembershipForNonExistingMembershipIdShouldReturnNotFound() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members/{id}", "acsp2")
                  .header("X-Request-Id", "theId123")
                  .header("ERIC-Identity", "user123")
                  .header("ERIC-Identity-Type", "oauth2"))
          .andExpect(status().isNotFound())
          .andReturn()
          .getResponse();
    }
  }

  @Nested
  @DisplayName("POST /acsp-members Tests")
  class PostAcspMembershipTests {

    @Test
    void addAcspMemberForbiddenWhenAcspIsDeauthorised() throws Exception {
      String newUserId = "newUser";
      User newUser = new User();
      newUser.setUserId(newUserId);
      newUser.setEmail("newuser@example.com");
      newUser.setDisplayName("New User");
      when(usersService.fetchUserDetails(newUserId)).thenReturn(newUser);
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      AcspMembersDao adminMembership = new AcspMembersDao();
      adminMembership.setUserId(adminUserId);
      adminMembership.setAcspNumber("ACSP003");
      adminMembership.setUserRole(UserRoleEnum.ADMIN);
      adminMembership.setCreatedAt(now);
      adminMembership.setAddedAt(now);
      adminMembership.setAddedBy("system");
      adminMembership.setEtag(generateEtag());
      acspMembersRepository.save(adminMembership);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP002\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isForbidden());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP003");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberSuccessAdminAddsStandard() throws Exception {
      String newUserId = "newUser";
      User newUser = new User();
      newUser.setUserId(newUserId);
      newUser.setEmail("newuser@example.com");
      newUser.setDisplayName("New User");
      when(usersService.fetchUserDetails(newUserId)).thenReturn(newUser);
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").exists());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertEquals(1, members.size());
      Assertions.assertEquals(UserRoleEnum.STANDARD, members.get(0).getUserRole());
    }

    @Test
    void addAcspMemberSuccessOwnerAddsAdmin() throws Exception {
      String newUserId = "newAdminUser";
      User newUser = new User();
      newUser.setUserId(newUserId);
      newUser.setEmail("newadmin@example.com");
      newUser.setDisplayName("New Admin User");
      when(usersService.fetchUserDetails(newUserId)).thenReturn(newUser);
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ownerUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"admin\"}",
                          newUserId)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").exists());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertEquals(1, members.size());
      Assertions.assertEquals(UserRoleEnum.ADMIN, members.get(0).getUserRole());
    }

    @Test
    void addAcspMemberForbiddenWhenRequestingUserNotMember() throws Exception {
      String newUserId = "newUser";
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "nonMemberUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isForbidden());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberBadRequestWhenInviteeUserDoesNotExist() throws Exception {
      String newUserId = "nonExistentUser";
      when(usersService.doesUserExist(newUserId)).thenReturn(false);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isBadRequest());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberBadRequestWhenInviteeAlreadyMember() throws Exception {
      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          userId)))
          .andExpect(status().isBadRequest());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(userId, "ACSP001");
      Assertions.assertEquals(1, members.size());
    }

    @Test
    void addAcspMemberForbiddenWhenStandardUserAddsAdmin() throws Exception {
      String newUserId = "newAdminUser";
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", userId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"admin\"}",
                          newUserId)))
          .andExpect(status().isForbidden());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberForbiddenWhenAdminAddsOwner() throws Exception {
      String newUserId = "newOwnerUser";
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"owner\"}",
                          newUserId)))
          .andExpect(status().isForbidden());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberSuccessWhenPreviouslyRemovedMember() throws Exception {
      String previouslyRemovedUserId = "previouslyRemovedUser";
      User previouslyRemovedUser = new User();
      previouslyRemovedUser.setUserId(previouslyRemovedUserId);
      previouslyRemovedUser.setEmail("removed@example.com");
      previouslyRemovedUser.setDisplayName("Previously Removed User");
      when(usersService.fetchUserDetails(previouslyRemovedUserId))
          .thenReturn(previouslyRemovedUser);
      when(usersService.doesUserExist(previouslyRemovedUserId)).thenReturn(true);

      AcspMembersDao removedMembership = new AcspMembersDao();
      removedMembership.setUserId(previouslyRemovedUserId);
      removedMembership.setAcspNumber("ACSP001");
      removedMembership.setUserRole(UserRoleEnum.STANDARD);
      removedMembership.setCreatedAt(now.minusDays(10));
      removedMembership.setAddedAt(now.minusDays(10));
      removedMembership.setAddedBy("admin1");
      removedMembership.setRemovedAt(now.minusDays(1));
      removedMembership.setRemovedBy("admin2");
      removedMembership.setEtag(generateEtag());
      acspMembersRepository.save(removedMembership);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          previouslyRemovedUserId)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").exists());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(previouslyRemovedUserId, "ACSP001");
      Assertions.assertEquals(2, members.size());
      Assertions.assertTrue(members.stream().anyMatch(m -> m.getRemovedAt() == null));
    }

    @Test
    void addAcspMemberBadRequestWhenInviteeAlreadyActiveMember() throws Exception {
      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          userId)))
          .andExpect(status().isBadRequest());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(userId, "ACSP001");
      Assertions.assertEquals(1, members.size());
      Assertions.assertNull(members.get(0).getRemovedAt());
    }

    @Test
    void addAcspMemberSuccessWhenInviteeHasRemovedMembershipInDifferentAcsp() throws Exception {
      String newUserId = "userWithRemovedMembership";
      User newUser = new User();
      newUser.setUserId(newUserId);
      newUser.setEmail("removed@example.com");
      newUser.setDisplayName("User With Removed Membership");
      when(usersService.fetchUserDetails(newUserId)).thenReturn(newUser);
      when(usersService.doesUserExist(newUserId)).thenReturn(true);

      AcspMembersDao removedMembership = new AcspMembersDao();
      removedMembership.setUserId(newUserId);
      removedMembership.setAcspNumber("ACSP002");
      removedMembership.setUserRole(UserRoleEnum.STANDARD);
      removedMembership.setCreatedAt(now.minusDays(10));
      removedMembership.setAddedAt(now.minusDays(10));
      removedMembership.setAddedBy("admin1");
      removedMembership.setRemovedAt(now.minusDays(1));
      removedMembership.setRemovedBy("admin2");
      removedMembership.setEtag(generateEtag());
      acspMembersRepository.save(removedMembership);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", adminUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP001\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").exists());

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertEquals(1, members.size());
      Assertions.assertNull(members.get(0).getRemovedAt());
    }
  }

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
    mongoTemplate.dropCollection(AcspDataDao.class);
  }
}
