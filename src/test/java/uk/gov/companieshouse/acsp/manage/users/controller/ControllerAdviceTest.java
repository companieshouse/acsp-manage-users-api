package uk.gov.companieshouse.acsp.manage.users.controller;

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
import uk.gov.companieshouse.acsp.manage.users.configuration.InterceptorConfig;
import uk.gov.companieshouse.acsp.manage.users.configuration.WebSecurityConfig;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag( "unit-test" )
@Import(WebSecurityConfig.class)
@WebMvcTest( AcspMembershipController.class )
class ControllerAdviceTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private AcspMembersService acspMemersService;

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

    @Test
    void testNotFoundRuntimeError() throws Exception {
        Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Couldn't find association" ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        Mockito.doThrow( new BadRequestRuntimeException( "Request was less than ideal" ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockMvc.perform( get("/acsps/memberships/$$$")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void testOnInternalServerError() throws Exception {
        Mockito.doThrow( new NullPointerException( "Something was null, which shouldn't have been." ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001") )
                .andExpect( status().isInternalServerError() );
    }

    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        Mockito.doThrow( new InternalServerErrorRuntimeException( "Problem" ) ).when( acspMemersService ).fetchMembership( any() );

        mockMvc.perform( get("/acsps/memberships/TS001")
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "TSU001") )
                .andExpect( status().isInternalServerError() );
    }

}

