package uk.gov.companieshouse.acsp.manage.users.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class UserAcspMembershipTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;
    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AcspMembersRepository acspMembersRepository;
    @MockBean
    UsersService usersService;

    @Autowired
    AcspDataRepository acspDataRepository;

    @BeforeEach
    void setup() {

        User user = new User();
        user.setUserId("user1");
        user.setEmail("user1@test.com");
        user.setDisplayName("Test User");
        when(usersService.fetchUserDetails("user1")).thenReturn(user);

        AcspMembersDao acspMembersDao = new AcspMembersDao();
        acspMembersDao.setId("acsp1");
        acspMembersDao.setUserId("user1");
        acspMembersDao.setAcspNumber("ACSP123");
        acspMembersDao.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
        acspMembersDao.setCreatedAt(LocalDateTime.now().minusDays(10));
        acspMembersDao.setAddedAt(LocalDateTime.now().minusDays(10));
        acspMembersDao.setAddedBy("admin1");
        acspMembersDao.setEtag("etag");

        acspMembersRepository.save(acspMembersDao);

        AcspDataDao acspData1 = new AcspDataDao();
        acspData1.setId("ACSP123");
        acspData1.setAcspName("ACSP123");
        acspData1.setAcspStatus("active");


        acspDataRepository.save(acspData1);
    }

    @Test
    void getAcspMembershipForAcspIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members" )
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspIdWithoutRequiredHeadersReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members" ))
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void getAcspMembershipForAcspIdWithNonExistentIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/{id}","neId" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAcspMembershipForExistingMemberIdShouldReturnData() throws Exception {
        final var response = mockMvc.perform( get( "/acsp-members/{id}","acsp1" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect( status().isOk() )
                .andReturn()
                .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final var responseMembership  = objectMapper.readValue(response.getContentAsByteArray(), AcspMembership.class);

        Assertions.assertEquals("acsp1", responseMembership.getId());
        Assertions.assertEquals("user1", responseMembership.getUserId());
        Assertions.assertEquals("ACSP123", responseMembership.getAcspNumber());
        Assertions.assertEquals("admin1", responseMembership.getAddedBy());
    }
    @Test
    void getAcspMembershipForNonExistingMemberIdShouldReturnData() throws Exception {
        final var response = mockMvc.perform(get("/acsp-members/{id}", "acsp2")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(AcspMembersDao.class);
        mongoTemplate.dropCollection(AcspDataDao.class);
    }
}
