package uk.gov.companieshouse.acsp.manage.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAcspMembership.class)
@Tag("unit-test")
class UserAcspMembershipTest {
    @Autowired
    public MockMvc mockMvc;

    @MockBean
    AcspMembershipService acspMembershipService;

    @MockBean
    private UsersService usersService;
    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    private AcspMembership acspMembership1;
    @BeforeEach
    void setUp() {
        acspMembership1 = new AcspMembership();
        acspMembership1.setId("acsp1");
        acspMembership1.setAcspNumber("ACSP123");
        acspMembership1.setUserId("user1");
        acspMembership1.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
        acspMembership1.setAddedAt(OffsetDateTime.now().minusDays(30));
    }
    @Test
    void getAcspMembershipForAcspIdTestShouldThrow400ErrorRequestWhenRequestIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/acsp-members/{id}","acsp1")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2")
                ).andReturn();
        assertEquals(400, response.getResponse().getStatus());
    }

    @Test
    void getAcspMembershipForAcspIdTestShouldThrow404ErrorRequestWhenRequestIdMalformed() throws Exception {
        var response = mockMvc.perform(get("/acsp-members/{id}","acsp1")
                        .header("X-Request-Id", "&&&&")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andReturn();
        assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void getAcspMembershipForExistingMemberIdShouldReturnData() throws Exception {
        Mockito.doReturn(true).when(acspMembershipService).memberIdExists("acsp1");
        Mockito.doReturn(Optional.of(acspMembership1)).when(acspMembershipService).fetchAcspMembership("acsp1");

        final var responseJson = mockMvc.perform(
                get("/acsp-members/{id}","acsp1")
                        .header("X-Request-Id", "theId")
                        .header("ERIC-Identity", "user1")
                        .header("ERIC-Identity-Type", "oauth2")
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final var response  = objectMapper.readValue(responseJson.getContentAsByteArray(), AcspMembership.class);

        Assertions.assertEquals("acsp1", response.getId());
    }

    @Test
    void getAcspMembershipForNonExistingMemberIdShouldReturnData() throws Exception {
        Mockito.doReturn(false).when(acspMembershipService).memberIdExists("acsp2");
        Mockito.doReturn(Optional.of(acspMembership1)).when(acspMembershipService).fetchAcspMembership("acsp2");

        final var responseJson = mockMvc.perform(
                        get("/acsp-members/{id}","acsp2")
                                .header("X-Request-Id", "theId")
                                .header("ERIC-Identity", "user1")
                                .header("ERIC-Identity-Type", "oauth2")
                                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse();

    }

}
