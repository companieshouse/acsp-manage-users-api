package uk.gov.companieshouse.acsp.manage.users.integration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.ACTIVE;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.PENDING;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.REMOVED;

@Tag( "integration-test" )
@DataMongoTest
class AcspMembersRepositoryIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void findAllNotRemovedByAcspNumberAndUserRoleReturnsNotRemovedMembersForGivenAcspNumberAndUserRole() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003", "COM004", "COM005", "COM006" ) );

        final var result = acspMembersRepository.fetchActiveMembershipsForAcspNumberAndUserRole( "COMA001", UserRoleEnum.ADMIN.getValue(), PageRequest.of( 0, 10 ) );

        assertEquals( 2, result.getTotalElements() );
        assertTrue( result.getContent().stream().allMatch( member -> member.getAcspNumber().equals( "COMA001" ) && member.getUserRole().equals( UserRoleEnum.ADMIN ) && member.getRemovedBy() == null ) ) ;
    }

    @Test
    void findAllByAcspNumberAndUserRoleReturnsAllMembersForGivenAcspNumberAndUserRole() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006" ) );

        final var result = acspMembersRepository.fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole( "COMA001", UserRoleEnum.ADMIN.getValue(), PageRequest.of(0, 10) );

        assertEquals( 3, result.getTotalElements() );
        assertTrue( result.getContent().stream().allMatch( member -> member.getAcspNumber().equals( "COMA001" ) && member.getUserRole().equals( UserRoleEnum.ADMIN ) ) );
    }

    @Test
    void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero(){
        Assertions.assertEquals( 0, acspMembersRepository.fetchNumberOfActiveOwners( null ) );
        Assertions.assertEquals( 0, acspMembersRepository.fetchNumberOfActiveOwners( "£££" ) );
        Assertions.assertEquals( 0, acspMembersRepository.fetchNumberOfActiveOwners( "TS001" ) );
    }

    @Test
    void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001", "COM001", "COM002", "COM003", "COM004" ) );
        Assertions.assertEquals( 1, acspMembersRepository.fetchNumberOfActiveOwners( "COMA001" ) );
    }

    @Test
    void fetchActiveAcspMembershipWithNullOrMalformedOrNonexistentUserIdOrAcspNumberReturnsEmptyOptional(){
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( null, "TSA001" ).isPresent() );
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( "£££", "TSA001" ).isPresent() );
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( "TSU001", null ).isPresent() );
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( "TSU001", "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( "TSU001", "TSA001" ).isPresent() );
    }

    @Test
    void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS002" ) );
        Assertions.assertFalse( acspMembersRepository.fetchActiveMembership( "TSU002", "TSA001" ).isPresent() );
    }

    @Test
    void fetchActiveAcspMembershipRetrievesMembership(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
        Assertions.assertEquals( "TS001", acspMembersRepository.fetchActiveMembership( "TSU001", "TSA001" ).get().getId() );
    }

    @Test
    void updateAcspMembershipWithNullOrMalformedOrNonexistentAcspMembershipIdDoesNotPerformUpdate(){
        Assertions.assertEquals( 0, acspMembersRepository.updateAcspMembership( null, new Update().set( "user_role", "standard" ) ) );
        Assertions.assertEquals( 0, acspMembersRepository.updateAcspMembership( "£££", new Update().set( "user_role", "standard" ) ) );
        Assertions.assertEquals( 0, acspMembersRepository.updateAcspMembership( "TS001", new Update().set( "user_role", "standard" ) ) );
    }

    @Test
    void updateAcspMembershipWithNullUpdateThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalStateException.class, () -> acspMembersRepository.updateAcspMembership( "TS001", null ) );
    }

    @Test
    void updateAcspMembershipPerformsUpdate(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
        Assertions.assertEquals( 1, acspMembersRepository.updateAcspMembership( "TS001", new Update().set( "user_role", "standard" ) ) );
        Assertions.assertEquals( UserRoleEnum.STANDARD, acspMembersRepository.findById( "TS001" ).get().getUserRole() );
    }

    @Test
    void fetchMembershipsForUserAndStatusRetrievesMembershipForNonnullUserIdAndNullUserEmail(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006", "WIT007" ) );
        final var memberships = acspMembersRepository.fetchMembershipsForUserAndStatus( "WITU005", null, Set.of( ACTIVE.getValue(), PENDING.getValue() ) );
        Assertions.assertEquals( 1, memberships.size() );
        Assertions.assertEquals( "WIT006", memberships.getFirst().getId() );
    }

    @Test
    void fetchMembershipsForUserAndStatusRetrievesMembershipForNullUserIdAndNonnullUserEmail(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006", "WIT007" ) );
        final var memberships = acspMembersRepository.fetchMembershipsForUserAndStatus( null, "dijkstra.witcher@inugami-example.com", Set.of( PENDING.getValue() ) );
        Assertions.assertEquals( 1, memberships.size() );
        Assertions.assertEquals( "WIT005", memberships.getFirst().getId() );
    }

    private static Stream<Arguments> fetchMembershipsForUserAndStatusNonexistentScenarios(){
        return Stream.of(
                Arguments.of( "404UserId", null, Set.of( ACTIVE.getValue(), PENDING.getValue(), REMOVED.getValue() ) ),
                Arguments.of( null, "404UserEmail@test.com", Set.of( ACTIVE.getValue(), PENDING.getValue(), REMOVED.getValue() ) ),
                Arguments.of( null, "dijkstra.witcher@inugami-example.com", Set.of() ),
                Arguments.of( null, "dijkstra.witcher@inugami-example.com", Set.of( "complicated" ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchMembershipsForUserAndStatusNonexistentScenarios" )
    void fetchMembershipsForUserAndStatusRetrievesEmptyListWhenQueryUnsatisfied( final String userId, final String userEmail, final Set<String> statuses ){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006", "WIT007" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForUserAndStatus( userId, userEmail, statuses ).isEmpty() );
    }

    @Test
    void fetchMembershipsForUserAndStatusWithNullStatusesThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.fetchMembershipsForUserAndStatus( "dijkstra.witcher@inugami-example.com", null, null ) );
    }

    @Test
    void fetchMembershipsForAcspAndStatusesAndRolesWithNullSetsThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( "WITA001", null, Set.of( UserRoleEnum.ADMIN.getValue() ), null ) );
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( "WITA001", Set.of( ACTIVE.getValue(), PENDING.getValue() ), null, null ) );
    }

    @Test
    void fetchMembershipsForAcspAndStatusesAndRolesWithNullAcspNumberReturnsEmptyList(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM002", "WIT001", "WIT002", "WIT003", "WIT004", "WIT005", "WIT006", "WIT007" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( null, Set.of( ACTIVE.getValue(), PENDING.getValue() ), Set.of( UserRoleEnum.ADMIN.getValue() ), null ).toList().isEmpty() );
    }

    private static Stream<Arguments> fetchMembershipsForAcspAndStatusesAndRolesEmptyScenarios(){
        return Stream.of(
                Arguments.of( "404ACSP", new HashSet<>( Set.of( ACTIVE.getValue(), PENDING.getValue() ) ), new HashSet<>( Set.of( UserRoleEnum.ADMIN.getValue() ) ) ),
                Arguments.of( "WITA001", new HashSet<String>(), new HashSet<>( Set.of( UserRoleEnum.ADMIN.getValue() ) ) ),
                Arguments.of( "WITA001", new HashSet<>( Set.of( ACTIVE.getValue(), PENDING.getValue() ) ), new HashSet<String>() ),
                Arguments.of( "WITA001", new HashSet<>( Set.of( "bad_role" ) ), new HashSet<>( Set.of( UserRoleEnum.ADMIN.getValue() ) ) ),
                Arguments.of( "WITA001", new HashSet<>( Set.of( ACTIVE.getValue(), PENDING.getValue() ) ), new HashSet<>( Set.of( "bad_status" ) ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchMembershipsForAcspAndStatusesAndRolesEmptyScenarios" )
    void fetchMembershipsForAcspAndStatusesAndRolesReturnsEmptyListWhenRecordsNotFound( final String acspNumber, final HashSet<String> statuses, final HashSet<String> roles ){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM002", "WIT001", "WIT002", "WIT003", "WIT004", "WIT005", "WIT006", "WIT007" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( acspNumber, statuses, roles, null ).toList().isEmpty() );
    }

    @Test
    void fetchMembershipsForAcspAndStatusesAndRolesAppliesFiltersCorrectly(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM002", "WIT001", "WIT002", "WIT003", "WIT004", "WIT005", "WIT006", "WIT007" ) );

        final var membershipIds = acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( "WITA001", Set.of( ACTIVE.getValue(), PENDING.getValue() ), Set.of( UserRoleEnum.ADMIN.getValue() ), null )
                .map( AcspMembersDao::getId )
                .stream()
                .toList();

        Assertions.assertEquals( 3, membershipIds.size() );
        Assertions.assertTrue( membershipIds.containsAll( List.of(  "WIT002", "WIT005", "WIT006" ) ) );
    }

    @Test
    void fetchMembershipsForAcspAndStatusesAndRolesImplementsPaginationCorrectly(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM002", "WIT001", "WIT002", "WIT003", "WIT004", "WIT005", "WIT006", "WIT007" ) );

        final var membershipIds = acspMembersRepository.fetchMembershipsForAcspAndStatusesAndRoles( "WITA001", Set.of( ACTIVE.getValue(), PENDING.getValue() ), Set.of( UserRoleEnum.ADMIN.getValue() ), PageRequest.of( 1, 1 ) )
                .map( AcspMembersDao::getId )
                .stream()
                .toList();

        Assertions.assertEquals( 1, membershipIds.size() );
        Assertions.assertEquals( "WIT005", membershipIds.getFirst() );
    }

    @Test
    void fetchMembershipsForUserAcspNumberAndStatusesWithNullStatusesThrowsUncategorizedMongoDbException(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( "WITU005", null, "WITA001", null ) );
    }

    private static Stream<Arguments> fetchMembershipsForUserAcspNumberAndStatusesEmptyScenarios(){
        return Stream.of(
                Arguments.of( "WITU404", "WITA001", new HashSet<>( Set.of( ACTIVE.getValue() ) ) ),
                Arguments.of( "WITU005", "WITA404", new HashSet<>( Set.of( ACTIVE.getValue() ) ) ),
                Arguments.of( "WITU005", "WITA001", new HashSet<>() ),
                Arguments.of( "WITU005", "WITA001", new HashSet<>( Set.of( "bad_status" ) ) )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchMembershipsForUserAcspNumberAndStatusesEmptyScenarios" )
    void fetchMembershipsForUserAcspNumberAndStatusesReturnsEmptyListWhenRecordsNotFound( final String userId, final String acspNumber, final HashSet<String> statuses ){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( userId, null, acspNumber, statuses ).isEmpty() );
    }

    @Test
    void fetchMembershipsForUserAcspNumberAndStatusesWithNonexistentEmailReturnsEmptyList(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( null, "404@example.com", "WITA001", Set.of( PENDING.getValue() ) ).isEmpty() );
    }

    @Test
    void fetchMembershipsForUserAcspNumberAndStatusesWithNullAcspNumberReturnsEmptyList(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertTrue( acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( "WITU005", null, null, Set.of( ACTIVE.getValue() ) ).isEmpty());
    }

    @Test
    void fetchMembershipsForUserAcspNumberAndStatusesRetrievesMembershipsForUserId(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertEquals( "WIT006", acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( "WITU005", null, "WITA001", Set.of( ACTIVE.getValue() ) ).getFirst().getId() );
    }

    @Test
    void fetchMembershipsForUserAcspNumberAndStatusesRetrievesMembershipsForUserEmail(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006" ) );
        Assertions.assertEquals( "WIT005", acspMembersRepository.fetchMembershipsForUserAcspNumberAndStatuses( null, "dijkstra.witcher@inugami-example.com", "WITA001", Set.of( PENDING.getValue() ) ).getFirst().getId() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}