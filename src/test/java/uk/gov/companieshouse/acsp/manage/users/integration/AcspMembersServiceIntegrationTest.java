package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipsListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Testcontainers
@Tag("integration-test")
class AcspMembersServiceIntegrationTest {

  @Container @ServiceConnection
  private static MongoDBContainer container = new MongoDBContainer("mongo:5");

  @Autowired private MongoTemplate mongoTemplate;

  @MockBean private ApiClientService apiClientService;

  @MockBean private InternalApiClient internalApiClient;

  @MockBean StaticPropertyUtil staticPropertyUtil;

  private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Autowired
    private AcspMembersService acspMembersService;

  @Autowired private AcspMembersRepository acspMembersRepository;

  @MockBean private AcspMembershipListMapper acspMembershipListMapper;

  @MockBean private AcspMembershipsListMapper acspMembershipsListMapper;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspDataService acspDataService;

  private User testUser;

  private void setupEnvironment(){
    testUser = testDataManager.fetchUserDtos("COMU002").get(0);

    acspMembersRepository.deleteAll();

    List<AcspMembersDao> testMembers =
            testDataManager.fetchAcspMembersDaos(
                    "COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
    acspMembersRepository.saveAll(testMembers);

    when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser)))
            .thenAnswer(
                    invocation -> {
                      List<AcspMembersDao> daos = invocation.getArgument(0);
                      return daos.stream()
                              .map(
                                      dao -> {
                                        AcspMembership membership = new AcspMembership();
                                        membership.setAcspNumber(dao.getAcspNumber());
                                        membership.setUserRole(
                                                AcspMembership.UserRoleEnum.fromValue(dao.getUserRole()));
                                        return membership;
                                      })
                              .toList();
                    });
  }

  @Nested
  class FetchAcspMembershipsTests {
    @Test
    void fetchAcspMembershipsReturnsAcspMembershipsListWithAllAcspMembersIfIncludeRemovedTrue() {
      setupEnvironment();

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertEquals(1, result.getItems().size());
      assertTrue(result.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("COMA001")));
    }

    @Test
    void fetchAcspMembershipsReturnsAcspMembershipsListWithActiveAcspMembersIfIncludeRemovedFalse() {
      setupEnvironment();
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

      assertEquals(1, result.getItems().size());
      assertTrue(result.getItems().stream().allMatch(m -> m.getAcspNumber().equals("COMA001")));
    }

    @Test
    void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedTrue() {
      User nonExistentUser = testDataManager.fetchUserDtos("TSU001").get(0);
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(nonExistentUser, true);

      assertTrue(result.getItems().isEmpty());
    }

    @Test
    void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedFalse() {
      User nonExistentUser = testDataManager.fetchUserDtos("TSU001").get(0);
      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(nonExistentUser, false);

      assertTrue(result.getItems().isEmpty());
    }
  }

  @Nested
  class FindAllByAcspNumberAndRoleTests {

    private AcspDataDao acspDataDao;

    @BeforeEach
    void setUp() {
      acspDataDao = new AcspDataDao();
      acspDataDao.setId("COMA001");

      when(acspMembershipsListMapper.daoToDto(any(Page.class), any(AcspDataDao.class)))
          .thenAnswer(
              invocation -> {
                Page<AcspMembersDao> daos = invocation.getArgument(0);
                AcspMembershipsList list = new AcspMembershipsList();
                list.setItems(
                    daos.getContent().stream()
                        .map(
                            dao -> {
                              AcspMembership membership = new AcspMembership();
                              membership.setAcspNumber(dao.getAcspNumber());
                              membership.setUserRole(
                                  AcspMembership.UserRoleEnum.fromValue(dao.getUserRole()));
                              return membership;
                            })
                        .toList());
                return list;
              });
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedTrue() {
      setupEnvironment();
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "COMA001", acspDataDao, "standard", true, 0, 10);

      assertFalse(result.getItems().isEmpty());
      assertTrue(
          result.getItems().stream()
              .allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedFalse() {
      setupEnvironment();
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "COMA001", acspDataDao, "standard", false, 0, 10);

      assertFalse(result.getItems().isEmpty());
      assertTrue(
          result.getItems().stream()
              .allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedTrue() {
      setupEnvironment();
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("COMA001", acspDataDao, null, true, 0, 10);

      assertFalse(result.getItems().isEmpty());
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedFalse() {
      setupEnvironment();
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("COMA001", acspDataDao, null, false, 0, 10);

      assertFalse(result.getItems().isEmpty());
    }

    @Test
    void findAllByAcspNumberAndRoleReturnsEmptyListForNonExistentAcsp() {
      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "NON_EXISTENT", acspDataDao, null, true, 0, 10);

      assertTrue(result.getItems().isEmpty());
    }
  }

    @Nested
    class FetchMembership {
        @Test
        void fetchMembershipWithNullMembershipIdThrowsIllegalArguemntException(){
            Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchMembership( null ) );
        }

        @Test
        void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional(){
            Assertions.assertFalse( acspMembersService.fetchMembership( "$$$" ).isPresent() );
        }

        @Test
        void fetchMembershipRetrievesMembership(){
            acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );

            Mockito.doReturn( testDataManager.fetchUserDtos( "TSU001" ).getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
            Mockito.doReturn( testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst() ).when( acspDataService ).fetchAcspData( "TSA001" );

            Assertions.assertTrue( acspMembersService.fetchMembership( "TS001" ).isPresent() );
        }
    }

  @Test
  void fetchMembershipDaoWithNullMembershipIdThrowsIllegalArgumentException(){
    Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchMembershipDao( null ) );
  }

  @Test
  void fetchMembershipDaoWithMalformedOrNonExistentMembershipIdReturnsEmptyOptional(){
    Assertions.assertFalse( acspMembersService.fetchMembershipDao( "£££" ).isPresent() );
    Assertions.assertFalse( acspMembersService.fetchMembershipDao( "TS001" ).isPresent() );
  }

  @Test
  void fetchMembershipDaoRetrievesMembership(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
    Assertions.assertEquals( "TS001", acspMembersService.fetchMembershipDao( "TS001" ).get().getId() );
  }

  @Test
  void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero(){
    Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( null ) );
    Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( "£££" ) );
    Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( "TS001" ) );
  }

  @Test
  void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001", "COM001", "COM002", "COM003", "COM004" ) );
    Assertions.assertEquals( 1, acspMembersService.fetchNumberOfActiveOwners( "COMA001" ) );
  }

  @Test
  void fetchActiveAcspMembershipWithNullOrMalformedOrNonexistentUserIdOrAcspNumberReturnsEmptyOptional(){
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( null, "TSA001" ).isPresent() );
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "£££", "TSA001" ).isPresent() );
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", null ).isPresent() );
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", "£££" ).isPresent() );
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", "TSA001" ).isPresent() );
  }

  @Test
  void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS002" ) );
    Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU002", "TSA001" ).isPresent() );
  }

  @Test
  void fetchActiveAcspMembershipRetrievesMembership(){
    acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
    Assertions.assertEquals( "TS001", acspMembersService.fetchActiveAcspMembership( "TSU001", "TSA001" ).get().getId() );
  }

  @Test
  void updateMembershipWithNullMembershipIdThrowsIllegalArgumentException(){
    Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.updateMembership( null, UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
  }

  @Test
  void updateMembershipWithMalformedOrNonexistentMembershipIdThrowsInternalServerErrorRuntimeException(){
    Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership( "£££", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
    Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
  }

  @Test
  void updateMembershipWithNullUserStatusAndNullUserRoleOnlyUpdatesEtag(){
    final var originalDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
    acspMembersRepository.insert( originalDao );

    acspMembersService.updateMembership( "TS001", null, null, "TSU002" );
    final var updatedDao = acspMembersRepository.findById( "TS001" ).get();

    Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
    Assertions.assertEquals( originalDao.getUserRole(), updatedDao.getUserRole() );
    Assertions.assertEquals( originalDao.getStatus(), updatedDao.getStatus() );
    Assertions.assertEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
    Assertions.assertEquals( originalDao.getRemovedBy(), updatedDao.getRemovedBy() );
  }

  @Test
  void updateMembershipWithNullUserStatusAndNotNullUserRoleOnlyUpdatesEtagAndRole(){
    final var originalDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
    acspMembersRepository.insert( originalDao );

    acspMembersService.updateMembership( "TS001", null, UserRoleEnum.STANDARD, "TSU002" );
    final var updatedDao = acspMembersRepository.findById( "TS001" ).get();

    Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
    Assertions.assertEquals( UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole() );
    Assertions.assertEquals( originalDao.getStatus(), updatedDao.getStatus() );
    Assertions.assertEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
    Assertions.assertEquals( originalDao.getRemovedBy(), updatedDao.getRemovedBy() );
  }

  @Test
  void updateMembershipWithNotNullUserStatusAndNullUserRoleOnlyUpdatesEtagAndStatusAndRemovedAtAndRemovedBy(){
    final var originalDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
    acspMembersRepository.insert( originalDao );

    acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, null, "TSU002" );
    final var updatedDao = acspMembersRepository.findById( "TS001" ).get();

    Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
    Assertions.assertEquals( originalDao.getUserRole(), updatedDao.getUserRole() );
    Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
    Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
    Assertions.assertEquals( "TSU002", updatedDao.getRemovedBy() );
  }

  @Test
  void updateMembershipWithNotNullUserStatusAndNotNullUserRoleOnlyUpdatesEverything(){
    final var originalDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
    acspMembersRepository.insert( originalDao );

    acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" );
    final var updatedDao = acspMembersRepository.findById( "TS001" ).get();

    Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
    Assertions.assertEquals( UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole() );
    Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
    Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
    Assertions.assertEquals( "TSU002", updatedDao.getRemovedBy() );
  }

  @Test
  void updateMembershipWithNullRequestingUserIdSetsRemovedByToNull(){
    final var originalDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
    acspMembersRepository.insert( originalDao );

    acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, null );
    final var updatedDao = acspMembersRepository.findById( "TS001" ).get();

    Assertions.assertNotEquals( originalDao.getEtag(), updatedDao.getEtag() );
    Assertions.assertEquals( UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole() );
    Assertions.assertEquals( UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus() );
    Assertions.assertNotEquals( originalDao.getRemovedAt(), updatedDao.getRemovedAt() );
    Assertions.assertNull( updatedDao.getRemovedBy() );
  }


    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }
}
