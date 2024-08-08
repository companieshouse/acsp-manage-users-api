package uk.gov.companieshouse.acsp.manage.users.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.sdk.ApiClientService;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
@SpringBootTest
class UserAcspMembershipControllerIntegrationTest {

  @Autowired MongoTemplate mongoTemplate;

  @Autowired public MockMvc mockMvc;

  @MockBean ApiClientService apiClientService;

  @MockBean InternalApiClient internalApiClient;

  @MockBean StaticPropertyUtil staticPropertyUtil;

  @MockBean private UsersService usersService;

  @MockBean private AcspDataService acspDataService;

  @Autowired private AcspMembersRepository acspMembersRepository;

  private final TestDataManager testDataManager = TestDataManager.getInstance();
  private String testUserId;

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

  @BeforeEach
  void setUp() {
    testUserId = "COMU002";
  }

  @AfterEach
  public void after() {
    mongoTemplate.dropCollection(AcspMembersDao.class);
  }

  @Test
  void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/user/acsps/memberships")
                .header("Eric-identity", testUserId)
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
                .header("Eric-identity", testUserId)
                .header("ERIC-Identity-Type", "oauth2")
                .header("ERIC-Authorised-Key-Roles", "*"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getAcspMembershipsForUserIdWhoHasNoAcspMemebershipsReturnsEmptyAcspMembershipsList()
      throws Exception {
    // Given
    final var acspMemberDaos =
        testDataManager.fetchAcspMembersDaos(
            "COM001", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009",
            "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016");

    acspMembersRepository.insert(acspMemberDaos);
    acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "TS002"));
    mockFetchUserDetailsFor(
        "COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008",
        "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
    mockFetchAcspDataFor("COMA001");
    // When
    final var response =
        mockMvc
            .perform(
                get("/user/acsps/memberships")
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", testUserId)
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    final var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    final var acspMembershipsList =
        objectMapper.readValue(response.getContentAsString(), AcspMembershipsList.class);

    final var acspMemberships = acspMembershipsList.getItems();
    // Then
    assertTrue(acspMemberships.isEmpty());
  }

  @Test
  void getAcspMembershipsForUserIdWhoHasAcspMemebershipsReturnsNonEmptyAcspMembershipsList()
      throws Exception {
    // Given
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
    // When
    final var response =
        mockMvc
            .perform(
                get("/user/acsps/memberships")
                    .header("X-Request-Id", "theId123")
                    .header("Eric-identity", testUserId)
                    .header("ERIC-Identity-Type", "oauth2")
                    .header("ERIC-Authorised-Key-Roles", "*"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    final var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    final var acspMembershipsList =
        objectMapper.readValue(response.getContentAsString(), AcspMembershipsList.class);

    final var acspMemberships = acspMembershipsList.getItems();
    // Then
    assertEquals(1, acspMemberships.size());
    assertEquals("COM002", acspMemberships.getFirst().getId());
    assertEquals(testUserId, acspMemberships.getFirst().getUserId());
    assertEquals("COMA001", acspMemberships.getFirst().getAcspNumber());
  }
}
