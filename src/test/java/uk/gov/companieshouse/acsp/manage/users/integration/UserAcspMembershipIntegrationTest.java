package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
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

  private final TestDataManager testDataManager = TestDataManager.getInstance();

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

      assertEquals("acsp1", responseMembership.getId());
      assertEquals("user123", responseMembership.getUserId());
      assertEquals("ACSP123", responseMembership.getAcspNumber());
      assertEquals("admin1", responseMembership.getAddedBy());
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
    void addAcspMemberThrowsBadRequestWhenAcspIsDeauthorised() throws Exception {
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
                          "{\"user_id\":\"%s\",\"acsp_number\":\"ACSP003\",\"user_role\":\"standard\"}",
                          newUserId)))
          .andExpect(status().isBadRequest())
          .andExpect(
              result ->
                  Assertions.assertInstanceOf(
                      BadRequestRuntimeException.class, result.getResolvedException()))
          .andExpect(
              result ->
                  assertEquals(
                      "ACSP is currently deauthorised, cannot add users",
                      result.getResolvedException().getMessage()));

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
      assertEquals(1, members.size());
      assertEquals(UserRoleEnum.STANDARD, members.get(0).getUserRole());
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
      assertEquals(1, members.size());
      assertEquals(UserRoleEnum.ADMIN, members.get(0).getUserRole());
    }

    @Test
    void addAcspMemberThrowsBadRequestWhenRequestingUserNotMember() throws Exception {
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
          .andExpect(status().isBadRequest())
          .andExpect(
              result ->
                  assertTrue(result.getResolvedException() instanceof BadRequestRuntimeException))
          .andExpect(
              result ->
                  assertEquals(
                      "Requesting user is not an active ACSP member",
                      result.getResolvedException().getMessage()));

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberThrowsNotFoundWhenInviteeUserDoesNotExist() throws Exception {
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
          .andExpect(status().isNotFound())
          .andExpect(
              result ->
                  Assertions.assertInstanceOf(
                      NotFoundRuntimeException.class, result.getResolvedException()))
          .andExpect(
              result ->
                  assertEquals(
                      "Invitee user does not exist", result.getResolvedException().getMessage()));

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberThrowsBadRequestWhenInviteeAlreadyMember() throws Exception {
      when(usersService.doesUserExist(userId)).thenReturn(true);

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
          .andExpect(status().isBadRequest())
          .andExpect(
              result ->
                  Assertions.assertInstanceOf(
                      BadRequestRuntimeException.class, result.getResolvedException()))
          .andExpect(
              result ->
                  assertEquals(
                      "Invitee is already an active ACSP member",
                      result.getResolvedException().getMessage()));

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(userId, "ACSP001");
      Assertions.assertEquals(1, members.size());
    }

    @Test
    void addAcspMemberThrowsBadRequestWhenStandardUserAddsAdmin() throws Exception {
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
          .andExpect(status().isBadRequest())
          .andExpect(
              result ->
                  assertTrue(result.getResolvedException() instanceof BadRequestRuntimeException))
          .andExpect(
              result ->
                  assertEquals(
                      "Requesting user does not have permission to add user with specified role",
                      result.getResolvedException().getMessage()));

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(newUserId, "ACSP001");
      Assertions.assertTrue(members.isEmpty());
    }

    @Test
    void addAcspMemberThrowsBadRequestWhenAdminAddsOwner() throws Exception {
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
          .andExpect(status().isBadRequest())
          .andExpect(
              result ->
                  assertTrue(result.getResolvedException() instanceof BadRequestRuntimeException))
          .andExpect(
              result ->
                  assertEquals(
                      "Requesting user does not have permission to add user with specified role",
                      result.getResolvedException().getMessage()));

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
      assertEquals(2, members.size());
      Assertions.assertTrue(members.stream().anyMatch(m -> m.getRemovedAt() == null));
    }

    @Test
    void addAcspMemberBadRequestWhenInviteeAlreadyActiveMember() throws Exception {
      when(usersService.doesUserExist(userId)).thenReturn(true);

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
      assertEquals(1, members.size());
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
      assertEquals(1, members.size());
      Assertions.assertNull(members.get(0).getRemovedAt());
    }
  }

  @Test
  void updateAcspMembershipForIdWithNullXRequestIdReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWithMalformedTargetAcspMemberIdReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/£" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWithNonexistentTargetAcspMemberIdReturnsNotFound() throws Exception {
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isNotFound() );
  }

  @Test
  void updateAcspMembershipForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWithoutActionReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWithMalformedActionReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"dance\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWithoutEricIdentityReturnsUnauthorised() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isUnauthorized() );
  }

  @Test
  void updateAcspMembershipForIdWithMalformedEricIdentityReturnsForbidden() throws Exception {
    Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Cannot find user" ) ).when( usersService ).fetchUserDetails( "£££" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "£££")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isForbidden() );
  }

  @Test
  void updateAcspMembershipForIdWithNonexistentEricIdentityReturnsForbidden() throws Exception {
    Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Cannot find user" ) ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isForbidden() );
  }

  @Test
  void updateAcspMembershipForIdWhereActionIsEditRoleButUserRoleIsNullReturnsBadRequest() throws Exception {
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT002", "WIT004" ) );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereActionIsEditRoleAndUserRoleIsMalformedReturnsBadRequest() throws Exception {
    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"chef\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAssociationBetweenEricIdentityAndAcspDoesNotExistReturnsBadRequest() throws Exception {
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT002" ) );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToRemoveOwnerSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMember = acspMembersRepository.findById( "WIT001" ).get();

    Assertions.assertNotNull( updatedAcspMember.getRemovedAt() );
    Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedAcspMember.getRemovedBy() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMember.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToRemoveAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT002", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMember = acspMembersRepository.findById( "WIT002" ).get();

    Assertions.assertNotNull( updatedAcspMember.getRemovedAt() );
    Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedAcspMember.getRemovedBy() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMember.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToRemoveStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT003", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMember = acspMembersRepository.findById( "WIT003" ).get();

    Assertions.assertNotNull( updatedAcspMember.getRemovedAt() );
    Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedAcspMember.getRemovedBy() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMember.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToRemoveOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI001", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToRemoveAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI002", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMember = acspMembersRepository.findById( "NEI002" ).get();

    Assertions.assertNotNull( updatedAcspMember.getRemovedAt() );
    Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedAcspMember.getRemovedBy() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMember.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToRemoveStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI003", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMember = acspMembersRepository.findById( "NEI003" ).get();

    Assertions.assertNotNull( updatedAcspMember.getRemovedAt() );
    Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedAcspMember.getRemovedBy() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMember.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToRemoveOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME001", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToRemoveAdminReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME002", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToRemoveStandardReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME003", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeOwnerToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeAdminToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT002", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeStandardToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT003", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeOwnerToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI001", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeAdminToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI002", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeStandardToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI003", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeOwnerToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME001", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeAdminToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME002", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeStandardToOwnerReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME003", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"owner\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeOwnerToAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT001" ).get();

    Assertions.assertEquals( UserRoleEnum.ADMIN, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeAdminToAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT002", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT002" ).get();

    Assertions.assertEquals( UserRoleEnum.ADMIN, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeStandardToAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT003", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT003" ).get();

    Assertions.assertEquals( UserRoleEnum.ADMIN, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeOwnerToAdminReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI001", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeAdminToAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI002", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "NEI002" ).get();

    Assertions.assertEquals( UserRoleEnum.ADMIN, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeStandardToAdminSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI003", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "NEI003" ).get();

    Assertions.assertEquals( UserRoleEnum.ADMIN, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeOwnerToAdminReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME001", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeAdminToAdminReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME002", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeStandardToAdminReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME003", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"admin\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeOwnerToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT001" ).get();

    Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeAdminToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT002", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT002" ).get();

    Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereOwnerAttemptsToChangeStandardToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT003", "WIT004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "WIT003" ).get();

    Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeOwnerToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI001", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeAdminToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI002", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "NEI002" ).get();

    Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereAdminAttemptsToChangeStandardToStandardSucceeds() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "NEI003", "NEI004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/NEI003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isOk() );

    final var updatedAcspMembersDao = acspMembersRepository.findById( "NEI003" ).get();

    Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
    Assertions.assertNotEquals( acspMemberDaos.getFirst().getEtag(), updatedAcspMembersDao.getEtag() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeOwnerToStandardReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME001", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME001" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeAdminToStandardReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME002", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME002" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdWhereStandardAttemptsToChangeStandardToStandardReturnsBadRequest() throws Exception {
    final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME003", "XME004" );

    acspMembersRepository.insert( acspMemberDaos );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/XME003" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\",\"user_role\":\"standard\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdReturnsBadRequestWhenOwnerAttemptsToRemoveThemselves() throws Exception {
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT004" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"remove_member\"}" ) )
            .andExpect( status().isBadRequest() );
  }

  @Test
  void updateAcspMembershipForIdReturnsBadRequestWhenOwnerAttemptsToChangeTheirOwnRole() throws Exception {
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos(  "WIT004" ) );
    Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

    mockMvc.perform( patch( "/acsp-members/WIT004" )
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*")
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( "{\"action\":\"edit_role\", \"user_role\":\"standard\" }" ) )
            .andExpect( status().isBadRequest() );
  }

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
    mongoTemplate.dropCollection(AcspDataDao.class);
  }
}
