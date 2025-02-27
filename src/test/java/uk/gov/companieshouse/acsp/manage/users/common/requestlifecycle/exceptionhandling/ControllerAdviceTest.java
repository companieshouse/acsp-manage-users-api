package uk.gov.companieshouse.acsp.manage.users.common.requestlifecycle.exceptionhandling;

import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.acsp.manage.users.ascpprofile.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.common.requestlifecycle.interceptor.InterceptorConfig;
import uk.gov.companieshouse.acsp.manage.users.membership.MembershipService;
import uk.gov.companieshouse.acsp.manage.users.testresources.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.common.requestlifecycle.filterchain.WebSecurityConfig;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.membership.controller.AcspMembershipExternalController;
import uk.gov.companieshouse.acsp.manage.users.email.EmailService;
import uk.gov.companieshouse.acsp.manage.users.common.utils.StaticPropertyUtil;
import uk.gov.companieshouse.acsp.manage.users.user.UsersService;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag( "unit-test" )
@Import(WebSecurityConfig.class)
@WebMvcTest( AcspMembershipExternalController.class )
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private MembershipService acspMemersService;

    @MockBean
    private InterceptorConfig interceptorConfig;

    @MockBean
    private UsersService usersService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup( context )
                .apply( SecurityMockMvcConfigurers.springSecurity() )
                .build();
        Mockito.doNothing().when( interceptorConfig ).addInterceptors( any() );
    }
    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchUserDetailsFor( final String... userIds ) {
        Arrays.stream( userIds ).forEach( userId -> Mockito.doReturn( testDataManager.fetchUserDtos( userId ).getFirst() ).when( usersService ).fetchUserDetails( userId ) );
    }

    @Test
    void testNotFoundRuntimeError() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doThrow( new NotFoundRuntimeException( "Couldn't find association", new Exception( "Couldn't find association" ) ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doThrow( new BadRequestRuntimeException( "Request was less than ideal", new Exception( "Request was less than ideal" ) ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

        mockMvc.perform( get("/acsps/memberships/$$$")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void testOnInternalServerError() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doThrow( new NullPointerException( "Something was null, which shouldn't have been." ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isInternalServerError() );
    }

    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doThrow( new InternalServerErrorRuntimeException( "Problem", new Exception( "Problem" ) ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isInternalServerError() );
    }

    @Test
    void testForbiddenRuntimeError() throws Exception {
        mockFetchUserDetailsFor( "TSU001" );
        Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMemersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doThrow( new ForbiddenRuntimeException( "Request was less than ideal", new Exception( "Request was less than ideal" ) ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isForbidden() );
    }

}

