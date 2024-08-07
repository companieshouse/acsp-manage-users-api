package uk.gov.companieshouse.acsp.manage.users.integration;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;

import java.util.stream.Stream;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.parseResponseTo;

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

    private final TestDataManager testDataManager = TestDataManager.getInstance();

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
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ))
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithMalformedMembershipIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsps/memberships/$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ))
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ))
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAcspMembershipForAcspAndIdRetrievesAcspMembership() throws Exception {
        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( dao );

        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "TSA001" ).getFirst() ).when(
                acspProfileService).fetchAcspProfile( "TSA001" );

        final var response =
        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ))
                .andExpect( status().isOk() );

        final var acspMembership = parseResponseTo( response, AcspMembership.class );

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
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( testDataManager.fetchAcspProfiles( "TSA001" ).getFirst() ).when(
                acspProfileService).fetchAcspProfile( "TSA001" );

        mockMvc.perform( get( "/acsps/memberships/TS001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" ))
                .andExpect( status().isOk() );
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNullXRequestIdThrowsBadRequest() throws Exception {
        mockMvc.perform( patch( "/acsps/memberships/WIT001" )
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( requestBody )  )
                .andExpect( status().isBadRequest() );
    }



    @Test
    void updateAcspMembershipForAcspAndIdReturnsBadRequestWhenAttemptingToRemoveLastOwner() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" ).getFirst() ).when( usersService ).fetchUserDetails( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );

        mockMvc.perform( patch( "/acsps/memberships/WIT004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );

    }

    @Test
    void updateAcspMembershipForAcspAndIdWithInactiveCallerReturnsNotFound() throws Exception {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM004" ) );

        Mockito.doReturn( testDataManager.fetchUserDtos( "COMU001" ).getFirst() ).when( usersService ).fetchUserDetails( "COMU001" );

        mockMvc.perform( patch( "/acsps/memberships/COM004" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001" )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", "acsp_members=read" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    private static Stream<Arguments> membershipRemovalSuccessScenarios() {
        return Stream.of(
                Arguments.of( "WIT004", "WIT001", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
                Arguments.of( "WIT004", "WIT002", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
                Arguments.of( "WIT004", "WIT003", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
                Arguments.of( "NEI004", "NEI002", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
                Arguments.of( "NEI004", "NEI003", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
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
                Arguments.of( "NEI004", "NEI001", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
                Arguments.of( "XME004", "XME001", "acsp_id=XMEA001 acsp_members=read" ),
                Arguments.of( "XME004", "XME002", "acsp_id=XMEA001 acsp_members=read" ),
                Arguments.of( "XME004", "XME003", "acsp_id=XMEA001 acsp_members=read" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipRemovalFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToRemoveMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    private static Stream<Arguments> membershipUpdateRoleSuccessScenarios(){
        return Stream.of(
            Arguments.of( "WIT004", "WIT001", "owner", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT002", "owner", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT003", "owner", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT001", "admin", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT001", "standard", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT002", "admin", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT002", "standard", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT003", "admin", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "WIT004", "WIT003", "standard", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI002", "admin", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI002", "standard", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI003", "admin", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI003", "standard", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleSuccessScenarios" )
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyUpdatesMembership( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions ) throws Exception {
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
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
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
            Arguments.of( "NEI004", "NEI001", "owner", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI002", "owner", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI003", "owner", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI001", "admin", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "NEI004", "NEI001", "standard", "acsp_id=NEIA001 acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" ),
            Arguments.of( "XME004", "XME001", "owner", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME001", "admin", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME001", "standard", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME002", "owner", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME002", "admin", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME002", "standard", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME003", "owner", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME003", "admin", "acsp_id=XMEA001 acsp_members=read" ),
            Arguments.of( "XME004", "XME003", "standard", "acsp_id=XMEA001 acsp_members=read" )
        );
    }

    @ParameterizedTest
    @MethodSource( "membershipUpdateRoleFailureScenarios" )
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToUpdateRole( final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions ) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( requestingUserMembershipId, targetUserMembershipId );
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert( acspMembersDaos );
        Mockito.doReturn( testDataManager.fetchUserDtos( requestUserId ).getFirst() ).when( usersService ).fetchUserDetails( requestUserId );

        mockMvc.perform( patch( String.format( "/acsps/memberships/%s", targetUserMembershipId ) )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId )
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header( "Eric-Authorised-Token-Permissions", tokenPermissions )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_id=WITA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
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
                        .header( "Eric-Authorised-Token-Permissions", "acsp_id=COMA001 acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"user_role\":\"standard\",\"user_status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}
