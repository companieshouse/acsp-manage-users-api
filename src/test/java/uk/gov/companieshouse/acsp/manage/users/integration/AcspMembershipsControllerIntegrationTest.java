package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
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
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipsControllerIntegrationTest {

  @Container @ServiceConnection static MongoDBContainer container = new MongoDBContainer("mongo:5");

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
          // Standard role tests
          Arguments.of("standard", false, 2, new String[] {"COMU007", "COMU008"}),
          Arguments.of("standard", true, 3, new String[] {"COMU006", "COMU007", "COMU008"}),

          // Admin role tests
          Arguments.of("admin", false, 2, new String[] {"COMU004", "COMU005"}),
          Arguments.of("admin", true, 3, new String[] {"COMU003", "COMU004", "COMU005"}),

          // Owner role tests
          Arguments.of("owner", false, 1, new String[] {"COMU002"}),
          Arguments.of("owner", true, 3, new String[] {"COMU001", "COMU002", "COMU009"}));
    }
  }

  @AfterEach
  public void after() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
  }
}
