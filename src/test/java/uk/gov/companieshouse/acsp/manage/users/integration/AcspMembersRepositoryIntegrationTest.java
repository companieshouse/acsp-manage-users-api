package uk.gov.companieshouse.acsp.manage.users.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration-test")
@DataMongoTest
class AcspMembersRepositoryIntegrationTest {

  @Autowired private MongoTemplate mongoTemplate;

  @MockBean private ApiClientUtil apiClientUtil;

  @MockBean private StaticPropertyUtil staticPropertyUtil;

  private final TestDataManager testDataManager = TestDataManager.getInstance();

  @Autowired private AcspMembersRepository acspMembersRepository;


    @BeforeEach
  void setUp() {
    acspMembersRepository.deleteAll();
  }

  @Nested
  class FindAllNotRemovedByAcspNumber {
    @Test
    void returnsNotRemovedMembersForGivenAcspNumber() {
      // Given
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllNotRemovedByAcspNumber(acspNumber, pageable);

      // Then
      assertEquals(3, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(member -> member.getAcspNumber().equals(acspNumber)));
      assertTrue(result.getContent().stream().allMatch(member -> member.getRemovedBy() == null));
    }
  }

  @Nested
  class FindAllByAcspNumber {
    @Test
    void returnsAllMembersForGivenAcspNumber() {
      // Given
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result = acspMembersRepository.findAllByAcspNumber(acspNumber, pageable);

      // Then
      assertEquals(6, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(member -> member.getAcspNumber().equals(acspNumber)));
    }
  }

  @Nested
  class FindAllNotRemovedByAcspNumberAndUserRole {
    @Test
    void returnsNotRemovedMembersForGivenAcspNumberAndUserRole() {
      // Given
      String acspNumber = "COMA001";
      String userRole = AcspMembership.UserRoleEnum.ADMIN.getValue();
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
              acspNumber, userRole, pageable);

      // Then
      assertEquals(2, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(
                  member ->
                      member.getAcspNumber().equals(acspNumber)
                          && member.getUserRole().equals(userRole)
                          && member.getRemovedBy() == null));
    }
  }

  @Nested
  class FindAllByAcspNumberAndUserRole {
    @Test
    void returnsAllMembersForGivenAcspNumberAndUserRole() {
      // Given
      String acspNumber = "COMA001";
      String userRole = AcspMembership.UserRoleEnum.ADMIN.getValue();
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos(
              "COM001", "COM002", "COM003", "COM004", "COM005", "COM006"));
      Pageable pageable = PageRequest.of(0, 10);

      // When
      Page<AcspMembersDao> result =
          acspMembersRepository.findAllByAcspNumberAndUserRole(acspNumber, userRole, pageable);

      // Then
      assertEquals(3, result.getTotalElements());
      assertTrue(
          result.getContent().stream()
              .allMatch(
                  member ->
                      member.getAcspNumber().equals(acspNumber)
                          && member.getUserRole().equals(userRole)));
    }
  }

  @Nested
  class FetchAllAcspMembersByUserId {
    @Test
    void returnsAllAcspMembersForProvidedUserId() {
      // Given
      final var userId = "TSU002";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      final var result = acspMembersRepository.fetchAllAcspMembersByUserId(userId);

      // Then
      assertEquals(2, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("NF002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue())));
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("TS002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.REMOVED.getValue())));
    }
  }

  @Nested
  class FetchActiveAcspMembersByUserId {
    @Test
    void returnsActiveAcspMembersForProvidedUserId() {
      // Given
      final var userId = "TSU002";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      final var result = acspMembersRepository.fetchActiveAcspMembersByUserId(userId);

      // Then
      assertEquals(1, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  elem ->
                      elem.getId().equals("NF002")
                          && elem.getUserId().equals(userId)
                          && elem.getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue())));
    }
  }

  @Nested
  class FetchAllAcspMembersByUserIdAndAcspNumber {
    @Test
    void returnsRemovedAcspMembersForProvidedUserIdAndAcspNumber() {
      // Given
      String userId = "COMU001";
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      List<AcspMembersDao> result =
          acspMembersRepository.fetchAllAcspMembersByUserIdAndAcspNumber(userId, acspNumber);

      // Then
      assertEquals(1, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  member ->
                      member.getId().equals("COM001")
                          && member.getUserId().equals(userId)
                          && member.getAcspNumber().equals(acspNumber)
                          && member
                              .getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.REMOVED.getValue())));
    }
  }

  @Nested
  class FetchActiveAcspMembersByUserIdAndAcspNumber {
    @Test
    void returnsActiveAcspMembersForProvidedUserIdAndAcspNumber() {
      // Given
      String userId = "COMU002";
      String acspNumber = "COMA001";
      acspMembersRepository.insert(
          testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "NF002", "TS002"));

      // When
      List<AcspMembersDao> result =
          acspMembersRepository.fetchActiveAcspMembersByUserIdAndAcspNumber(userId, acspNumber);

      // Then
      assertEquals(1, result.size());
      assertTrue(
          result.stream()
              .anyMatch(
                  member ->
                      member.getId().equals("COM002")
                          && member.getUserId().equals(userId)
                          && member.getAcspNumber().equals(acspNumber)
                          && member
                              .getStatus()
                              .equals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue())));
    }
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
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( null, "TSA001" ).isPresent() );
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( "£££", "TSA001" ).isPresent() );
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( "TSU001", null ).isPresent() );
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( "TSU001", "£££" ).isPresent() );
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( "TSU001", "TSA001" ).isPresent() );
  }

  @Test
  void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS002" ) );
    Assertions.assertFalse( acspMembersRepository.fetchActiveAcspMembership( "TSU002", "TSA001" ).isPresent() );
  }

  @Test
  void fetchActiveAcspMembershipRetrievesMembership(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
    Assertions.assertEquals( "TS001", acspMembersRepository.fetchActiveAcspMembership( "TSU001", "TSA001" ).get().getId() );
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
    Assertions.assertEquals( "standard", acspMembersRepository.findById( "TS001" ).get().getUserRole() );
  }

  @AfterEach
  public void after() {
    mongoTemplate.dropCollection( AcspMembersDao.class );
  }

}
