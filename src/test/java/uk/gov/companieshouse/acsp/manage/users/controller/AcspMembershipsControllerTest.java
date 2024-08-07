package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.parseResponseTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.ERROR_CODE_1001;
import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.ERROR_CODE_1002;

import java.util.Collections;
import java.util.List;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.EmailService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

@WebMvcTest(AcspMembershipsController.class)
@Tag("unit-test")
class AcspMembershipsControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Nested
    class GetMembersForAcspTests {

        @Test
        void getMembersForAcspReturnsOk() throws Exception {
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).findAllByAcspNumberAndRole( "COMA001", acspProfile, "owner", false, 0, 15 );

            final var response =
            mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=false&role=owner&page_index=0&items_per_page=15" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ) )
                    .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }

        @Test
        void getMembersForAcspWithInvalidRoleThrowsBadRequestException() throws Exception {
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).findAllByAcspNumberAndRole( "COMA001", acspProfile, "owner", false, 0, 15 );

            mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=false&role=invalid_role&page_index=0&items_per_page=15" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void getMembersForAcspWithoutRoleReturnsOk() throws Exception {
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile("COMA001");
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).findAllByAcspNumberAndRole( "COMA001", acspProfile, null, true, 0, 20 );

            final var response =
                    mockMvc.perform( get( "/acsps/COMA001/memberships?include_removed=true&page_index=0&items_per_page=20" )
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ) )
                            .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }
    }

    @Nested
    class FindMembershipsForUserAndAcspTests {

        @Test
        void findMembershipsForUserAndAcspReturnsOk() throws Exception {
            final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();
            final var user = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
            final var usersList = new UsersList();
            usersList.add( user );

            Mockito.doReturn( usersList ).when( usersService ).searchUserDetails( List.of( "buzz.lightyear@toystory.com" ) );
            Mockito.doReturn( acspProfile ).when(acspProfileService).fetchAcspProfile( "COMA001" );
            Mockito.doReturn( new AcspMembershipsList() ).when( acspMembersService ).fetchAcspMemberships( user, false, "COMA001" );

            final var response =
            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                    .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_email\":\"buzz.lightyear@toystory.com\"}" ) )
                    .andExpect( status().isOk() );

            final var acspMembershipsList = parseResponseTo( response, AcspMembershipsList.class );

            Assertions.assertEquals( new AcspMembershipsList(), acspMembershipsList );
        }

        @Test
        void findMembershipsForUserAndAcspWithoutUserEmailThrowsBadRequestException() throws Exception {
            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void findMembershipsForUserAndAcspWithNonexistentUserEmailThrowsNotFoundException() throws Exception {
            Mockito.doReturn( new UsersList() ).when( usersService ).searchUserDetails( List.of( "buzz.lightyear@toystory.com" ) );

            mockMvc.perform( post( "/acsps/COMA001/memberships/lookup?include_removed=false" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_email\":\"buzz.lightyear@toystory.com\"}" ) )
                    .andExpect( status().isNotFound() );
        }
    }

    @Nested
    class AddMemberForAcsp {

        @Test
        void addMemberForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header("Eric-identity", "COMU002" )
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}") )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithMalformedUserIdReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"abc-111-&\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001-&/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithoutUserIdInBodyReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithoutUserRoleReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentUserRoleReturnsBadRequest() throws Exception {
            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"superuser\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentAcspNumberReturnsBadRequest() throws Exception {
            Mockito.doThrow( new NotFoundRuntimeException( "", "" ) ).when(acspProfileService).fetchAcspProfile( "TSA001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithNonexistentUserIdReturnsBadRequest() throws Exception {
            Mockito.doThrow( new NotFoundRuntimeException( "", "" ) ).when( usersService ).fetchUserDetails( "COMU001" );

            mockMvc.perform( post( "/acsps/TSA001/memberships" )
                            .header( "X-Request-Id", "theId123" )
                            .header( "Eric-identity", "COMU002" )
                            .header( "ERIC-Identity-Type", "oauth2" )
                            .header( "ERIC-Authorised-Key-Roles", "*" )
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType( MediaType.APPLICATION_JSON )
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithUserIdThatAlreadyHasActiveMembershipReturnsBadRequest() throws Exception {
            final var acspMembershipDaos = testDataManager.fetchUserDtos( "COMU002" );

            Mockito.doReturn( acspMembershipDaos ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU002", false );

            mockMvc.perform( post( "/acsps/COMA001/memberships" )
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU002\",\"user_role\":\"standard\"}") )
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithLoggedStandardUserReturnsBadRequest() throws Exception {
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
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithLoggedAdminUserAndNewOwnerUserReturnsBadRequest() throws Exception {
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
                    .andExpect( status().isBadRequest() );
        }

        @Test
        void addMemberForAcspWithCorrectDataReturnsAddedAcspMembership() throws Exception {
            final var targetUserData = testDataManager.fetchUserDtos( "COMU001" ).getFirst();
            final var targetAcspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

            Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
            Mockito.doReturn( targetUserData ).when( usersService ).fetchUserDetails( "COMU001" );
            Mockito.doReturn( targetAcspProfile ).when(acspProfileService).fetchAcspProfile( "TSA001" );
            Mockito.doReturn( List.of() ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU001", false );
            Mockito.doReturn( Optional.of( testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

            mockMvc.perform( post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "TSU001")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content( "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}" ) )
                    .andExpect( status().isCreated() );

            Mockito.verify( acspMembersService ).addAcspMembership( targetUserData, targetAcspProfile, "TSA001", UserRoleEnum.STANDARD,"TSU001" );
        }

    }


    @Test
    void addMemberForAcspSendsYouHaveBeenAddedNotificationsWithoutDisplayName() throws Exception {
      final var users = testDataManager.fetchUserDtos( "TSU001", "COMU001" );
      final var acsp = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();
      final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();

      Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
      Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
      Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "TSA001" );
      Mockito.doReturn( List.of() ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU001", false );
      Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "TSU001", "TSA001" );

      mockMvc.perform( post("/acsps/TSA001/memberships")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "TSU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header( "Eric-Authorised-Token-Permissions", "acsp_id=TSA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}") )
              .andExpect( status().isCreated() );

      Mockito.verify( emailService ).sendYouHaveBeenAddedToAcspEmail( "theId123", "jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story" );
    }

    @Test
    void addMemberForAcspSendsYouHaveBeenAddedNotificationsWithDisplayName() throws Exception {
      final var users = testDataManager.fetchUserDtos( "WITU001", "COMU001" );
      final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
      final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();

      Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU001" );
      Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
      Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
      Mockito.doReturn( List.of() ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU001", false );
      Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU001", "WITA001" );

      mockMvc.perform( post("/acsps/WITA001/memberships")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "WITU001")
                      .header("ERIC-Identity-Type", "oauth2")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header( "Eric-Authorised-Token-Permissions", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}") )
              .andExpect( status().isCreated() );

      Mockito.verify( emailService ).sendYouHaveBeenAddedToAcspEmail( "theId123", "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher" );

    }

    @Test
    void addMemberForAcspDoesNotSendYouHaveBeenAddedNotificationsWhenCalledInternally() throws Exception {
      final var users = testDataManager.fetchUserDtos( "WITU001", "COMU001" );
      final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
      final var requestingUsersMembership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();

      Mockito.doReturn( users.getFirst() ).when( usersService ).fetchUserDetails( "WITU001" );
      Mockito.doReturn( users.getLast() ).when( usersService ).fetchUserDetails( "COMU001" );
      Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
      Mockito.doReturn( List.of() ).when( acspMembersService ).fetchAcspMembershipDaos( "COMU001", false );
      Mockito.doReturn( Optional.of( requestingUsersMembership ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU001", "WITA001" );

      mockMvc.perform( post("/acsps/WITA001/memberships")
                      .header("X-Request-Id", "theId123")
                      .header("Eric-identity", "WITU001")
                      .header("ERIC-Identity-Type", "key")
                      .header("ERIC-Authorised-Key-Roles", "*")
                      .header( "Eric-Authorised-Token-Permissions", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}") )
              .andExpect( status().isCreated() );

      Mockito.verify( emailService, times( 0 ) ).sendYouHaveBeenAddedToAcspEmail( "theId123", "jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher" );
    }

}
