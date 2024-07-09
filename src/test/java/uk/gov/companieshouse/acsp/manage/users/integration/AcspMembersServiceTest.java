package uk.gov.companieshouse.acsp.manage.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
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
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembersMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Testcontainers
@Tag("integration-test")
class AcspMembersServiceTest {

    @Container
    @ServiceConnection
    private static MongoDBContainer container = new MongoDBContainer("mongo:5");

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockBean
    private ApiClientService apiClientService;

    @MockBean
    private InternalApiClient internalApiClient;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Autowired
    private AcspMembersService acspMembersService;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @MockBean
    private AcspMembersMapper acspMembersMapper;

    @MockBean
    private AcspMembershipListMapper acspMembershipListMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("testUser123");

        acspMembersRepository.deleteAll();

        AcspMembersDao activeMember =
                createAcspMembersDao("1", "ACSP001", testUser.getUserId(), UserRoleEnum.ADMIN, false);
        AcspMembersDao removedMember =
                createAcspMembersDao("2", "ACSP002", testUser.getUserId(), UserRoleEnum.STANDARD, true);

        acspMembersRepository.save(activeMember);
        acspMembersRepository.save(removedMember);

        when(acspMembershipListMapper.daoToDto(anyList(), eq(testUser)))
                .thenAnswer(
                        invocation -> {
                            List<AcspMembersDao> daos = invocation.getArgument(0);
                            return daos.stream()
                                    .map(
                                            dao -> {
                                                AcspMembership membership = new AcspMembership();
                                                membership.setAcspNumber(dao.getAcspNumber());
                                                membership.setUserRole(dao.getUserRole());
                                                return membership;
                                            })
                                    .toList();
                        });
    }

    private ArgumentMatcher<Page<AcspMembersDao>> acspMembersDaoPageMatcher( int pageNumber, int itemsPerPage, int totalElements, int totalPages, Set<String> expectedAcspIds ){
        return page -> {
            final var pageNumberIsCorrect = page.getNumber() == pageNumber;
            final var itemsPerPageIsCorrect = page.getSize() == itemsPerPage;
            final var totalElementsIsCorrect = page.getTotalElements() == totalElements;
            final var totalPagesIsCorrect = page.getTotalPages() == totalPages;
            final var contentIsCorrect = page.getContent()
                    .stream()
                    .map( AcspMembersDao::getId )
                    .collect(Collectors.toSet())
                    .containsAll( expectedAcspIds );
            return pageNumberIsCorrect && itemsPerPageIsCorrect && totalElementsIsCorrect && totalPagesIsCorrect && contentIsCorrect;
        };
    }

    private ArgumentMatcher<AcspDataDao> acspDataDaoMatcher( String acspId, String acspName, String acspStatus ){
        return acsp -> {
            final var acspIdIsCorrect = acspId.equals( acsp.getId() );
            final var acspNameIsCorrect = acspName.equals( acsp.getAcspName() );
            final var acspStatusIsCorrect = acspStatus.equals( acsp.getAcspStatus() );
            return acspIdIsCorrect && acspNameIsCorrect && acspStatusIsCorrect;
        };
    }

  @Test
  void
      fetchActiveAcspMembersWithMalformedRoleOrPageIndexOrItemsPerPageThrowsIllegalArgumentException() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();

        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchAcspMembers( acspData, true, null, "teacher", 0, 20 ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchAcspMembers( acspData, true, null, null, -1, 20 ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchAcspMembers( acspData, true, null, null, 0, -1 ) );
    }

  @Test
  void fetchAcspMembersWithMalformedActiveAcspIdReturnsEmptyResults() {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert( acspMemberDaos );

        final var malformedAcspId = new AcspDataDao();
        malformedAcspId.setId( "££££££" );
        acspMembersService.fetchAcspMembers( malformedAcspId, true, null, null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), eq( malformedAcspId ) );
    }

  @Test
  void fetchAcspMembersWithNullActiveAcspDataThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> acspMembersService.fetchAcspMembers( null, true, null, null, 0, 20 ) );
    }

  @Test
  void fetchAcspMembersWithNullActiveAcspNumberThrowsIllegalArgumentException() {
        final var acspDataDao = new AcspDataDao();
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchAcspMembers( acspDataDao, true, null, null, 0, 20 ) );
    }

  @Test
  void fetchAcspMembersWithNonexistentActiveAcspIdReturnsEmptyResults() {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert( acspMemberDaos );

        final var nonexistentAcspId = new AcspDataDao();
        nonexistentAcspId.setId( "919191" );
        acspMembersService.fetchAcspMembers( nonexistentAcspId, true, null, null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), eq( nonexistentAcspId ) );
    }

  @Test
  void fetchActiveAcspMembersWithMalformedUserIdReturnsEmptyResults() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert( acspMemberDaos );

        acspMembersService.fetchAcspMembers( acspData, true, "££££", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), argThat( acspDataDaoMatcher( acspData.getId(), acspData.getAcspName(), acspData.getAcspStatus() ) ) );
    }

  @Test
  void fetchActiveAcspMembersWithNonexistentUserIdReturnsEmptyResults() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert( acspMemberDaos );

        acspMembersService.fetchAcspMembers( acspData, true, "9191", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), argThat( acspDataDaoMatcher( acspData.getId(), acspData.getAcspName(), acspData.getAcspStatus() ) ) );
    }

  @Test
  void fetchAcspMembersAppliesActiveAcspIdAndRoleAndIncludeRemovedFiltersCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert( acspMemberDaos );
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001", "TS002" ) );

        acspMembersService.fetchAcspMembers( acspData, false, null, "owner", 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 2, 1, Set.of( "COM002", "COM010" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersAppliesUserIdFilterCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert(acspMemberDaos );

        acspMembersService.fetchAcspMembers( acspData, true, "COMU002", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 1, 1, Set.of( "COM002" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersWithoutFiltersReturnsEverything() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert(acspMemberDaos );

        acspMembersService.fetchAcspMembers( acspData, true, null, null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 16, 1, Set.of( "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersAppliesPaginationCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        acspMembersRepository.insert(acspMemberDaos );

        acspMembersService.fetchAcspMembers( acspData, true, null, null, 1, 3 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 1, 3, 16, 6, Set.of( "COM004", "COM005", "COM006" ) ) ), eq( acspData ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

  @Test
  void fetchActiveAcspMemberships_includeRemoved_returnsAllMemberships() {
        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> m.getAcspNumber().equals("ACSP001")));
        assertTrue(result.stream().anyMatch(m -> m.getAcspNumber().equals("ACSP002")));
    }

  @Test
  void fetchActiveAcspMemberships_excludeRemoved_returnsOnlyActiveMemberships() {
        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

        assertEquals(1, result.size());
        assertEquals("ACSP001", result.getFirst().getAcspNumber());
    }

  @Test
  void fetchActiveAcspMemberships_noMemberships_returnsEmptyList() {
        User newUser = new User();
        newUser.setUserId("newUser456");

        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(newUser, true);

        assertTrue(result.isEmpty());
    }

  @Test
  void fetchActiveAcspMemberships_nullUser_throwsIllegalArgumentException() {
        assertThrows(
                NullPointerException.class, () -> acspMembersService.fetchAcspMemberships(null, true));
    }

  @Test
  void fetchActiveAcspMemberships_userWithNoMemberships_returnsEmptyList() {
        User userWithNoMemberships = new User();
        userWithNoMemberships.setUserId("noMembershipsUser");

        List<AcspMembership> result =
                acspMembersService.fetchAcspMemberships(userWithNoMemberships, true);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchAcspMembersDaoShouldRetrieveAcspMember(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
        Assertions.assertTrue( acspMembersService.fetchAcspMembersDao( "TS001" ).isPresent() );
    }

    @Test
    void fetchAcspMembersDaoWithNullAcspMemberIdThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchAcspMembersDao( null ) );
    }

    @Test
    void fetchAcspMembersDaoWithMalformedOrNonexistentAcspMemberIdReturnsEmptyOptional(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );

        Assertions.assertFalse( acspMembersService.fetchAcspMembersDao( "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembersDao( "TS002" ).isPresent() );
    }

    @Test
    void fetchAcspMembershipWithNullOrMalformedOrNonexistentAcspNumberOrUserIdReturnsEmptyOptional(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( null, "TSU001" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( "££££££", "TSU001" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( "TS002", "TSU001" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( "TSA001", null ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( "TSA001", "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchAcspMembership( "TSA001", "TSU002" ).isPresent() );
    }

    @Test
    void fetchAcspMembershipRetrievesMembership(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );
        Assertions.assertTrue( acspMembersService.fetchAcspMembership( "TSA001", "TSU001" ).isPresent() );
    }

    @Test
    void updateRoleWithNullAcspMemberIdOrNullOrMalformedUserRoleThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.updateRole( null, "standard" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.updateRole( "TS001", null ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.updateRole( "TS001", "Hippy" ) );
    }

    @Test
    void updateRoleWithMalformedOrNonexistentAcspMemberIdThrowsInternalServerErrorRuntimeException(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateRole( "£££", "standard" ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateRole( "TS002", "standard" ) );
    }

    @Test
    void updateRoleSetsRoleToSpecifiedRole(){
        final var originalAcspMembersDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( originalAcspMembersDao );

        acspMembersService.updateRole( "TS001", UserRoleEnum.STANDARD.getValue() );
        final var updatedAcspMembersDao = acspMembersRepository.findById( "TS001" ).get();

        Assertions.assertEquals( UserRoleEnum.STANDARD, updatedAcspMembersDao.getUserRole() );
        Assertions.assertNotEquals( originalAcspMembersDao.getEtag(), updatedAcspMembersDao.getEtag() );
    }

    @Test
    void removeMemberWithNullAcspMemberIdOrRemovedByUserIdThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.removeMember( null, "TSU002" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.removeMember( "TS001", null ) );
    }

    @Test
    void removeMemberWithMalformedOrNonexistentAcspMemberIdThrowsInternalServerErrorRuntimeException(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001" ) );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.removeMember( "£££", "TSU002" ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.removeMember( "TS002", "TSU002" ) );
    }

    @Test
    void removeMemberSetsRemovedAtAndRemovedByAndEtag(){
        final var originalAcspMembersDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        acspMembersRepository.insert( originalAcspMembersDao );

        acspMembersService.removeMember( "TS001", "TSU002" );
        final var updatedAcspMembersDao = acspMembersRepository.findById( "TS001" ).get();

        Assertions.assertEquals( "TSU002", updatedAcspMembersDao.getRemovedBy() );
        Assertions.assertNotNull( updatedAcspMembersDao.getRemovedAt() );
        Assertions.assertNotEquals( originalAcspMembersDao.getEtag(), updatedAcspMembersDao.getEtag() );
    }

    private AcspMembersDao createAcspMembersDao(
            String id, String acspNumber, String userId, UserRoleEnum userRole, boolean removed) {
        AcspMembersDao dao = new AcspMembersDao();
        dao.setId(id);
        dao.setAcspNumber(acspNumber);
        dao.setUserId(userId);
        dao.setUserRole(userRole);
        dao.setCreatedAt(LocalDateTime.now());
        dao.setAddedAt(LocalDateTime.now());
        dao.setAddedBy("testAdder");
        if (removed) {
            dao.setRemovedAt(LocalDateTime.now());
            dao.setRemovedBy("testRemover");
        }
        dao.setEtag("testEtag");
        return dao;
    }
}
