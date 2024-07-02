package uk.gov.companieshouse.acsp.manage.users.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    StaticPropertyUtil staticPropertyUtil;
    private AcspMembersDao acspMember1;

    private AcspMembership acspMembership1;
    @BeforeEach
    void setUp() {
        acspMember1 = new AcspMembersDao();
        acspMember1.setId("acsp1");
        acspMember1.setAcspNumber("ACSP123");
        acspMember1.setUserId("user1");
        acspMember1.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
        acspMember1.setAddedAt(LocalDateTime.now().minusDays(30));
        acspMember1.setRemovedBy("Test1");
        acspMember1.setRemovedAt(LocalDateTime.now().minusDays(10));

        acspMembership1 = new AcspMembership();
        acspMembership1.setId("acspMembership1");
        acspMembership1.setAcspNumber("ACSP123");
        acspMembership1.setUserId("user1");
        acspMembership1.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
        acspMembership1.setAddedAt(OffsetDateTime.now().minusDays(30));
    }
    @Test
    void getAcspMembershipForAcspIdTestShouldThrow401ErrorRequestWhenIdNotProvided() throws Exception {
        var response = mockMvc.perform(get("/acsp-members").header("X-Request-Id", "theId")).andReturn();
        assertEquals(401, response.getResponse().getStatus());
    }

    @Test
    void getAcspMembershipForAcspIdTestShouldThrow404ErrorRequestWhenIdDoesntExist() throws Exception {
        var response = mockMvc.perform(get("/acsp-members")
                .header("X-Request-Id", "theId")
                .header("Id", "apsc123"))
                .andReturn();
        assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void testGetAcspMembershipForIdReturnsData() throws Exception {

        when(acspMembershipService.fetchAcspMembership("acsp1")).thenReturn(Optional.ofNullable(acspMembership1));

        final var response = mockMvc.perform(
                get("/acsp-members")
                        .header("X-Request-Id", "test123")
                        .header("id", "acsp1"))
                .andReturn()
                .getResponse();

        //verify().fetchAcspMemberships();
    }


}
