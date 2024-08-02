package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.ERROR_CODE_1001;
import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.ERROR_CODE_1002;

import java.util.Collections;
import java.util.List;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@ExtendWith(MockitoExtension.class)
class AcspMembershipsControllerTest {

  @Mock private UsersService usersService;

  @Mock private AcspDataService acspDataService;

  @Mock private AcspMembersService acspMembersService;

  @InjectMocks private AcspMembershipsController acspMembershipsController;

  private static final String REQUEST_ID = "test-request-id";
  private static final String ACSP_NUMBER = "COMA001";

  @Nested
  class GetMembersForAcspTests {

    @Test
    void validRequest_ReturnsOk() {
      final boolean includeRemoved = false;
      final int pageIndex = 0;
      final int itemsPerPage = 15;
      final String role = "owner";

      final AcspDataDao acspDataDao = new AcspDataDao();
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      AcspMembershipsList expectedList = new AcspMembershipsList();
      when(acspMembersService.findAllByAcspNumberAndRole(
              ACSP_NUMBER, acspDataDao, role, includeRemoved, pageIndex, itemsPerPage))
          .thenReturn(expectedList);

      ResponseEntity<AcspMembershipsList> response =
          acspMembershipsController.getMembersForAcsp(
              ACSP_NUMBER, REQUEST_ID, includeRemoved, pageIndex, itemsPerPage, role);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(expectedList, response.getBody());
    }

    @Test
    void invalidRole_ThrowsBadRequestException() {
      final String invalidRole = "invalid_role";

      assertThrows(
          BadRequestRuntimeException.class,
          () ->
              acspMembershipsController.getMembersForAcsp(
                  ACSP_NUMBER, REQUEST_ID, false, 0, 15, invalidRole));
    }

    @Test
    void nullRole_ReturnsOk() {
      final Boolean includeRemoved = true;
      final Integer pageIndex = 0;
      final Integer itemsPerPage = 20;
      final String role = null;

      final AcspDataDao acspDataDao = new AcspDataDao();
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      final AcspMembershipsList expectedList = new AcspMembershipsList();
      when(acspMembersService.findAllByAcspNumberAndRole(
              ACSP_NUMBER, acspDataDao, role, includeRemoved, pageIndex, itemsPerPage))
          .thenReturn(expectedList);

      ResponseEntity<AcspMembershipsList> response =
          acspMembershipsController.getMembersForAcsp(
              ACSP_NUMBER, REQUEST_ID, includeRemoved, pageIndex, itemsPerPage, role);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(expectedList, response.getBody());
    }
  }

  @Nested
  class FindMembershipsForUserAndAcspTests {

    @Test
    void validRequest_ReturnsOk() {
      final String userEmail = "test@example.com";
      final boolean includeRemoved = false;
      final RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(userEmail);

      final User user = new User();
      user.setEmail(userEmail);
      final UsersList usersList = new UsersList();
      usersList.add(user);
      when(usersService.searchUserDetails(List.of(userEmail))).thenReturn(usersList);

      AcspDataDao acspDataDao = new AcspDataDao();
      when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

      AcspMembershipsList expectedList = new AcspMembershipsList();
      when(acspMembersService.fetchAcspMemberships(user, includeRemoved, ACSP_NUMBER))
          .thenReturn(expectedList);

      ResponseEntity<AcspMembershipsList> response =
          acspMembershipsController.findMembershipsForUserAndAcsp(
              REQUEST_ID, ACSP_NUMBER, includeRemoved, requestBody);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(expectedList, response.getBody());
    }

    @Test
    void nullUserEmail_ThrowsBadRequestException() {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(null);

      assertThrows(
          BadRequestRuntimeException.class,
          () ->
              acspMembershipsController.findMembershipsForUserAndAcsp(
                  REQUEST_ID, ACSP_NUMBER, false, requestBody));
    }

    @Test
    void userNotFound_ThrowsNotFoundException() {
      final String userEmail = "nonexistent@example.com";
      final RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(userEmail);

      when(usersService.searchUserDetails(List.of(userEmail))).thenReturn(new UsersList());

      assertThrows(
          NotFoundRuntimeException.class,
          () ->
              acspMembershipsController.findMembershipsForUserAndAcsp(
                  REQUEST_ID, ACSP_NUMBER, false, requestBody));
    }
  }

  @Nested
  @WebMvcTest(AcspMembershipsController.class)
  @Tag("unit-test")
  class AddMemberForAcsp {
    @Autowired private MockMvc mockMvc;

    @MockBean ApiClientService apiClientService;

    @MockBean InternalApiClient internalApiClient;

    @MockBean private UsersService usersService;

    @MockBean private AcspDataService acspDataService;

    @MockBean private StaticPropertyUtil staticPropertyUtil;

    @MockBean private AcspMembersService acspMembersService;

    private final TestDataManager testDataManager = TestDataManager.getInstance();
    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN =
        "Please check the request and try again";

    private String userIdFromRequest;
    private String userRoleFromRequest;
    private String acspNumberFromPath;
    private String loggedUserId;
    private AcspMembershipsList loggedUserAcspMembershipsList;
    private AcspMembershipsList newUserAcspMembershipsList;
    private String url;

    private void setTestEnvironment(
        String userIdFromRequest,
        String acspNumberFromPath,
        String loggedUserId,
        String[] loggedUserAcspMembersDaoIds,
        String[] newUserAcspMembersDaoIds,
        boolean hasActiveAcspMemberships) {
      // Set loggedUser
      User loggedUser = testDataManager.fetchUserDtos(loggedUserId).getFirst();
      UserContext.setLoggedUser(loggedUser);
      when(usersService.fetchUserDetails(loggedUserId)).thenReturn(loggedUser);
      when(usersService.doesUserExist(anyString())).thenReturn(true);

      // Set loggedUser data
      final var loggedUserAcspMembersDaos =
          testDataManager.fetchAcspMembersDaos(loggedUserAcspMembersDaoIds);
      final var loggedUserAcspMemberships = getAcspMemberships(loggedUserAcspMembersDaos);
      loggedUserAcspMembershipsList.setItems(loggedUserAcspMemberships);
      when(acspMembersService.fetchAcspMemberships(eq(loggedUser), Mockito.anyBoolean()))
          .thenReturn(loggedUserAcspMembershipsList);

      // Set newUser
      final var newUser = testDataManager.fetchUserDtos(userIdFromRequest).getFirst();
      when(usersService.fetchUserDetails(userIdFromRequest)).thenReturn(newUser);

      // Set newUser data
      List<AcspMembership> newUserAcspMemberships;
      if (hasActiveAcspMemberships) {
        final var newUserAcspMembersDaos =
            testDataManager.fetchAcspMembersDaos(newUserAcspMembersDaoIds);
        newUserAcspMemberships = getAcspMemberships(newUserAcspMembersDaos);
      } else {
        newUserAcspMemberships = Collections.emptyList();
      }
      newUserAcspMembershipsList.setItems(newUserAcspMemberships);
      when(acspMembersService.fetchAcspMemberships(eq(newUser), Mockito.anyBoolean()))
          .thenReturn(newUserAcspMembershipsList);

      // Set acspDataDao
      final var acspDataDaos = testDataManager.fetchAcspDataDaos(acspNumberFromPath);
      when(acspDataService.fetchAcspData(acspNumberFromPath)).thenReturn(acspDataDaos.getFirst());

      // Set addedAcspMembership
      final var newAcspMembership = new AcspMembership();
      newAcspMembership.setAcspNumber(acspNumberFromPath);
      newAcspMembership.setUserId(userIdFromRequest);
      newAcspMembership.setAddedBy(loggedUserId);
      newAcspMembership.setUserRole(AcspMembership.UserRoleEnum.STANDARD);

      when(acspMembersService.addAcspMembership(
              eq(newUser), Mockito.any(), eq(acspNumberFromPath), Mockito.any(), eq(loggedUserId)))
          .thenReturn(newAcspMembership);
    }

    private List<AcspMembership> getAcspMemberships(List<AcspMembersDao> acspMembersDaos) {
      return acspMembersDaos.stream()
          .map(
              dao -> {
                final var acspMembership = new AcspMembership();
                acspMembership.setUserId(dao.getUserId());
                acspMembership.setAcspNumber(dao.getAcspNumber());
                acspMembership.setUserRole(
                    AcspMembership.UserRoleEnum.fromValue(dao.getUserRole()));
                return acspMembership;
              })
          .toList();
    }

    @BeforeEach
    void setUp() {
      loggedUserAcspMembershipsList = new AcspMembershipsList();
      newUserAcspMembershipsList = new AcspMembershipsList();
      userRoleFromRequest = "standard";
      acspNumberFromPath = "TSA001";
      loggedUserId = "COMU002";
      userIdFromRequest = "COMU001";
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
    }

    @Test
    void addMemberForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
      // Given preset data
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains("Required header 'X-Request-Id' is not present."));
    }

    @ParameterizedTest
    @CsvSource({"abc-111-&,TSA001", "COMU001,TSA001-&"})
    void addMemberForAcspWithMalformedUserIdInBodyOrMalformedAcspNumberInUrlReturnsBadRequest(
        String id, String acspNumber) throws Exception {
      // Given
      userIdFromRequest = id;
      acspNumberFromPath = acspNumber;
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void addMemberForAcspWithoutUserIdInBodyReturnsBadRequest() throws Exception {
      // Given
      userIdFromRequest = null;
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":%s,\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN));
    }

    @Test
    void addMemberForAcspWithoutUserRoleReturnsBadRequest() throws Exception {
      // Given
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      userRoleFromRequest = null;
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":%s}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void addMemberForAcspWithNonexistentUserRoleReturnsBadRequest() throws Exception {
      // Given
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      userRoleFromRequest = "superuser";
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void addMemberForAcspWithNonexistentAcspNumberReturnsBadRequest() throws Exception {
      // Given
      acspNumberFromPath = "NONEXISTENT";
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      when(acspDataService.fetchAcspData(acspNumberFromPath))
          .thenThrow(new NotFoundRuntimeException("", ""));
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains("Please check the request and try again"));
    }

    @Test
    void addMemberForAcspWithNonexistentUserIdReturnsBadRequest() throws Exception {
      // Given
      userIdFromRequest = "NONEXISTENT";
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      when(usersService.fetchUserDetails(userIdFromRequest))
          .thenThrow(new NotFoundRuntimeException("", ""));
      setTestEnvironment(
          "COMU001", acspNumberFromPath, loggedUserId, new String[] {}, new String[] {}, false);
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(response.getResponse().getContentAsString().contains(ERROR_CODE_1001.getCode()));
    }

    @Test
    void addMemberForAcspWithUserIdThatAlredyHasActiveMembershipReturnsBadRequest() throws Exception {
      Mockito.doReturn( testDataManager.fetchUserDtos( "COMU002" ) ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU002", false );
      var response =
          mockMvc.perform( post( "/acsps/COMA001/memberships" )
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content( "{\"user_id\":\"COMU002\",\"user_role\":\"standard\"}") )
              .andReturn();


      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(response.getResponse().getContentAsString().contains(ERROR_CODE_1002.getCode()));
    }

    @Test
    void addMemberForAcspWithLoggedStandardUserReturnsBadRequest() throws Exception {
      // Given
      loggedUserId = "COMU007";
      acspNumberFromPath = "COMA001";
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      setTestEnvironment(
          userIdFromRequest,
          acspNumberFromPath,
          loggedUserId,
          new String[] {"COM007"},
          new String[] {},
          false);
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains("Please check the request and try again"));
    }

    @Test
    void addMemberForAcspWithLoggedAdminUserAndNewOwnerUserReturnsBadRequest() throws Exception {
      // Given
      loggedUserId = "COMU005";
      userRoleFromRequest = "owner";
      acspNumberFromPath = "COMA001";
      url = String.format("/acsps/%s/memberships", acspNumberFromPath);
      setTestEnvironment(
          userIdFromRequest,
          acspNumberFromPath,
          loggedUserId,
          new String[] {"COM005"},
          new String[] {},
          false);
      // When
      var response =
          mockMvc
              .perform(
                  post(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", loggedUserId)
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          String.format(
                              "{\"user_id\":\"%s\",\"user_role\":\"%s\"}",
                              userIdFromRequest, userRoleFromRequest)))
              .andReturn();
      // Then
      assertEquals(400, response.getResponse().getStatus());
      assertTrue(
          response
              .getResponse()
              .getContentAsString()
              .contains("Please check the request and try again"));
    }

    @Test
    void addMemberForAcspWithCorrectDataReturnsAddedAcspMembership() throws Exception {
      final var targetUserData = testDataManager.fetchUserDtos( "COMU001" ).getFirst();
      final var targetAcspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();

      Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );

      Mockito.doReturn( targetUserData ).when( usersService ).fetchUserDetails( "COMU001" );
      Mockito.doReturn( targetAcspData ).when( acspDataService ).fetchAcspData( "TSA001" );
      Mockito.doReturn( List.of() ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU001", false );

      Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

      var response =
          mockMvc.perform( post("/acsps/TSA001/memberships")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "TSU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                  .andReturn();

      Mockito.verify( acspMembersService ).addAcspMembership( eq( targetUserData ), eq( targetAcspData ), eq( "TSA001" ), eq( UserRoleEnum.STANDARD ), eq("TSU001") );

      assertEquals(201, response.getResponse().getStatus());
    }
  }
}
