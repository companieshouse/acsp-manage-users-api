package uk.gov.companieshouse.acsp.manage.users.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

        mockMvc.perform( get( "/user/acsps/memberships")
                        .header( "Eric-identity", "COMU002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "ERIC-Authorised-Key-Roles", "*" )
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipsForUserIdWithWrongIncludeRemovedParameterInBodyReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
        Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

        mockMvc.perform( get( "/user/acsps/memberships?include_removed=null" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "COMU002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "ERIC-Authorised-Key-Roles", "*" )
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) ) )
                .andExpect( status().isBadRequest() );
    }

}
