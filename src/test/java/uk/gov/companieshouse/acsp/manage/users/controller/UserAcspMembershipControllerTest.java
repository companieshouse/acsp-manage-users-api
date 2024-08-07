package uk.gov.companieshouse.acsp.manage.users.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAcspMembershipController.class)
@Tag("unit-test")
class UserAcspMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspMembersService acspMembersService;

    @Test
    void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/user/acsps/memberships")
                        .header( "Eric-identity", "COMU002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "ERIC-Authorised-Key-Roles", "*" )
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipsForUserIdWithWrongIncludeRemovedParameterInBodyReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/user/acsps/memberships?include_removed=null" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "COMU002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "ERIC-Authorised-Key-Roles", "*" )
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ) )
                .andExpect( status().isBadRequest() );
    }

}
