package uk.gov.companieshouse.acsp.manage.users.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag( "integration-test" )
@DataMongoTest
class AcspMembersRepositoryIntegrationTest extends BaseMongoIntegration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @MockitoBean
    private StaticPropertyUtil staticPropertyUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void findAllNotRemovedByAcspNumberReturnsNotRemovedMembersForGivenAcspNumber() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006" ) );

        final var result = acspMembersRepository.fetchActiveMembershipsForAcspNumber("COMA001", PageRequest.of( 0, 10 ) ).getContent();

        assertEquals( 3, result.size() );
        assertTrue( result.stream().allMatch( member -> member.getAcspNumber().equals("COMA001" ) ) );
        assertTrue( result.stream().allMatch( member -> member.getRemovedBy() == null ) );
    }

    @Test
    void findAllByAcspNumberReturnsAllMembersForGivenAcspNumber() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003", "COM004", "COM005", "COM006" ) );

        final var result = acspMembersRepository.fetchActiveAndRemovedMembershipsForAcspNumber( "COMA001", PageRequest.of(0, 10) );

        assertEquals( 6, result.getTotalElements() );
        assertTrue( result.getContent().stream().allMatch( member -> member.getAcspNumber().equals("COMA001" ) ) );
    }

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
    void fetchAllAcspMembersByUserIdReturnsAllAcspMembersForProvidedUserId() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003", "NF002", "TS002" ) );

        final var result = acspMembersRepository.fetchActiveAndRemovedMembershipsForUserId( "TSU002" );

        assertEquals( 2, result.size() );
        assertTrue( result.stream().anyMatch( elem -> elem.getId().equals( "NF002" ) && elem.getUserId().equals( "TSU002" ) && elem.getStatus().equals( MembershipStatusEnum.ACTIVE.getValue() ) ) );
        assertTrue( result.stream().anyMatch( elem -> elem.getId().equals("TS002") && elem.getUserId().equals( "TSU002" ) && elem.getStatus().equals( MembershipStatusEnum.REMOVED.getValue() ) ) );
    }

    @Test
    void fetchActiveAcspMembersByUserIdReturnsActiveAcspMembersForProvidedUserId() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003", "NF002", "TS002" ) );

        final var result = acspMembersRepository.fetchActiveMembershipForUserId( "TSU002" ).map( List::of ).orElse( List.of() );

        assertEquals( 1, result.size() );
        assertTrue( result.stream().anyMatch( elem -> elem.getId().equals( "NF002" ) && elem.getUserId().equals( "TSU002" ) && elem.getStatus().equals( MembershipStatusEnum.ACTIVE.getValue() ) ) );
    }

    @Test
    void fetchAllAcspMembersByUserIdAndAcspNumberReturnsRemovedAcspMembersForProvidedUserIdAndAcspNumber() {
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003", "NF002", "TS002" ) );

        final var result = acspMembersRepository.fetchActiveAndRemovedMemberships( "COMU001", "COMA001" );

        assertEquals( 1, result.size() );
        assertTrue( result.stream().anyMatch( member -> member.getId().equals( "COM001" ) && member.getUserId().equals( "COMU001" ) && member.getAcspNumber().equals( "COMA001" ) && member.getStatus().equals( MembershipStatusEnum.REMOVED.getValue() ) ) );
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

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}