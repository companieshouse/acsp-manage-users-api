package uk.gov.companieshouse.acsp.manage.users.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAcspMembershipController.class)
@Tag("unit-test")
class UserAcspMembershipControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StaticPropertyUtil staticPropertyUtil;

  @MockBean private UsersService usersService;

  @MockBean private AcspMembersService acspMembersService;

  private final TestDataManager testDataManager = TestDataManager.getInstance();
  private User existingUser;
  private String userId;
  private AcspMembershipsList acspMembershipsList;

  @BeforeEach
  void setUp() {
    userId = "COMU002";
    existingUser = new User();
    existingUser.setUserId(userId);
    when(usersService.fetchUserDetails(existingUser.getUserId())).thenReturn(existingUser);
    when(usersService.doesUserExist(anyString())).thenReturn(true);
    UserContext.setLoggedUser(existingUser);
    acspMembershipsList = new AcspMembershipsList();
  }

  @Test
  void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/user/acsps/memberships")
                .header("Eric-identity", userId)
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getAcspMembershipsForUserIdWithWrongIncludeRemovedParameterInBodyReturnsBadRequest()
      throws Exception {
    mockMvc
        .perform(
            get("/user/acsps/memberships?include_removed=null")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", userId)
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getAcspMembershipsForUserIdWhoHasNoAcspMemebershipsReturnsEmptyAcspMembershipsList()
      throws Exception {
    when(usersService.fetchUserDetails(userId))
        .thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

    acspMembershipsList.setItems(List.of());
    when(acspMembersService.fetchAcspMemberships(existingUser, false))
        .thenReturn(acspMembershipsList);

    mockMvc
        .perform(
            get("/user/acsps/memberships")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", userId)
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(0));
    verify(acspMembersService).fetchAcspMemberships(existingUser, false);
  }

  @Test
  void getAcspMembershipsForUserIdWhoHasNoAcspMemebershipsReturnsNonEmptyAcspMembershipsList()
      throws Exception {
    when(usersService.fetchUserDetails(userId))
        .thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

    final var acspMembership = new AcspMembership();
    acspMembership.setUserId(userId);
    acspMembershipsList.setItems(List.of(acspMembership));
    when(acspMembersService.fetchAcspMemberships(existingUser, false))
        .thenReturn(acspMembershipsList);

    mockMvc
        .perform(
            get("/user/acsps/memberships")
                .header("X-Request-Id", "theId123")
                .header("Eric-identity", userId)
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].user_id").value(userId));
    verify(acspMembersService).fetchAcspMemberships(existingUser, false);
  }
}
