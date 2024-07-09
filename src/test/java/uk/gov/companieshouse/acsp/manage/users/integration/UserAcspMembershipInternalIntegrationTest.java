package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class UserAcspMembershipInternalIntegrationTest {

  @Container @ServiceConnection
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5");

  @Autowired MongoTemplate mongoTemplate;
  @Autowired MockMvc mockMvc;
  @Autowired AcspMembersRepository acspMembersRepository;
  @Autowired AcspDataRepository acspDataRepository;
  @MockBean UsersService usersService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String acspNumber = "ACSP001";
  private final String ownerUserId = "ownerUser";
  private final String ownerEmail = "owner@example.com";

  @BeforeEach
  void setup() {
    User ownerUser = new User();
    ownerUser.setUserId(ownerUserId);
    ownerUser.setEmail(ownerEmail);
    ownerUser.setDisplayName("Owner User");
    when(usersService.fetchUserDetails(ownerUserId)).thenReturn(ownerUser);

    setupTestData();
  }

  private void setupTestData() {
    AcspDataDao acspData = new AcspDataDao();
    acspData.setId(acspNumber);
    acspData.setAcspName("ACSP 1 Ltd");
    acspData.setAcspStatus("active");
    acspDataRepository.save(acspData);
  }

  @Nested
  @DisplayName("POST /internal/acsp-members/acsp/{acsp_number} Tests")
  class AddAcspOwnerTests {

    @Test
    void addAcspOwnerSuccess() throws Exception {
      UsersList users = new UsersList();
      User ownerUser = new User();
      ownerUser.setUserId(ownerUserId);
      ownerUser.setEmail(ownerEmail);
      users.add(ownerUser);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      MvcResult result =
          mockMvc
              .perform(
                  post("/internal/acsp-members/acsp/" + acspNumber)
                      .header("X-Request-Id", "test-request-id")
                      .header("ERIC-Identity", "internal-api")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.acsp_membership_id").exists())
              .andReturn();

      String responseContent = result.getResponse().getContentAsString();
      String acspMembershipId =
          objectMapper.readTree(responseContent).get("acsp_membership_id").asText();

      List<AcspMembersDao> members =
          acspMembersRepository.findByUserIdAndAcspNumber(ownerUserId, acspNumber);
      assertEquals(1, members.size());
      assertEquals(UserRoleEnum.OWNER, members.get(0).getUserRole());
      assertEquals(acspMembershipId, members.get(0).getId());
    }

    @Test
    void addAcspOwnerFailsWhenUserAlreadyMember() throws Exception {
      UsersList users = new UsersList();
      User ownerUser = new User();
      ownerUser.setUserId(ownerUserId);
      ownerUser.setEmail(ownerEmail);
      users.add(ownerUser);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      AcspMembersDao existingMember = new AcspMembersDao();
      existingMember.setUserId(ownerUserId);
      existingMember.setAcspNumber(acspNumber);
      existingMember.setUserRole(UserRoleEnum.STANDARD);
      existingMember.setEtag(generateEtag());
      acspMembersRepository.save(existingMember);

      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(
              content()
                  .string(
                      containsString(
                          "ACSP for acspNumber "
                              + acspNumber
                              + " and userId "
                              + ownerUserId
                              + " already exists")));
    }

    @Test
    void addAcspOwnerFailsWhenUserNotFound() throws Exception {
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(new UsersList());

      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
          .andExpect(status().isNotFound())
          .andExpect(content().string(containsString("Failed to find user")));
    }

    @Test
    void addAcspOwnerFailsWhenAcspNotFound() throws Exception {
      UsersList users = new UsersList();
      User ownerUser = new User();
      ownerUser.setUserId(ownerUserId);
      ownerUser.setEmail(ownerEmail);
      users.add(ownerUser);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      String nonExistentAcspNumber = "NONEXISTENT001";

      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + nonExistentAcspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.errors[0].error").value(containsString("not found")))
          .andExpect(jsonPath("$.errors[0].error").value(containsString(nonExistentAcspNumber)))
          .andExpect(jsonPath("$.errors[0].type").value("ch:service"));
    }

    @Test
    void addAcspOwnerFailsWithInvalidEmail() throws Exception {
      String invalidEmail = "invalid.email";

      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + invalidEmail + "\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addAcspOwnerFailsWithMissingEmail() throws Exception {
      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addAcspOwnerFailsWithInvalidAcspNumber() throws Exception {
      String invalidAcspNumber = "INVALID!@#$";

      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + invalidAcspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addAcspOwnerFailsWithMissingHeaders() throws Exception {
      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"owner_email\":\"" + ownerEmail + "\"}"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void addAcspOwnerFailsWithInvalidContentType() throws Exception {
      mockMvc
          .perform(
              post("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.TEXT_PLAIN)
                  .content("owner_email=" + ownerEmail))
          .andExpect(status().isUnsupportedMediaType());
    }
  }

  @Nested
  @DisplayName("GET /internal/acsp-members/acsp/{acsp_number} Tests")
  class IsActiveMemberTests {

    @Test
    void isActiveMemberReturnsTrueForActiveMember() throws Exception {
      UsersList users = new UsersList();
      User user = new User();
      user.setUserId(ownerUserId);
      user.setEmail(ownerEmail);
      users.add(user);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      AcspMembersDao activeMember = new AcspMembersDao();
      activeMember.setUserId(ownerUserId);
      activeMember.setAcspNumber(acspNumber);
      activeMember.setUserRole(UserRoleEnum.OWNER);
      activeMember.setEtag(generateEtag());
      acspMembersRepository.save(activeMember);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("true"));
    }

    @Test
    void isActiveMemberReturnsFalseForInactiveAcsp() throws Exception {
      AcspDataDao inactiveAcsp = new AcspDataDao();
      inactiveAcsp.setId(acspNumber + "-inactive");
      inactiveAcsp.setAcspName("Inactive ACSP");
      inactiveAcsp.setAcspStatus("deauthorised");
      acspDataRepository.save(inactiveAcsp);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber + "-inactive")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @Test
    void isActiveMemberReturnsFalseForNonMember() throws Exception {
      UsersList users = new UsersList();
      User user = new User();
      user.setUserId(ownerUserId);
      user.setEmail(ownerEmail);
      users.add(user);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @Test
    void isActiveMemberReturnsNotFoundForNonExistentUser() throws Exception {
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(new UsersList());

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isNotFound());
    }

    @Test
    void isActiveMemberReturnsFalseForInactiveMember() throws Exception {
      UsersList users = new UsersList();
      User user = new User();
      user.setUserId(ownerUserId);
      user.setEmail(ownerEmail);
      users.add(user);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      AcspMembersDao inactiveMember = new AcspMembersDao();
      inactiveMember.setUserId(ownerUserId);
      inactiveMember.setAcspNumber(acspNumber);
      inactiveMember.setUserRole(UserRoleEnum.OWNER);
      inactiveMember.setEtag(generateEtag());
      inactiveMember.setRemovedBy("inactive-as-removed");
      acspMembersRepository.save(inactiveMember);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @Test
    void isActiveMemberReturnsFalseForMemberOfDifferentAcsp() throws Exception {
      UsersList users = new UsersList();
      User user = new User();
      user.setUserId(ownerUserId);
      user.setEmail(ownerEmail);
      users.add(user);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      String differentAcspNumber = "ACSP002";
      AcspMembersDao memberOfDifferentAcsp = new AcspMembersDao();
      memberOfDifferentAcsp.setUserId(ownerUserId);
      memberOfDifferentAcsp.setAcspNumber(differentAcspNumber);
      memberOfDifferentAcsp.setUserRole(UserRoleEnum.OWNER);
      memberOfDifferentAcsp.setEtag(generateEtag());
      acspMembersRepository.save(memberOfDifferentAcsp);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("false"));
    }

    @Test
    void isActiveMemberHandlesMultipleMembershipsCorrectly() throws Exception {
      UsersList users = new UsersList();
      User user = new User();
      user.setUserId(ownerUserId);
      user.setEmail(ownerEmail);
      users.add(user);
      when(usersService.searchUserDetails(List.of(ownerEmail))).thenReturn(users);

      AcspMembersDao activeMember = new AcspMembersDao();
      activeMember.setUserId(ownerUserId);
      activeMember.setAcspNumber(acspNumber);
      activeMember.setUserRole(UserRoleEnum.OWNER);
      activeMember.setEtag(generateEtag());
      acspMembersRepository.save(activeMember);

      AcspMembersDao inactiveMember = new AcspMembersDao();
      inactiveMember.setUserId(ownerUserId);
      inactiveMember.setAcspNumber(acspNumber);
      inactiveMember.setUserRole(UserRoleEnum.ADMIN);
      inactiveMember.setEtag(generateEtag());
      inactiveMember.setRemovedBy("inactive-as-removed");
      acspMembersRepository.save(inactiveMember);

      AcspMembersDao differentAcspMember = new AcspMembersDao();
      differentAcspMember.setUserId(ownerUserId);
      differentAcspMember.setAcspNumber("ACSP002");
      differentAcspMember.setUserRole(UserRoleEnum.OWNER);
      differentAcspMember.setEtag(generateEtag());
      acspMembersRepository.save(differentAcspMember);

      mockMvc
          .perform(
              get("/internal/acsp-members/acsp/" + acspNumber)
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "internal-api")
                  .header("ERIC-Identity-Type", "key")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .header("user_email", ownerEmail))
          .andExpect(status().isOk())
          .andExpect(content().string("true"));
    }
  }

  @Nested
  @DisplayName("PATCH /internal/acsp-members/acsp/{acsp_number} Tests")
  class PerformActionOnAcspTests {
    // TODO: Implement tests for performActionOnAcsp endpoint when it's implemented
  }

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
    mongoTemplate.dropCollection(AcspDataDao.class);
  }
}
