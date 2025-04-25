package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.configuration.WebSecurityConfig;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.ACTIVE;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;

@WebMvcTest(AcspMembershipsController.class)
@Import(WebSecurityConfig.class)
@Tag("unit-test")
class AcspMembershipsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private AcspMembersService acspMembersService;

    @MockBean
    private EmailService emailService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchUserDetailsFor( final String... userIds ) {
        Arrays.stream( userIds ).forEach( userId -> Mockito.doReturn( testDataManager.fetchUserDtos( userId ).getFirst() ).when( usersService ).fetchUserDetails( userId ) );
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup( context )
                .apply( SecurityMockMvcConfigurers.springSecurity() )
                .build();
        ReflectionTestUtils.setField( staticPropertyUtil, "APPLICATION_NAMESPACE", "acsp-manage-users-api" );
    }

    @Nested
    class GetMembersForAcspTests {

        @Test
        void getMembersForAcspReturnsOk() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).fetchMembershipsForAcspNumberAndRole( acspProfile, "owner", false, 0, 15 );

            final var response =
            mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=false&role=owner&page_index=0&items_per_page=15" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                    .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }

        @Test
        void getMembersForAcspWithInvalidRoleThrowsBadRequestException() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).fetchMembershipsForAcspNumberAndRole( acspProfile, "owner", false, 0, 15 );

            mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=false&role=invalid_role&page_index=0&items_per_page=15" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void getMembersForAcspWithoutRoleReturnsOk() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).fetchMembershipsForAcspNumberAndRole( acspProfile, null, true, 0, 20 );

            final var response =
                    mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=true&page_index=0&items_per_page=20" )
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                            .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }
    }

    @Nested
    class FindMembershipsForUserAndAcspTests {

        @Test
        void findMembershipsForUserAndAcspReturnsOk() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();
            final var user = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
            final var usersList = new UsersList();
            usersList.add( user );

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );
            Mockito.doReturn( usersList ).when( usersService ).searchUserDetails( List.of( "buzz.lightyear@toystory.com" ) );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile( "COMA001" );
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).fetchMemberships( user, false, "COMA001" );

            final var response =
            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                    .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_email\":\"buzz.lightyear@toystory.com\"}" ) )
                    .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }

        @Test
        void findMembershipsForUserAndAcspWithoutUserEmailThrowsBadRequestException() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );

            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void findMembershipsForUserAndAcspWithNonexistentUserEmailThrowsNotFoundException() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();

            mockFetchUserDetailsFor( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );
            Mockito.doReturn( new UsersList() ).when( usersService ).searchUserDetails( List.of( "buzz.lightyear@toystory.com" ) );

            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_email\":\"buzz.lightyear@toystory.com\"}" ) )
                    .andExpect( status().isNotFound() );
        }
    }

    @Nested
    class AddMemberForAcsp {

        @Test
        void addMemberForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header("Eric-identity", "COMU002" )
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}") )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithMalformedUserIdReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"abc-111-&\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

            mockMvc.perform( post( "/acsps/TSA001-&/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithoutUserRoleReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentUserRoleReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();
            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"superuser\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentAcspNumberReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );
            Mockito.doThrow( new NotFoundRuntimeException( "", new Exception( "" ) ) ).when(acspProfileService).fetchAcspProfile( "TSA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentUserIdReturnsBadRequest() throws Exception {
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );
            Mockito.doThrow( new NotFoundRuntimeException( "", new Exception( "" ) ) ).when( usersService ).retrieveUserDetails( "COMU001", null );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithUserIdThatAlreadyHasActiveMembershipReturnsBadRequest() throws Exception {
            final var requestingUser = testDataManager.fetchUserDtos( "COMU002" );
            final var requestingUserDao = testDataManager.fetchAcspMembersDaos( "COM002" ).getFirst();

            mockFetchUserDetailsFor( "COMU002" );
            Mockito.doReturn( Optional.of( requestingUserDao ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU002", "COMA001" );
            Mockito.doReturn( requestingUser ).when( acspMembersService ).fetchMembershipDaos( "COMU002", null, false );

            mockMvc.perform( post( "/acsps/COMA001/memberships" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM002" ) )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU002\",\"user_role\":\"standard\"}") )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithLoggedStandardUserReturnsForbidden() throws Exception {
            final var users = testDataManager.fetchUserDtos( "COMU007", "COMU001" );
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();
            final var membership = testDataManager.fetchAcspMembersDaos( "COM007" ).getFirst();

            Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "COMU007" );
            Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile( "COMA001" );
            Mockito.doReturn( Optional.of( membership ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU007", "COMA001" );

            mockMvc.perform( post( "/acsps/COMA001/memberships" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU007")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM007" ) )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isForbidden() );
        }

        @Test
        void addMemberForAcspWithLoggedAdminUserAndNewOwnerUserReturnsForbidden() throws Exception {
            final var users = testDataManager.fetchUserDtos( "COMU005", "COMU001" );
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();
            final var membership = testDataManager.fetchAcspMembersDaos( "COM005" ).getFirst();

            Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "COMU005" );
            Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile( "COMA001" );
            Mockito.doReturn( Optional.of( membership ) ).when( acspMembersService ).fetchActiveAcspMembership( "COMU005", "COMA001" );

            mockMvc.perform( post( "/acsps/COMA001/memberships" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU005")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM005" ) )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"owner\"}" ) )
                    .andExpect( status().isForbidden() );
        }

        @Test
        void addMemberForAcspWithCorrectDataReturnsAddedAcspMembership() throws Exception {
            final var targetUserData = testDataManager.fetchUserDtos( "COMU001" ).getFirst();
            final var targetAcspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

            Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
            Mockito.doReturn( targetUserData ).when( usersService ).retrieveUserDetails( "COMU001", null );
            Mockito.doReturn( targetAcspProfile ).when(acspProfileService).fetchAcspProfile( "TSA001" );
            Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( "COMU001", null, false );
            Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
            Mockito.doReturn( Mono.empty() ).when( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", STANDARD );

            mockMvc.perform( post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "TSU001")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isCreated() );

            Mockito.verify( acspMembersService ).createMembership( targetUserData, targetAcspProfile, STANDARD,"TSU001" );
        }

    }

    static Stream<Arguments> rolesStream(){
        return Stream.of(
                Arguments.of( UserRoleEnum.OWNER ),
                Arguments.of( UserRoleEnum.ADMIN ),
                Arguments.of( STANDARD )
        );
    }

    @ParameterizedTest
    @MethodSource( "rolesStream" )
    void addMemberForAcspSendsConfirmYouAreAMemberNotificationsWithoutDisplayName( final UserRoleEnum role ) throws Exception {
        final var users = testDataManager.fetchUserDtos( "TSU001", "COMU001" );
        final var acsp = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( users.getLast() ).when( usersService ).retrieveUserDetails( "COMU001", null );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "TSA001" );
        Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( "COMU001", null, false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", role );

        mockMvc.perform( post("/acsps/TSA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue() ) ) )
                .andExpect( status().isCreated() );

        if ( UserRoleEnum.OWNER.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail("jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", UserRoleEnum.OWNER );
        } else if ( UserRoleEnum.ADMIN.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", UserRoleEnum.ADMIN );
        } else if ( STANDARD.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", STANDARD );
        }

    }

    @ParameterizedTest
    @MethodSource( "rolesStream" )
    void addMemberForAcspSendsConfirmYouAreAMemberNotificationsWithDisplayName( final UserRoleEnum role ) throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU001", "COMU001" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU001" );
        Mockito.doReturn( users.getLast() ).when( usersService ).retrieveUserDetails( "COMU001", null );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( "COMU001", null, false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU001", "WITA001" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendConfirmYouAreAMemberEmail("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", role );

        mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT001" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue() ) ) )
                .andExpect( status().isCreated() );

        if ( UserRoleEnum.OWNER.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", UserRoleEnum.OWNER );
        } else if ( UserRoleEnum.ADMIN.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", UserRoleEnum.ADMIN );
        } else if ( STANDARD.equals( role ) ) {
            Mockito.verify( emailService ).sendConfirmYouAreAMemberEmail( "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", STANDARD );
        }

    }

    @ParameterizedTest
    @MethodSource( "rolesStream" )
    void addMemberForAcspDoesNotSendConfirmYouAreAMemberNotificationsWhenCalledInternally( final UserRoleEnum role ) throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU001", "COMU001" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU001" );
        Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( "COMU001", null, false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU001", "WITA001" );

        mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT001" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue() ) ) )
                .andExpect( status().isCreated() );

        if ( UserRoleEnum.OWNER.equals( role ) ) {
            Mockito.verify( emailService, times( 0 ) ).sendConfirmYouAreAMemberEmail(  "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", UserRoleEnum.OWNER );
        } else if ( UserRoleEnum.ADMIN.equals( role ) ) {
            Mockito.verify( emailService, times( 0 ) ).sendConfirmYouAreAMemberEmail(  "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", UserRoleEnum.ADMIN );
        } else if ( STANDARD.equals( role ) ) {
            Mockito.verify( emailService, times( 0 ) ).sendConfirmYouAreAMemberEmail(  "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", STANDARD );
        }
    }

    @Test
    void addMemberForAcspWithMalformedUserEmailReturnsBadRequest() throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU005" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT006" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU005" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU005", "WITA001" );

        mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT006" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_email\":\"$$$\",\"user_role\":\"%s\"}", ADMIN.getValue() ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addMemberForAcspWithNullUserIdAndUserEmailReturnsBadRequest() throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU005" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT006" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU005" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU005", "WITA001" );

        mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT006" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_role\":\"%s\"}", ADMIN.getValue() ) ) )
                .andExpect( status().isBadRequest() );

    }

    @Test
    void addMemberForAcspSendsInvitationWhenCHSAccountDoesNotExist() throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU005" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT006" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU005" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( null, "dijkstra.witcher@inugami-example.com", false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU005", "WITA001" );
        Mockito.doReturn( testDataManager.fetchAcspMembershipDtos( "WIT005" ).getFirst() ).when( acspMembersService ).createInvitation( "dijkstra.witcher@inugami-example.com", acsp, ADMIN, "WITU005" );

        final var response = mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT006" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_email\":\"dijkstra.witcher@inugami-example.com\",\"user_role\":\"%s\"}", ADMIN.getValue() ) ) )
                .andExpect( status().isCreated() );

        final var membership = parseResponseTo( response, AcspMembership.class );

        Assertions.assertNotNull( membership.getEtag() );
        Assertions.assertEquals( "WIT005", membership.getId() );
        Assertions.assertNull( membership.getUserId() );
        Assertions.assertEquals( "Not Provided", membership.getUserDisplayName() );
        Assertions.assertEquals( "dijkstra.witcher@inugami-example.com", membership.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, membership.getUserRole() );
        Assertions.assertEquals( "WITA001", membership.getAcspNumber() );
        Assertions.assertEquals( "Witcher", membership.getAcspName() );
        Assertions.assertEquals( AcspStatusEnum.ACTIVE, membership.getAcspStatus() );
        Assertions.assertEquals( MembershipStatusEnum.PENDING, membership.getMembershipStatus() );
        Assertions.assertNull( membership.getAddedAt() );
        Assertions.assertNotNull( membership.getInvitedAt() );
        Assertions.assertNull( membership.getAcceptedAt() );
        Assertions.assertEquals( "WITU001", membership.getAddedBy() );
        Assertions.assertNull( membership.getRemovedBy() );
        Assertions.assertNull( membership.getRemovedAt() );
        Assertions.assertEquals( "acsp-membership", membership.getKind() );
    }

    @Test
    void addMemberForAcspCreateActiveMembershipWhenCHSAccountExists() throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU005", "WITU006" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT006" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU005" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( List.of() ).when( acspMembersService ).fetchMembershipDaos( null, "margarita.witcher@inugami-example.com", false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU005", "WITA001" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "WITU006" ).getFirst() ).when( usersService ).retrieveUserDetails( null, "margarita.witcher@inugami-example.com" );
        Mockito.doReturn( new AcspMembership().id( "WIT007" ).membershipStatus( ACTIVE ) ).when( acspMembersService ).createMembership( users.getLast(), acsp, ADMIN, "WITU005" );
        Mockito.doReturn( Mono.empty() ).when( emailService ).sendConfirmYouAreAMemberEmail( "margarita.witcher@inugami-example.com", "letho.witcher@inugami-example.com", "Witcher", ADMIN );

        final var response = mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT006" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_email\":\"margarita.witcher@inugami-example.com\",\"user_role\":\"%s\"}", ADMIN.getValue() ) ) )
                .andExpect( status().isCreated() );
        final var membership = parseResponseTo( response, AcspMembership.class );

        Assertions.assertEquals( "WIT007", membership.getId() );
        Assertions.assertEquals( MembershipStatusEnum.ACTIVE, membership.getMembershipStatus() );
    }

    @Test
    void addMemberForAcspReturnsBadRequestWhenInvitationAlreadyPending() throws Exception {
        final var users = testDataManager.fetchUserDtos( "WITU005" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT006" ).getFirst();

        Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU005" );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( testDataManager.fetchAcspMembersDaos( "WIT005" ) ).when( acspMembersService ).fetchMembershipDaos( null, "dijkstra.witcher@inugami-example.com", false );
        Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU005", "WITA001" );

        mockMvc.perform( post("/acsps/WITA001/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT006" ) )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content( String.format( "{\"user_email\":\"dijkstra.witcher@inugami-example.com\",\"user_role\":\"%s\"}", ADMIN.getValue() ) ) )
                .andExpect( status().isBadRequest() );

    }

}
