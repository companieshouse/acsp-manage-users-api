package uk.gov.companieshouse.acsp.manage.users.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(UserAcspMembershipInternal.class)
@Tag("unit-test")
class UserAcspMembershipInternalTest {
  @Autowired public MockMvc mockMvc;
  @MockBean StaticPropertyUtil staticPropertyUtil;
  @MockBean AcspDataService acspDataService;
  @MockBean UsersService usersService;
  @MockBean AcspMembersService acspMembersService;

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
    Mockito.when(
            acspMembersService.fetchAcspMemberships(user1, false))
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
