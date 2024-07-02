package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@WebMvcTest(UserAcspMembership.class)
class UserAcspMembershipTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AcspMembershipListMapper acspMembershipListMapper;

  @MockBean private AcspMembersService acspMembersService;

  @MockBean private UsersService usersService;

  @MockBean private StaticPropertyUtil staticPropertyUtil;

  private AcspMembersDao activeMember;
  private AcspMembersDao removedMember;
  private AcspMembership activeMembership;
  private AcspMembership removedMembership;

  private User user1 = new User();

  @BeforeEach
  void setUp() {
    user1.setUserId("user1");
    Mockito.doReturn(user1).when(usersService).fetchUserDetails(user1.getUserId());

    activeMember = new AcspMembersDao();
    activeMember.setId("active1");
    activeMember.setAcspNumber("ACSP123");
    activeMember.setUserId("user1");
    activeMember.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
    activeMember.setAddedAt(LocalDateTime.now().minusDays(30));

    removedMember = new AcspMembersDao();
    removedMember.setId("removed1");
    removedMember.setAcspNumber("ACSP456");
    removedMember.setUserId("user1");
    removedMember.setUserRole(AcspMembership.UserRoleEnum.STANDARD);
    removedMember.setAddedAt(LocalDateTime.now().minusDays(60));
    removedMember.setRemovedBy("removed_by_1");
    removedMember.setRemovedAt(LocalDateTime.now().minusDays(10));

    activeMembership = new AcspMembership();
    activeMembership.setId("active1");
    activeMembership.setAcspNumber("ACSP123");
    activeMembership.setUserId("user1");
    activeMembership.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
    activeMembership.setAddedAt(OffsetDateTime.now().minusDays(30));

    removedMembership = new AcspMembership();
    removedMembership.setId("removed1");
    removedMembership.setAcspNumber("ACSP456");
    removedMembership.setUserId("user1");
    removedMembership.setUserRole(AcspMembership.UserRoleEnum.STANDARD);
    removedMembership.setAddedAt(OffsetDateTime.now().minusDays(60));
    removedMembership.setRemovedBy("removed_by_1");
    removedMembership.setRemovedAt(OffsetDateTime.now().minusDays(10));
  }

  @Test
  void testGetAcspMembershipForUserIdExcludeRemoved() throws Exception {
    List<AcspMembership> acspMembershipList = Collections.singletonList(activeMembership);

    when(acspMembersService.fetchAcspMemberships(user1, false)).thenReturn(acspMembershipList);

    mockMvc
        .perform(
            get("/acsp-members")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity", "user1")
                .header("ERIC-Identity-Type", "oauth2")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("active1"))
        .andExpect(jsonPath("$[0].acsp_number").value("ACSP123"));

    verify(acspMembersService).fetchAcspMemberships(user1, false);
  }

  @Test
  void testGetAcspMembershipForUserIdIncludeRemoved() throws Exception {
    List<AcspMembership> acspMembershipList = Arrays.asList(activeMembership, removedMembership);

    when(acspMembersService.fetchAcspMemberships(user1, true)).thenReturn(acspMembershipList);

    mockMvc
        .perform(
            get("/acsp-members")
                .header("X-Request-Id", "test-request-id")
                .header("ERIC-Identity", "user1")
                .header("ERIC-Identity-Type", "oauth2")
                .param("include_removed", "true")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("active1"))
        .andExpect(jsonPath("$[1].id").value("removed1"))
        .andExpect(jsonPath("$[1].removed_at").exists());

    verify(acspMembersService).fetchAcspMemberships(user1, true);
  }
}
