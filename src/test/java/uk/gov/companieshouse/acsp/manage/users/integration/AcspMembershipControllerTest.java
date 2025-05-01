package uk.gov.companieshouse.acsp.manage.users.integration;

import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.Status;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.*;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipControllerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @Value( "${signin.url}" )
    private String signinUrl;

    private CountDownLatch latch;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

    private void setEmailProducerCountDownLatch( int countdown ){
        latch = new CountDownLatch( countdown );
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when( emailProducer ).sendEmail( any(), any() );
    }

    private void mockFetchUserDetailsFor( final String... userIds ) {
        Arrays.stream( userIds ).forEach( userId -> Mockito.doReturn( testDataManager.fetchUserDtos( userId ).getFirst() ).when( usersService ).fetchUserDetails( userId ) );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithMalformedMembershipIdReturnsBadRequest() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( get( "/acsps/memberships/$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAcspMembershipForAcspAndIdRetrievesAcspMembership() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( dao );

        mockFetchUserDetailsFor("TSU001", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).retrieveUserDetails( "TSU001", null );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "TSA001" ).getFirst() ).when(
                acspProfileService).fetchAcspProfile( "TSA001" );

        final var response =
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "TSU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "TS001" ) ) )
                .andExpect( status().isOk() );

        final var acspMembership = parseResponseTo( response, AcspMembership.class );

        Assertions.assertEquals( dao.getEtag(), acspMembership.getEtag() );
        Assertions.assertEquals( "TS001", acspMembership.getId() );
        Assertions.assertEquals( "TSU001", acspMembership.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( OWNER.getValue(), acspMembership.getUserRole().getValue() );
        Assertions.assertEquals( "TSA001", acspMembership.getAcspNumber() );
        Assertions.assertEquals( "Toy Story", acspMembership.getAcspName() );
        Assertions.assertEquals("active", acspMembership.getAcspStatus().getValue());
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getAddedAt() ), reduceTimestampResolution( acspMembership.getAddedAt().toString() ) );
        Assertions.assertNull( acspMembership.getAddedBy() );
        Assertions.assertNull( acspMembership.getRemovedBy() );
        Assertions.assertNull( acspMembership.getRemovedAt() );
        Assertions.assertEquals( MembershipStatusEnum.ACTIVE, acspMembership.getMembershipStatus() );
        Assertions.assertEquals( DEFAULT_KIND, acspMembership.getKind() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithApiKeySucceeds() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004", "TS001" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "TSA001" ).getFirst() ).when(
                acspProfileService).fetchAcspProfile( "TSA001" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) ) )
                .andExpect( status().isOk() );
    }

    @Test
    void getAcspMembershipForAcspAndIdCanFetchPendingMembership() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005" ) );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "WITA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "WITA001" );

        final var response = mockMvc.perform( get( "/acsps/memberships/WIT005" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );

        final var acspMembership = parseResponseTo( response, AcspMembership.class );

        Assertions.assertNull( acspMembership.getUserId() );
        Assertions.assertEquals( "dijkstra.witcher@inugami-example.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( MembershipStatusEnum.PENDING, acspMembership.getMembershipStatus() );
        Assertions.assertNotNull( acspMembership.getInvitedAt() );
        Assertions.assertNull( acspMembership.getAcceptedAt() );
        Assertions.assertNull( acspMembership.getRemovedAt() );
    }

    @Test
    void getAcspMembershipForAcspAndIdCanFetchAcceptedInvitationMembership() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT006" ) );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "WITA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "WITA001" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "WITU005" ).getFirst() ).when( usersService ).retrieveUserDetails( "WITU005", null );

        final var response = mockMvc.perform( get( "/acsps/memberships/WIT006" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );

        final var acspMembership = parseResponseTo( response, AcspMembership.class );

        Assertions.assertEquals( "WITU005", acspMembership.getUserId() );
        Assertions.assertEquals( "letho.witcher@inugami-example.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( MembershipStatusEnum.ACTIVE, acspMembership.getMembershipStatus() );
        Assertions.assertNotNull( acspMembership.getInvitedAt() );
        Assertions.assertNotNull( acspMembership.getAcceptedAt() );
        Assertions.assertNull( acspMembership.getRemovedAt() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNullXRequestIdThrowsBadRequest() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithMalformedMembershipIdThrowsBadRequest() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/£££" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
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
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( requestBody )  )
                .andExpect( status().isBadRequest() );
    }



    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2ReturnsForbiddenWhenAttemptingToRemoveLastOwner() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "WITA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyAndActiveAcspReturnsForbiddenWhenAttemptingToRemoveLastOwner() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "WITA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyAndCeasedAcspSucceedsWhenAttemptingToRemoveLastOwner() throws Exception {
        final var acspProfile = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        acspProfile.setStatus( Status.CEASED );

        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( acspProfile ).when( acspProfileService ).fetchAcspProfile( "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithInactiveCallerReturnsForbidden() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "COMA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "COMA001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "COM001" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isForbidden() );
    }

    private static Stream<Arguments> membershipRemovalSuccessScenarios() {
        return Stream.of(
                Arguments.of( "WIT004", "WIT001", testDataManager.fetchTokenPermissions( "WIT004" ) ),
                Arguments.of( "WIT004", "WIT002", testDataManager.fetchTokenPermissions( "WIT004" ) ),
                Arguments.of( "WIT004", "WIT003", testDataManager.fetchTokenPermissions( "WIT004" ) ),
                Arguments.of( "NEI004", "NEI002", testDataManager.fetchTokenPermissions( "NEI004" ) ),
                Arguments.of( "NEI004", "NEI003", testDataManager.fetchTokenPermissions( "NEI004" ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipRemovalSuccessScenarios" )
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyRemovesMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( originalDao.getAcspNumber() ).getFirst() ).when( acspProfileService ).fetchAcspProfile( originalDao.getAcspNumber() );


        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( targetUserMembershipId ).get();
        Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
        Assertions.assertEquals( originalDao.getUserRole(), updatedDao.getUserRole() );
        Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
        Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
        Assertions.assertEquals( requestUserId, updatedDao.getRemovedBy() );
    }

    private static Stream<Arguments> membershipRemovalFailureScenarios(){
        return Stream.of(
                Arguments.of( "NEI004", "NEI001", testDataManager.fetchTokenPermissions( "NEI004" ) ),
                Arguments.of( "XME004", "XME001", testDataManager.fetchTokenPermissions( "XME004" ) ),
                Arguments.of( "XME004", "XME002", testDataManager.fetchTokenPermissions( "XME004" ) ),
                Arguments.of( "XME004", "XME003", testDataManager.fetchTokenPermissions( "XME004" ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipRemovalFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsForbiddenWhenAttemptingToRemoveMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        final var targetAcspNumber = acspMembersDaos.getLast().getAcspNumber();
        Mockito.doReturn( testDataManager.fetchAcspProfiles( targetAcspNumber ).getFirst() ).when( acspProfileService ).fetchAcspProfile( targetAcspNumber );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isForbidden() );
    }

    private static Stream<Arguments> membershipUpdateRoleSuccessScenarios(){
        return Stream.of(
            Arguments.of( "WIT004", "WIT001", "owner", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT002", "owner", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT003", "owner", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT001", "admin", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT001", "standard", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT002", "admin", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT002", "standard", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT003", "admin", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "WIT004", "WIT003", "standard", testDataManager.fetchTokenPermissions( "WIT004" ) ),
            Arguments.of( "NEI004", "NEI002", "admin", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI002", "standard", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI003", "admin", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI003", "standard", testDataManager.fetchTokenPermissions( "NEI004" ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleSuccessScenarios" )
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyUpdatesMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();
        final var requestingUser = testDataManager.fetchUserDtos( requestUserId ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( originalDao.getUserId() ).getFirst();
        final var acsp = testDataManager.fetchAcspProfiles( originalDao.getAcspNumber() ).getFirst();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( targetUser.getUserId() );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( acsp.getNumber() );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_role\":\"%s\"}", userRole ) ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( targetUserMembershipId ).get();
        Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
        Assertions.assertEquals( UserRoleEnum.fromValue( userRole ), updatedDao.getUserRole() );
        Assertions.assertEquals( originalDao.getStatus(), updatedDao.getStatus() );
        Assertions.assertEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
        Assertions.assertEquals( originalDao.getRemovedBy(), updatedDao.getRemovedBy() );

        latch.await( 10, TimeUnit.SECONDS );

        final var requestingUserDisplayName = Optional.ofNullable( requestingUser.getDisplayName() ).orElse( requestingUser.getEmail() );
        if ( OWNER.getValue().equals( userRole ) ){
            Mockito.verify( emailProducer ).sendEmail( new YourRoleAtAcspHasChangedToOwnerEmailData( targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl ), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue() );
        } else if ( UserRoleEnum.ADMIN.getValue().equals( userRole ) ){
            Mockito.verify( emailProducer ).sendEmail( new YourRoleAtAcspHasChangedToAdminEmailData( targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl ), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue() );
        } else {
            Mockito.verify( emailProducer ).sendEmail( new YourRoleAtAcspHasChangedToStandardEmailData( targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl ), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue() );
        }
    }

    private static Stream<Arguments> membershipUpdateRoleFailureScenarios(){
        return Stream.of(
            Arguments.of( "NEI004", "NEI001", "owner", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI002", "owner", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI003", "owner", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI001", "admin", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "NEI004", "NEI001", "standard", testDataManager.fetchTokenPermissions( "NEI004" ) ),
            Arguments.of( "XME004", "XME001", "owner", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME001", "admin", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME001", "standard", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME002", "owner", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME002", "admin", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME002", "standard", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME003", "owner", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME003", "admin", testDataManager.fetchTokenPermissions( "XME004" ) ),
            Arguments.of( "XME004", "XME003", "standard", testDataManager.fetchTokenPermissions( "XME004" ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsForbiddenWhenAttemptingToUpdateRole( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        final var targetAcspNumber = acspMembersDaos.getLast().getAcspNumber();
        Mockito.doReturn( testDataManager.fetchAcspProfiles( targetAcspNumber ).getFirst() ).when( acspProfileService ).fetchAcspProfile( targetAcspNumber );


        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_role\":\"%s\"}", userRole ) ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdCanUpdateUserRoleAndUserStatusAtTheSameTime() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "WIT004", "WIT002" );
        final var originalDao = acspMembersDaos.getLast();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "WITU002" ).getFirst() ).when( usersService ).fetchUserDetails( "WITU002" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "WITA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "WITA001" );

        mockMvc.perform( patch( "/acsps/memberships/WIT002" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT004" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( "WIT002" ).get();
        Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
        Assertions.assertEquals( UserRoleEnum.STANDARD, updatedDao.getUserRole() );
        Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
        Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
        Assertions.assertEquals( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedDao.getRemovedBy() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyByPassesOAuth2Checks() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM004" );

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "COMA001" ).getFirst() ).when( acspProfileService ).fetchAcspProfile( "COMA001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdReturnsForbiddenWhenUserAttemptsToActivateNonPendingMembership() throws Exception {
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT007" ).getFirst();
        final var requestingUser = testDataManager.fetchUserDtos( "WITU006" ).getFirst();
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();

        acspMembersRepository.insert( membership );
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( requestingUser.getUserId() );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( acsp.getNumber() );

        mockMvc.perform( patch( "/acsps/memberships/WIT007" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU006" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"approved\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdReturnsForbiddenWhenUserAttemptsToActivateAnotherUsersMembership() throws Exception {
        final var memberships = testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" );
        final var requestingUser = testDataManager.fetchUserDtos( "WITU005" ).getFirst();
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();

        acspMembersRepository.insert( memberships );
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( requestingUser.getUserId() );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( acsp.getNumber() );

        mockMvc.perform( patch( "/acsps/memberships/WIT005" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU005" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"approved\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApprovedUserStatusUpdatesMembershipCorrectly() throws Exception {
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT005" ).getFirst().userRole( OWNER.getValue() );
        final var requestingUser = new User().userId( "WITU404" ).email( "dijkstra.witcher@inugami-example.com" ).displayName( "Dijkstra" );
        final var acsp = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();

        acspMembersRepository.insert( membership );
        Mockito.doReturn( requestingUser ).when( usersService ).fetchUserDetails( requestingUser.getUserId() );
        Mockito.doReturn( acsp ).when( acspProfileService ).fetchAcspProfile( acsp.getNumber() );

        mockMvc.perform( patch( "/acsps/memberships/WIT005" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU404" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions( "WIT001" ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"active\"}" ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( "WIT005" ).get();

        Assertions.assertNotNull( updatedDao.getEtag() );
        Assertions.assertEquals( "WITU404", updatedDao.getUserId() );
        Assertions.assertNull( updatedDao.getUserEmail() );
        Assertions.assertNotNull( updatedDao.getAddedAt() );
        Assertions.assertNotNull( updatedDao.getAcceptedAt() );
        Assertions.assertEquals( MembershipStatusEnum.ACTIVE.getValue(), updatedDao.getStatus() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}
