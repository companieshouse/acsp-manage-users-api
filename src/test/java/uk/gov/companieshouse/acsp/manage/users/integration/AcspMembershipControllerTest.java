package uk.gov.companieshouse.acsp.manage.users.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

import java.util.stream.Stream;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipControllerTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    ApiClientService apiClientService;

    @MockBean
    InternalApiClient internalApiClient;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspDataService acspDataService;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

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
    void getAcspMembershipForAcspAndIdRetrievesAcspMembership() throws Exception {
        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( dao );

        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst() ).when( acspDataService ).fetchAcspData( "TSA001" );

        final var response =
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() )
                .andReturn()
                .getResponse();

        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule( new JavaTimeModule() );
        final var acspMembership = objectMapper.readValue( response.getContentAsByteArray(), AcspMembership.class );

        Assertions.assertEquals( dao.getEtag(), acspMembership.getEtag() );
        Assertions.assertEquals( "TS001", acspMembership.getId() );
        Assertions.assertEquals( "TSU001", acspMembership.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER.getValue(), acspMembership.getUserRole().getValue() );
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
        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( dao );

        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst() ).when( acspDataService ).fetchAcspData( "TSA001" );

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
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT004" );

        acspMembersRepository.insert( acspMemberDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

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

    @Test
    void updateAcspMembershipForAcspAndIdWithEmptyRequestBodyReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

        acspMembersRepository.insert( acspMemberDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" )  )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithMalformedStatusReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

        acspMembersRepository.insert( acspMemberDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"complicated\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithMalformedRoleReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT001", "WIT004" );

        acspMembersRepository.insert( acspMemberDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"jester\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdReturnsBadRequestWhenAttemptingToRemoveLastOwner() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "WIT004" );

        acspMembersRepository.insert( acspMemberDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

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

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    private static Stream<Arguments> membershipRemovalSuccessScenarios() {
        return Stream.of(
                Arguments.of( "WIT004", "WIT001" ),
                Arguments.of( "WIT004", "WIT002" ),
                Arguments.of( "WIT004", "WIT003" ),
                Arguments.of( "NEI004", "NEI002" ),
                Arguments.of( "NEI004", "NEI003" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipRemovalSuccessScenarios" )
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyRemovesMembership( final String requestingUserMembershipId, final String targetUserMembershipId ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
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
                Arguments.of( "NEI004", "NEI001" ),
                Arguments.of( "XME004", "XME001" ),
                Arguments.of( "XME004", "XME002" ),
                Arguments.of( "XME004", "XME003" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipRemovalFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToRemoveMembership( final String requestingUserMembershipId, final String targetUserMembershipId ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    private static Stream<Arguments> membershipUpdateRoleSuccessScenarios(){
        return Stream.of(
            Arguments.of( "WIT004", "WIT001", "admin" ),
            Arguments.of( "WIT004", "WIT001", "standard" ),
            Arguments.of( "WIT004", "WIT002", "admin" ),
            Arguments.of( "WIT004", "WIT002", "standard" ),
            Arguments.of( "WIT004", "WIT003", "admin" ),
            Arguments.of( "WIT004", "WIT003", "standard" ),
            Arguments.of( "NEI004", "NEI002", "admin" ),
            Arguments.of( "NEI004", "NEI002", "standard" ),
            Arguments.of( "NEI004", "NEI003", "admin" ),
            Arguments.of( "NEI004", "NEI003", "standard" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleSuccessScenarios" )
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyUpdatesMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_role\":\"%s\"}", userRole ) ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( targetUserMembershipId ).get();
        Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
        Assertions.assertEquals( userRole, updatedDao.getUserRole() );
        Assertions.assertEquals( originalDao.getStatus(), updatedDao.getStatus() );
        Assertions.assertEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
        Assertions.assertEquals( originalDao.getRemovedBy(), updatedDao.getRemovedBy() );
    }

    private static Stream<Arguments> membershipUpdateRoleFailureScenarios(){
        return Stream.of(
            Arguments.of( "WIT004", "WIT001", "owner" ),
            Arguments.of( "WIT004", "WIT002", "owner" ),
            Arguments.of( "WIT004", "WIT003", "owner" ),
            Arguments.of( "NEI004", "NEI001", "owner" ),
            Arguments.of( "NEI004", "NEI002", "owner" ),
            Arguments.of( "NEI004", "NEI003", "owner" ),
            Arguments.of( "NEI004", "NEI001", "admin" ),
            Arguments.of( "NEI004", "NEI001", "standard" ),
            Arguments.of( "XME004", "XME001", "owner" ),
            Arguments.of( "XME004", "XME001", "admin" ),
            Arguments.of( "XME004", "XME001", "standard" ),
            Arguments.of( "XME004", "XME002", "owner" ),
            Arguments.of( "XME004", "XME002", "admin" ),
            Arguments.of( "XME004", "XME002", "standard" ),
            Arguments.of( "XME004", "XME003", "owner" ),
            Arguments.of( "XME004", "XME003", "admin" ),
            Arguments.of( "XME004", "XME003", "standard" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToUpdateRole( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_role\":\"%s\"}", userRole ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdCanUpdateUserRoleAndUserStatusAtTheSameTime() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "WIT004", "WIT002" );
        final var originalDao = acspMembersDaos.getLast();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT002" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedDao = acspMembersRepository.findById( "WIT002" ).get();
        Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
        Assertions.assertEquals( UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole() );
        Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
        Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
        Assertions.assertEquals( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedDao.getRemovedBy() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyByPassesOAuth2Checks() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM004" );

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}
