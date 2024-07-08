package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum.ACTIVE;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@WebMvcTest(UserAcspMembershipInternal.class)
@Tag("unit-test")
class UserAcspMembershipInternalTest {
  @Autowired public MockMvc mockMvc;
  @MockBean StaticPropertyUtil staticPropertyUtil;
  @MockBean AcspDataService acspDataService;
  @MockBean UsersService usersService;
  @MockBean AcspMembersService acspMembersService;

  @Nested
  @DisplayName("POST /internal/acsp-members/acsp/{acsp_number} Tests")
  class AddAcspOwnerTests {

    private UsersList users;
    private final User user1 = new User();

    @BeforeEach
    void setUp() {
      user1.setUserId("user1");
      when(usersService.fetchUserDetails(user1.getUserId())).thenReturn(user1);
    }

    @Test
    void addAcspOwnerWithoutHeadersReturnsUnauthorised() throws Exception {
      // Given
      String acspNumber = "1122334455";
      String ownerEmail = "j.smith@test.com";
      String payload = String.format("{\"owner_email\":\"%s\"}", ownerEmail);
      String url = String.format("/internal/acsp-members/acsp/%s", acspNumber);

      // When
      var response =
          mockMvc
              .perform(post(url).contentType(MediaType.APPLICATION_JSON).content(payload))
              .andReturn();

      // Then
      assertEquals(401, response.getResponse().getStatus());
    }

    @Test
    void addAcspOwnerReturnsBadRequestIfAcspMembersAlreadyExists() throws Exception {
      // Given
      String acspNumber = "1122334455";
      String ownerEmail = "j.smith@test.com";
      String payload = String.format("{\"owner_email\":\"%s\"}", ownerEmail);
      String url = String.format("/internal/acsp-members/acsp/%s", acspNumber);
      users = new UsersList();
      users.add(user1);
      final var acspMembership = new AcspMembership();
      acspMembership.setAcspNumber(acspNumber);
      Mockito.when(usersService.searchUserDetails(Mockito.any())).thenReturn(users);
      Mockito.when(acspMembersService.fetchAcspMemberships(user1, false))
          .thenReturn(List.of(acspMembership));

      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(payload))
              .andReturn();

      // Then
      assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void addAcspOwnerReturnsNotFoundIfNoUsersListReturnedFromUsersService() throws Exception {
      // Given
      Mockito.when(acspDataService.fetchAcspData(Mockito.any())).thenReturn(new AcspDataDao());
      String acspNumber = "1122334455";
      String ownerEmail = "j.smith@test.com";
      Mockito.when(usersService.searchUserDetails(Mockito.any())).thenReturn(null);
      String payload = String.format("{\"owner_email\":\"%s\"}", ownerEmail);
      String url = String.format("/internal/acsp-members/acsp/%s", acspNumber);

      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(payload))
              .andReturn();

      // Then
      assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void addAcspOwnerReturnsNotFoundIfNoUserWithProvidedUserEmailFound() throws Exception {
      // Given
      Mockito.when(acspDataService.fetchAcspData(Mockito.any())).thenReturn(new AcspDataDao());
      String acspNumber = "1122334455";
      String ownerEmail = "j.smith@test.com";
      users = new UsersList();
      Mockito.when(usersService.searchUserDetails(Mockito.any())).thenReturn(users);
      String payload = String.format("{\"owner_email\":\"%s\"}", ownerEmail);
      String url = String.format("/internal/acsp-members/acsp/%s", acspNumber);

      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(payload))
              .andReturn();

      // Then
      assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void addAcspOwnerAddsAcspOwnerToAcspAndReturnsAcspMembersIdAndCreatedStatus() throws Exception {
      // Given
      Mockito.when(acspDataService.fetchAcspData(Mockito.any())).thenReturn(new AcspDataDao());
      String acspNumber = "1122334455";
      String ownerEmail = "j.smith@test.com";
      users = new UsersList();
      users.add(new User());
      Mockito.when(usersService.searchUserDetails(Mockito.any())).thenReturn(users);
      Mockito.when(acspMembersService.createAcspMembersWithOwnerRole(Mockito.any(), Mockito.any()))
          .thenReturn(new AcspMembersDao());
      String payload = String.format("{\"owner_email\":\"%s\"}", ownerEmail);
      String url = String.format("/internal/acsp-members/acsp/%s", acspNumber);

      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(payload))
              .andReturn();

      // Then
      assertEquals(201, response.getResponse().getStatus());
      assertTrue(response.getResponse().getContentAsString().contains("acsp_membership_id"));
    }
  }

  @Nested
  @DisplayName("GET /internal/acsp-members/acsp/{acsp_number} Tests")
  class IsActiveMemberTests {

    private static final String ACSP_NUMBER = "1122334455";
    private static final String USER_EMAIL = "j.smith@test.com";
    private static final String URL = "/internal/acsp-members/acsp/" + ACSP_NUMBER;

    @Test
    void isActiveMemberWithoutRequiredHeadersReturnsUnauthorised() throws Exception {
      // When
      var response = mockMvc.perform(get(URL)).andReturn();

      // Then
      assertEquals(401, response.getResponse().getStatus());
    }

    @Test
    void isActiveMemberWithoutUserEmailHeaderReturnsBadRequest() throws Exception {
      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*"))
              .andReturn();

      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains("Required header 'user_email' is not present."));
    }

    @Test
    void isActiveMemberReturnsFalseIfAcspIsNotActive() throws Exception {
      // Given
      AcspDataDao acspDataDao = new AcspDataDao();
      acspDataDao.setAcspStatus("INACTIVE");
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header("user_email", USER_EMAIL))
              .andReturn();

      // Then
      assertEquals(200, response.getResponse().getStatus());
      assertEquals("false", response.getResponse().getContentAsString());
    }

    @Test
    void isActiveMemberReturnsNotFoundIfUserNotFound() throws Exception {
      // Given
      AcspDataDao acspDataDao = new AcspDataDao();
      acspDataDao.setAcspStatus("active");
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);
      when(usersService.searchUserDetails(List.of(USER_EMAIL))).thenReturn(new UsersList());

      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header("user_email", USER_EMAIL))
              .andReturn();

      // Then
      assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void isActiveMemberReturnsFalseIfUserIsNotActiveMember() throws Exception {
      // Given
      AcspDataDao acspDataDao = new AcspDataDao();
      acspDataDao.setAcspStatus("ACTIVE");
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      UsersList users = new UsersList();
      User user = new User();
      user.setUserId("user1");
      users.add(user);
      when(usersService.searchUserDetails(List.of(USER_EMAIL))).thenReturn(users);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber("user1", ACSP_NUMBER))
          .thenReturn(Optional.empty());

      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header("user_email", USER_EMAIL))
              .andReturn();

      // Then
      assertEquals(200, response.getResponse().getStatus());
      assertEquals("false", response.getResponse().getContentAsString());
    }

    @Test
    void isActiveMemberReturnsTrueIfUserIsActiveMember() throws Exception {
      // Given
      AcspDataDao acspDataDao = new AcspDataDao();
      acspDataDao.setAcspStatus(ACTIVE.getValue());
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      UsersList users = new UsersList();
      User user = new User();
      user.setUserId("user1");
      users.add(user);
      when(usersService.searchUserDetails(List.of(USER_EMAIL))).thenReturn(users);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber("user1", ACSP_NUMBER))
          .thenReturn(Optional.of(new AcspMembersDao()));

      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header("user_email", USER_EMAIL))
              .andReturn();

      // Then
      assertEquals(200, response.getResponse().getStatus());
      assertEquals("true", response.getResponse().getContentAsString());
    }

    @Test
    void isActiveMemberReturnsNotFoundForNonExistentAcsp() throws Exception {
      // Given
      when(acspDataService.fetchAcspData(ACSP_NUMBER))
          .thenThrow(
              new NotFoundRuntimeException(
                  StaticPropertyUtil.APPLICATION_NAMESPACE, "ACSP not found"));

      // When
      var response =
          mockMvc
              .perform(
                  get(URL)
                      .header("X-Request-Id", "theId123")
                      .header("ERIC-Identity", "user1")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header("user_email", USER_EMAIL))
              .andReturn();

      // Then
      assertEquals(404, response.getResponse().getStatus());
      assertTrue(response.getResponse().getContentAsString().contains("ACSP not found"));
    }
  }

  @Nested
  @DisplayName("PATCH /internal/acsp-members/acsp/{acsp_number} Tests")
  class PerformActionOnAcspTests {
    // TODO: Implement tests for performActionOnAcsp endpoint
  }
}