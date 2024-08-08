package uk.gov.companieshouse.acsp.manage.users.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.sdk.ApiClientService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipsControllerIntegrationTest {

  @Container
  @ServiceConnection
  static MongoDBContainer container = new MongoDBContainer("mongo:5");

  @Autowired MongoTemplate mongoTemplate;

  @Autowired public MockMvc mockMvc;

  @MockBean ApiClientService apiClientService;

  @MockBean InternalApiClient internalApiClient;

  @MockBean StaticPropertyUtil staticPropertyUtil;

  private final TestDataManager testDataManager = TestDataManager.getInstance();

  @MockBean private UsersService usersService;

  @MockBean private AcspDataService acspDataService;

  @Autowired private AcspMembersRepository acspMembersRepository;

  private void mockFetchUserDetailsFor(String... userIds) {
    Arrays.stream(userIds)
        .forEach(
            userId ->
                Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst())
                    .when(usersService)
                    .fetchUserDetails(userId));
  }

  private void mockFetchAcspDataFor(String... acspNumbers) {
    Arrays.stream(acspNumbers)
        .forEach(
            acspNumber ->
                Mockito.doReturn(testDataManager.fetchAcspDataDaos(acspNumber).getFirst())
                    .when(acspDataService)
                    .fetchAcspData(acspNumber));
  }

  @Nested
  class GetMembersForAcsp {

    @Test
    void getMembersForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              get("/acsps/COMA001/memberships")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getMembersForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              get("/acsps/££££££/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getMembersForAcspWithNonExistentAcspNumberReturnsNotFound() throws Exception {
      Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Was not found"))
          .when(acspDataService)
          .fetchAcspData("919191");

      mockMvc
          .perform(
              get("/acsps/919191/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*"))
          .andExpect(status().isNotFound());
    }

    @Test
    void getMembersForAcspAppliesAcspNumberAndIncludeRemovedAndRoleFilterCorrectly()
        throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016");

      acspMembersRepository.insert(acspMemberDaos);
      acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "TS002"));

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
      mockFetchAcspDataFor("COMA001");

      final var response =
          mockMvc
              .perform(
                  get("/acsps/COMA001/memberships?include_removed=false&role=owner")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();

      final var objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      final var acspMembers =
          objectMapper.readValue(response.getContentAsByteArray(), AcspMembershipsList.class);

      final var links = acspMembers.getLinks();

      final var acspIds =
          acspMembers.getItems().stream().map(AcspMembership::getId).collect(Collectors.toSet());

      Assertions.assertEquals(0, acspMembers.getPageNumber());
      Assertions.assertEquals(15, acspMembers.getItemsPerPage());
      Assertions.assertEquals(2, acspMembers.getTotalResults());
      Assertions.assertEquals(1, acspMembers.getTotalPages());
      Assertions.assertEquals(
          "/acsps/COMA001/memberships?page_index=0&items_per_page=15", links.getSelf());
      Assertions.assertEquals("", links.getNext());
      assertTrue(acspIds.containsAll(Set.of("COM002", "COM010")));
    }

    @Test
    void getMembersForAcspAppliesIncludeRemovedFilterCorrectly() throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016");

      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
      mockFetchAcspDataFor("COMA001");

      final var response =
          mockMvc
              .perform(
                  get("/acsps/COMA001/memberships?include_removed=true&items_per_page=20")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();

      final var objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      final var acspMembers =
          objectMapper.readValue(response.getContentAsByteArray(), AcspMembershipsList.class);

      final var links = acspMembers.getLinks();

      final var acspIds =
          acspMembers.getItems().stream().map(AcspMembership::getId).collect(Collectors.toSet());

      Assertions.assertEquals(0, acspMembers.getPageNumber());
      Assertions.assertEquals(20, acspMembers.getItemsPerPage());
      Assertions.assertEquals(16, acspMembers.getTotalResults());
      Assertions.assertEquals(1, acspMembers.getTotalPages());
      Assertions.assertEquals(
          "/acsps/COMA001/memberships?page_index=0&items_per_page=20", links.getSelf());
      Assertions.assertEquals("", links.getNext());
      assertTrue(
          acspIds.containsAll(
              Set.of(
                  "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
                  "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016")));
    }

    @ParameterizedTest
    @MethodSource("provideRoleAndIncludeRemovedTestData")
    void getMembersForAcspWithRoleAndIncludeRemovedFilterAppliesCorrectly(
        String role, boolean includeRemoved, int expectedCount, String[] expectedUserIds)
        throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009");
      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009");
      mockFetchAcspDataFor("COMA001");

      String url =
          String.format(
              "/acsps/COMA001/memberships?role=%s&include_removed=%s", role, includeRemoved);

      MvcResult mvcResult =
          mockMvc
              .perform(
                  get(url)
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*"))
              .andExpect(status().isOk())
              .andReturn();

      String responseBody = mvcResult.getResponse().getContentAsString();

      assertTrue(responseBody.contains("\"total_results\":" + expectedCount));
      for (String userId : expectedUserIds) {
        assertTrue(responseBody.contains("\"user_id\":\"" + userId + "\""));
      }
      assertTrue(responseBody.contains("\"user_role\":\"" + role + "\""));
    }

    private static Stream<Arguments> provideRoleAndIncludeRemovedTestData() {
      return Stream.of(
          Arguments.of("standard", false, 2, new String[] {"COMU007", "COMU008"}),
          Arguments.of("standard", true, 3, new String[] {"COMU006", "COMU007", "COMU008"}),
          Arguments.of("admin", false, 2, new String[] {"COMU004", "COMU005"}),
          Arguments.of("admin", true, 3, new String[] {"COMU003", "COMU004", "COMU005"}),
          Arguments.of("owner", false, 1, new String[] {"COMU002"}),
          Arguments.of("owner", true, 3, new String[] {"COMU001", "COMU002", "COMU009"}));
    }
  }

  @Nested
  class FindMembershipsForUserAndAcsp {

    @Test
    void findMembershipsForUserAndAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail("shaun.lock@comedy.com");

      mockMvc
          .perform(
              post("/acsps/COMA001/memberships/lookup")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(new ObjectMapper().writeValueAsString(requestBody)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void findMembershipsForUserAndAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail("shaun.lock@comedy.com");

      mockMvc
          .perform(
              post("/acsps/££££££/memberships/lookup")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(new ObjectMapper().writeValueAsString(requestBody)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void findMembershipsForUserAndAcspWithNonExistentAcspNumberReturnsNotFound() throws Exception {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail("shaun.lock@comedy.com");

      Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Was not found"))
          .when(acspDataService)
          .fetchAcspData("NONEXISTENT");

      mockMvc
          .perform(
              post("/acsps/NONEXISTENT/memberships/lookup")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(new ObjectMapper().writeValueAsString(requestBody)))
          .andExpect(status().isNotFound());
    }

    @Test
    void findMembershipsForUserAndAcspWithNullUserEmailReturnsBadRequest() throws Exception {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(null);

      mockMvc
          .perform(
              post("/acsps/COMA001/memberships/lookup")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(new ObjectMapper().writeValueAsString(requestBody)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void findMembershipsForUserAndAcspWithNonExistentUserReturnsNotFound() throws Exception {
      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail("nonexistent@example.com");

      Mockito.when(usersService.searchUserDetails(List.of("nonexistent@example.com")))
          .thenReturn(new UsersList());

      mockMvc
          .perform(
              post("/acsps/COMA001/memberships/lookup")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(new ObjectMapper().writeValueAsString(requestBody)))
          .andExpect(status().isNotFound());
    }

    @Test
    void findMembershipsForUserAndAcspReturnsCorrectData() throws Exception {
      final var userDto = testDataManager.fetchUserDtos("COMU002").getFirst();
      final var acspDataDao = testDataManager.fetchAcspDataDaos("COMA001").getFirst();
      final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM002");

      UsersList usersList = new UsersList();
      usersList.add(userDto);
      Mockito.when(usersService.searchUserDetails(List.of(userDto.getEmail())))
          .thenReturn(usersList);
      Mockito.when(acspDataService.fetchAcspData("COMA001")).thenReturn(acspDataDao);

      acspMembersRepository.insert(acspMemberDaos);

      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(userDto.getEmail());

      MvcResult result =
          mockMvc
              .perform(
                  post("/acsps/COMA001/memberships/lookup")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(new ObjectMapper().writeValueAsString(requestBody)))
              .andExpect(status().isOk())
              .andReturn();

      ObjectMapper objectMapper =
          new ObjectMapper()
              .registerModule(new JavaTimeModule())
              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

      String content = result.getResponse().getContentAsString();
      AcspMembershipsList responseList = objectMapper.readValue(content, AcspMembershipsList.class);
      assertEquals(1, responseList.getItems().size());
      assertEquals("COMU002", responseList.getItems().get(0).getUserId());
      assertEquals(AcspMembership.UserRoleEnum.OWNER, responseList.getItems().get(0).getUserRole());
    }

    @Test
    void findMembershipsForActiveUser() throws Exception {
      final var activeUserDto = testDataManager.fetchUserDtos("COMU002").getFirst();
      final var acspDataDao = testDataManager.fetchAcspDataDaos("COMA001").getFirst();
      final var activeMembers = testDataManager.fetchAcspMembersDaos("COM002", "COM004", "COM005");
      final var removedMembers = testDataManager.fetchAcspMembersDaos("COM001", "COM003");

      UsersList usersList = new UsersList();
      usersList.add(activeUserDto);
      Mockito.when(usersService.searchUserDetails(List.of(activeUserDto.getEmail())))
          .thenReturn(usersList);
      Mockito.when(acspDataService.fetchAcspData("COMA001")).thenReturn(acspDataDao);

      acspMembersRepository.insert(activeMembers);
      acspMembersRepository.insert(removedMembers);

      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(activeUserDto.getEmail());

      MvcResult result =
          mockMvc
              .perform(
                  post("/acsps/COMA001/memberships/lookup?include_removed=true")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU002")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(new ObjectMapper().writeValueAsString(requestBody)))
              .andExpect(status().isOk())
              .andReturn();

      ObjectMapper objectMapper =
          new ObjectMapper()
              .registerModule(new JavaTimeModule())
              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      String content = result.getResponse().getContentAsString();
      AcspMembershipsList responseList = objectMapper.readValue(content, AcspMembershipsList.class);
      assertEquals(1, responseList.getItems().size());
      assertEquals("COM002", responseList.getItems().get(0).getId());
    }

    @Test
    void findMembershipsForRemovedUser() throws Exception {
      final var removedUserDto = testDataManager.fetchUserDtos("COMU001").getFirst();
      final var acspDataDao = testDataManager.fetchAcspDataDaos("COMA001").getFirst();
      final var activeMembers = testDataManager.fetchAcspMembersDaos("COM002", "COM004", "COM005");
      final var removedMembers = testDataManager.fetchAcspMembersDaos("COM001", "COM003");

      UsersList usersList = new UsersList();
      usersList.add(removedUserDto);
      Mockito.when(usersService.searchUserDetails(List.of(removedUserDto.getEmail())))
          .thenReturn(usersList);
      Mockito.when(acspDataService.fetchAcspData("COMA001")).thenReturn(acspDataDao);

      acspMembersRepository.insert(activeMembers);
      acspMembersRepository.insert(removedMembers);

      RequestBodyLookup requestBody = new RequestBodyLookup();
      requestBody.setUserEmail(removedUserDto.getEmail());

      MvcResult resultWithoutRemoved =
          mockMvc
              .perform(
                  post("/acsps/COMA001/memberships/lookup?include_removed=false")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(new ObjectMapper().writeValueAsString(requestBody)))
              .andExpect(status().isOk())
              .andReturn();

      MvcResult resultWithRemoved =
          mockMvc
              .perform(
                  post("/acsps/COMA001/memberships/lookup?include_removed=true")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "COMU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(new ObjectMapper().writeValueAsString(requestBody)))
              .andExpect(status().isOk())
              .andReturn();

      ObjectMapper objectMapper =
          new ObjectMapper()
              .registerModule(new JavaTimeModule())
              .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

      String contentWithoutRemoved = resultWithoutRemoved.getResponse().getContentAsString();
      AcspMembershipsList responseListWithoutRemoved =
          objectMapper.readValue(contentWithoutRemoved, AcspMembershipsList.class);
      assertEquals(0, responseListWithoutRemoved.getItems().size());

      String contentWithRemoved = resultWithRemoved.getResponse().getContentAsString();
      AcspMembershipsList responseListWithRemoved =
          objectMapper.readValue(contentWithRemoved, AcspMembershipsList.class);
      assertEquals(1, responseListWithRemoved.getItems().size());
      assertEquals("COM001", responseListWithRemoved.getItems().get(0).getId());
    }
  }

  @Nested
  class AddMemberForAcsp {
    @Test
    void addMemberForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/acsps/TSA001/memberships")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"abc-111-&,TSA001", "COMU001,TSA001-&"})
    void addMemberForAcspWithMalformedUserIdInBodyOrMalformedAcspNumberInUrlReturnsBadRequest(
        String id, String acspNumber) throws Exception {
      mockMvc
          .perform(
              post(String.format("/acsps/%s/memberships", acspNumber))
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(String.format("{\"user_id\":\"%s\",\"user_role\":\"standard\"}", id)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberForAcspWithoutUserIdInBodyReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/acsps/TSA001/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format("{\"user_id\":%s,\"user_role\":\"%s\"}", null, "standard")))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberForAcspWithoutUserRoleReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/acsps/TSA001/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(String.format("{\"user_id\":%s,\"user_role\":\"%s\"}", "COMU001", null)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberForAcspWithNonexistentUserRoleReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/acsps/TSA001/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format(
                          "{\"user_id\":%s,\"user_role\":\"%s\"}", "COMU001", "superuser")))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberForAcspWithNonexistentAcspNumberReturnsBadRequest() throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009");
      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009");
      mockFetchAcspDataFor("COMA001");
      when(acspDataService.fetchAcspData("NONEXISTENT"))
          .thenThrow(new NotFoundRuntimeException("", ""));
      mockMvc
          .perform(
              post("/acsps/NONEXISTENT/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberForAcspWithNonexistentUserIdReturnsBadRequest() throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009");
      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009");
      mockFetchAcspDataFor("COMA001");
      when(usersService.fetchUserDetails("NONEXISTENT"))
          .thenThrow(new NotFoundRuntimeException("", ""));
      mockMvc
          .perform(
              post("/acsps/TSA001/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", "COMU002")
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"user_id\":\"NONEXISTENT\",\"user_role\":\"standard\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(
              content()
                  .json("{\"errors\":[{\"error\":\"ERROR CODE: 1001\",\"type\":\"ch:service\"}]}"));
    }

    @ParameterizedTest
    @CsvSource({
      "COMU002,COMU007,standard,ERROR CODE: 1002",
      "COMU007,COMU001,standard,Please check the request and try again",
      "COMU005,COMU001,owner,Please check the request and try again"
    })
    void
        addMemberForAcspWithUserIdThatAlredyHasActiveMembershipOrLoggedStandardUserOrLoggedAdminUserAndNewOwnerUserReturnsBadRequest(
            String loggedUserId, String userId, String userRole, String errorMessage)
            throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009", "NEI003");
      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009", "NEIU003");
      mockFetchAcspDataFor("COMA001");
      mockMvc
          .perform(
              post("/acsps/COMA001/memberships")
                  .header("X-Request-Id", "theId123")
                  .header("Eric-identity", loggedUserId)
                  .header("ERIC-Identity-Type", "oauth2")
                  .header("ERIC-Authorised-Key-Roles", "*")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      String.format("{\"user_id\":\"%s\",\"user_role\":\"%s\"}", userId, userRole)))
          .andExpect(status().isBadRequest())
          .andExpect(
              content()
                  .json(
                      String.format(
                          "{\"errors\":[{\"error\":\"%s\",\"type\":\"ch:service\"}]}",
                          errorMessage)));
    }

    @Test
    void addMemberForAcspWithCorrectDataReturnsAddedAcspMembership() throws Exception {
      final var acspMemberDaos =
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008",
              "COM009", "TS001");
      acspMembersRepository.insert(acspMemberDaos);

      mockFetchUserDetailsFor(
          "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
          "COMU009", "TSU001");
      mockFetchAcspDataFor("TSA001");
      final var result =
          mockMvc
              .perform(
                  post("/acsps/TSA001/memberships")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "TSU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
              .andReturn();
      assertEquals(201, result.getResponse().getStatus());
      assertTrue(result.getResponse().getContentAsString().contains("\"acsp_number\":\"TSA001\""));
      assertTrue(result.getResponse().getContentAsString().contains("\"user_id\":\"COMU001\""));
      assertTrue(result.getResponse().getContentAsString().contains("\"added_by\":\"TSU001\""));
    }
  }

  @AfterEach
  public void after() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
  }
}
