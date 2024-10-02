package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@WebMvcTest(AcspMembershipController.class)
@Tag("unit-test")
class AcspMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiClientService apiClientService;

    @MockBean
    private InternalApiClient internalApiClient;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void getAcspMembershipForAcspAndIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithMalformedMembershipIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsps/memberships/$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithOAuth2Succeeds() throws Exception {
        Mockito.doReturn( Optional.of( new AcspMembership().id( "TS001" ) ) ).when( acspMembersService ).fetchMembership( "TS001" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithApiKeySucceeds() throws Exception {
        Mockito.doReturn( Optional.of( new AcspMembership().id( "TS001" ) ) ).when( acspMembersService ).fetchMembership( "TS001" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNullXRequestIdThrowsBadRequest() throws Exception {
        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithMalformedMembershipIdThrowsBadRequest() throws Exception {
        mockMvc.perform( patch( "/acsps/memberships/£££" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( Optional.empty() ).when( acspMembersService ).fetchMembershipDao( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    static Stream<Arguments> updateAcspMembershipForAcspAndIdWithMalformedBodyTestData(){
        return Stream.of(
            Arguments.of( "{}" ),
            Arguments.of( "{\"user_status\":\"complicated\"}" ),
            Arguments.of( "{\"user_role\":\"jester\"}" )
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAcspMembershipForAcspAndIdWithMalformedBodyTestData" )
    void updateAcspMembershipForAcspAndIdWithEmptyRequestBodyReturnsBadRequest( final String requestBody ) throws Exception {
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( requestBody )  )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdReturnsBadRequestWhenAttemptingToRemoveLastOwner() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT004" ).getFirst();

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( Optional.of( acspMemberDaos ) ).when( acspMembersService ).fetchMembershipDao( "WIT004" );
        Mockito.doReturn( 1 ).when( acspMembersService ).fetchNumberOfActiveOwners( "WITA004" );

        mockMvc.perform( patch( "/acsps/memberships/WIT004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithInactiveCallerReturnsNotFound() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM004" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );
        Mockito.doReturn( Optional.of( acspMembersDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "COM004" );
        Mockito.doReturn( Optional.empty() ).when( acspMembersService ).fetchActiveAcspMembership( "COMU001",  "COMA001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithAdminCallerAndUserRoleSetToOwnerInRequestBodyReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT002", "WIT003" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "WITU002" ).getFirst() ).when( usersService ).fetchUserDetails( "WITU002" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "WIT003" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "WITU002", "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT003" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU002" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"owner\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithStandardCallerReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "XME004", "XME002" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "XME002" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "XMEA001" );

        mockMvc.perform( patch( "/acsps/memberships/XME002" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithAdminCallerAndOwnerTargetReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "COM004", "COM002" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "COM002" );
        Mockito.doReturn( 2 ).when( acspMembersService ).fetchNumberOfActiveOwners( "COMA001" );
        Mockito.doReturn( Optional.of( acspMemberDaos.getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "COMA001" );

        mockMvc.perform( patch( "/acsps/memberships/COM002" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdCanUpdateUserRoleAndUserStatusAtTheSameTime() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "WIT004", "WIT002" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( Optional.of( acspMembersDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "WIT002" );
        Mockito.doReturn( 2 ).when( acspMembersService ).fetchNumberOfActiveOwners( "WITA001" );
        Mockito.doReturn( Optional.of( acspMembersDaos.getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT002" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        Mockito.verify( acspMembersService ).updateMembership( "WIT002", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyByPassesOAuth2Checks() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM004" );

        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );
        Mockito.doReturn( Optional.of( acspMembersDaos.getLast() ) ).when( acspMembersService ).fetchMembershipDao( "COM004" );
        Mockito.doReturn( 2 ).when( acspMembersService ).fetchNumberOfActiveOwners( "COMA001" );
        Mockito.doReturn( Optional.of( acspMembersDaos.getFirst() ) ).when( acspMembersService ).fetchActiveAcspMembership( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", "COMA001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

}
