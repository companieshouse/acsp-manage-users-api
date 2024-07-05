package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
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
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

@WebMvcTest(UserAcspMembership.class)
@Tag("unit-test")
class UserAcspMembershipTest {

  @MockBean private AcspMembershipService acspMembershipService;
  @Autowired private MockMvc mockMvc;
  @MockBean private AcspMembershipListMapper acspMembershipListMapper;
  @MockBean private AcspMembersService acspMembersService;
  @MockBean private UsersService usersService;

  @MockBean private StaticPropertyUtil staticPropertyUtil;

  @Nested
  @DisplayName("GET /acsp-members Tests")
  class GetAcspMembershipTests {
    private User existingUser;
    private AcspMembersDao activeMember;
    private AcspMembersDao removedMember;
    private AcspMembership activeMembership;
    private AcspMembership removedMembership;
    private AcspMembership acspMembership1;

    @BeforeEach
    void setUp() {
      existingUser = new User();
      existingUser.setUserId("existingUser");
      when(usersService.fetchUserDetails(existingUser.getUserId())).thenReturn(existingUser);
      when(usersService.doesUserExist(anyString())).thenReturn(true);

      activeMember = new AcspMembersDao();
      activeMember.setId("active1");
      activeMember.setAcspNumber("ACSP123");
      activeMember.setUserId("existingUser");
      activeMember.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
      activeMember.setAddedAt(LocalDateTime.now().minusDays(30));

      removedMember = new AcspMembersDao();
      removedMember.setId("removed1");
      removedMember.setAcspNumber("ACSP456");
      removedMember.setUserId("existingUser");
      removedMember.setUserRole(AcspMembership.UserRoleEnum.STANDARD);
      removedMember.setAddedAt(LocalDateTime.now().minusDays(60));
      removedMember.setRemovedBy("removedBy1");
      removedMember.setRemovedAt(LocalDateTime.now().minusDays(10));

      activeMembership = new AcspMembership();
      activeMembership.setId("active1");
      activeMembership.setAcspNumber("ACSP123");
      activeMembership.setUserId("existingUser");
      activeMembership.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
      activeMembership.setAddedAt(OffsetDateTime.now().minusDays(30));

      removedMembership = new AcspMembership();
      removedMembership.setId("removed1");
      removedMembership.setAcspNumber("ACSP456");
      removedMembership.setUserId("existingUser");
      removedMembership.setUserRole(AcspMembership.UserRoleEnum.STANDARD);
      removedMembership.setAddedAt(OffsetDateTime.now().minusDays(60));
      removedMembership.setRemovedBy("removedBy1");
      removedMembership.setRemovedAt(OffsetDateTime.now().minusDays(10));

      acspMembership1 = new AcspMembership();
      acspMembership1.setId("acsp1");
      acspMembership1.setAcspNumber("ACSP123");
      acspMembership1.setUserId("existingUser");
      acspMembership1.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
      acspMembership1.setAddedAt(OffsetDateTime.now().minusDays(30));
    }

    @Test
    void testGetAcspMembershipForUserIdExcludeRemoved() throws Exception {
      List<AcspMembership> acspMembershipList = Collections.singletonList(activeMembership);

      when(acspMembersService.fetchAcspMemberships(existingUser, false))
          .thenReturn(acspMembershipList);

      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].id").value("active1"))
          .andExpect(jsonPath("$[0].acsp_number").value("ACSP123"));

      verify(acspMembersService).fetchAcspMemberships(existingUser, false);
    }

    @Test
    void testGetAcspMembershipForUserIdIncludeRemoved() throws Exception {
      List<AcspMembership> acspMembershipList = Arrays.asList(activeMembership, removedMembership);

      when(acspMembersService.fetchAcspMemberships(existingUser, true))
          .thenReturn(acspMembershipList);

      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
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

      verify(acspMembersService).fetchAcspMemberships(existingUser, true);
    }

    @Test
    void testGetAcspMembershipForUserIdNoMemberships() throws Exception {
      when(acspMembersService.fetchAcspMemberships(existingUser, false))
          .thenReturn(Collections.emptyList());

      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));

      verify(acspMembersService).fetchAcspMemberships(existingUser, false);
    }

    @Test
    void testGetAcspMembershipForUserIdMissingHeaders() throws Exception {
      mockMvc
          .perform(get("/acsp-members").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAcspMembershipForUserIdInvalidIncludeRemovedParam() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .param("include_removed", "invalid")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAcspMembershipForUserIdServiceException() throws Exception {
      when(acspMembersService.fetchAcspMemberships(existingUser, false))
          .thenThrow(new RuntimeException("Service error"));

      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetAcspMembershipForUserIdMissingEricIdentity() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAcspMembershipForUserIdMissingEricIdentityType() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAcspMembershipForUserIdNonOauth2User() throws Exception {
      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "key")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAcspMembershipForUserIdUserNotFound() throws Exception {
      when(usersService.fetchUserDetails("existingUser"))
          .thenThrow(new InternalServerErrorRuntimeException("User not found"));

      mockMvc
          .perform(
              get("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", "existingUser")
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError());
    }

    @Test
    void getAcspMembershipForAcspIdTestShouldThrow400ErrorRequestWhenRequestIdNotProvided()
        throws Exception {
      var response =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "acsp1")
                      .header("ERIC-Identity", "existingUser")
                      .header("ERIC-Identity-Type", "oauth2"))
              .andReturn();
      assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void getAcspMembershipForAcspIdTestShouldThrow404ErrorRequestWhenRequestIdMalformed()
        throws Exception {
      var response =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "acsp1")
                      .header("X-Request-Id", "&&&&")
                      .header("ERIC-Identity", "existingUser")
                      .header("ERIC-Identity-Type", "oauth2"))
              .andReturn();
      assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void getAcspMembershipForExistingMemberIdShouldReturnData() throws Exception {
      Mockito.doReturn(Optional.of(acspMembership1))
          .when(acspMembershipService)
          .fetchAcspMembership("acsp1");

      final var responseJson =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "acsp1")
                      .header("X-Request-Id", "theId")
                      .header("ERIC-Identity", "existingUser")
                      .header("ERIC-Identity-Type", "oauth2")
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();

      final var objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      final var response =
          objectMapper.readValue(responseJson.getContentAsByteArray(), AcspMembership.class);

      Assertions.assertEquals("acsp1", response.getId());
    }

    @Test
    void getAcspMembershipForNonExistingMemberIdShouldNotReturnData() throws Exception {

      Mockito.doReturn(Optional.of(acspMembership1))
          .when(acspMembershipService)
          .fetchAcspMembership("acsp3");

      final var responseJson =
          mockMvc
              .perform(
                  get("/acsp-members/{id}", "acsp2")
                      .header("X-Request-Id", "theId")
                      .header("ERIC-Identity", "existingUser")
                      .header("ERIC-Identity-Type", "oauth2")
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isNotFound())
              .andReturn()
              .getResponse();
    }
  }

  @Nested
  @DisplayName("POST /acsp-members Tests")
  class AddAcspMembershipTests {
    private static final String ACSP_NUMBER = "ACSP789";
    private static final String ADMIN_USER_ID = "adminUser";
    private static final String OWNER_USER_ID = "ownerUser";
    private static final String STANDARD_USER_ID = "standardUser";
    private static final String NEW_USER_ID = "newUser";

    @BeforeEach
    void setUp() {
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
    }

    @Test
    void testAddAcspMemberSuccessAdminAddsStandard() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);
      AcspMembersDao addedMember =
          createMemberDao(NEW_USER_ID, AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID)).thenReturn(Optional.empty());
      when(acspMembersService.addAcspMember(requestBodyPost, NEW_USER_ID)).thenReturn(addedMember);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").value(addedMember.getId()));

      verify(acspMembersService).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberSuccessOwnerAddsAdmin() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.ADMIN);

      AcspMembersDao ownerMember =
          createMemberDao(OWNER_USER_ID, AcspMembership.UserRoleEnum.OWNER);
      AcspMembersDao addedMember = createMemberDao(NEW_USER_ID, AcspMembership.UserRoleEnum.ADMIN);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              OWNER_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(ownerMember));
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID)).thenReturn(Optional.empty());
      when(acspMembersService.addAcspMember(requestBodyPost, NEW_USER_ID)).thenReturn(addedMember);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", OWNER_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"admin\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").value(addedMember.getId()));

      verify(acspMembersService).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberForbiddenWhenRequestingUserNotMember() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.empty());

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isForbidden());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberBadRequestWhenInviteeUserDoesNotExist() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(false);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isBadRequest());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberBadRequestWhenInviteeAlreadyMember() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);
      AcspMembersDao existingMember =
          createMemberDao(NEW_USER_ID, AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID))
          .thenReturn(Optional.of(existingMember));

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isBadRequest());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberForbiddenWhenStandardUserAddsAdmin() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.ADMIN);

      AcspMembersDao standardMember =
          createMemberDao(STANDARD_USER_ID, AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              STANDARD_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(standardMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID)).thenReturn(Optional.empty());

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", STANDARD_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"admin\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isForbidden());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberForbiddenWhenAdminAddsOwner() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.OWNER);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID)).thenReturn(Optional.empty());

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"owner\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isForbidden());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberForbiddenWhenRequestingUserNotActiveMember() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.empty());

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isForbidden());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberBadRequestWhenInviteeAlreadyActiveMember() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);
      AcspMembersDao existingMember =
          createMemberDao(NEW_USER_ID, AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID))
          .thenReturn(Optional.of(existingMember));

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isBadRequest());

      verify(acspMembersService, never()).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    @Test
    void testAddAcspMemberSuccessWhenInviteeHasInactiveMembership() throws Exception {
      RequestBodyPost requestBodyPost =
          new RequestBodyPost()
              .userId(NEW_USER_ID)
              .acspNumber(ACSP_NUMBER)
              .userRole(RequestBodyPost.UserRoleEnum.STANDARD);

      AcspMembersDao adminMember =
          createMemberDao(ADMIN_USER_ID, AcspMembership.UserRoleEnum.ADMIN);
      AcspMembersDao addedMember =
          createMemberDao(NEW_USER_ID, AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(
              ADMIN_USER_ID, ACSP_NUMBER))
          .thenReturn(Optional.of(adminMember));
      when(usersService.doesUserExist(NEW_USER_ID)).thenReturn(true);
      when(acspMembersService.fetchActiveAcspMember(NEW_USER_ID)).thenReturn(Optional.empty());
      when(acspMembersService.addAcspMember(requestBodyPost, NEW_USER_ID)).thenReturn(addedMember);

      mockMvc
          .perform(
              post("/acsp-members")
                  .header("X-Request-Id", "test-request-id")
                  .header("ERIC-Identity", ADMIN_USER_ID)
                  .header("ERIC-Identity-Type", "oauth2")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":\"%s\",\"acsp_number\":\"%s\",\"user_role\":\"standard\"}",
                          NEW_USER_ID, ACSP_NUMBER)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.acsp_membership_id").value(addedMember.getId()));

      verify(acspMembersService).addAcspMember(requestBodyPost, NEW_USER_ID);
    }

    private AcspMembersDao createMemberDao(String userId, AcspMembership.UserRoleEnum userRole) {
      AcspMembersDao member = new AcspMembersDao();
      member.setId(userId + "_id");
      member.setAcspNumber(ACSP_NUMBER);
      member.setUserId(userId);
      member.setUserRole(userRole);
      return member;
    }
  }
}
