package uk.gov.companieshouse.acsp.manage.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembersMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembersServiceTest {

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Mock
    private AcspMembersRepository acspMembersRepository;

    @Mock
    private AcspMembersMapper acspMembersMapper;

    @Mock
    private AcspMembershipListMapper acspMembershipListMapper;

    @InjectMocks
    private AcspMembersService acspMembersService;

    private User testUser;
    private List<AcspMembersDao> testActiveAcspMembersDaos;
    private List<AcspMembersDao> testAllAcspMembersDaos;
    private List<AcspMembership> testAcspMemberships;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("user123");

        testActiveAcspMembersDaos = Arrays.asList(createAcspMembersDao("1", false));
        testAllAcspMembersDaos =
                Arrays.asList(createAcspMembersDao("1", false), createAcspMembersDao("2", true));

        testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
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
  void fetchActiveAcspMemberships_includeRemoved_returnsAllMemberships() {
        when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
                .thenReturn(testAllAcspMembersDaos);
        when(acspMembershipListMapper.daoToDto(testAllAcspMembersDaos, testUser))
                .thenReturn(testAcspMemberships);

        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, true);

        assertEquals(2, result.size());
        assertSame(testAcspMemberships, result);
        verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
        verify(acspMembershipListMapper).daoToDto(testAllAcspMembersDaos, testUser);
        verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
    }

  @Test
  void fetchActiveAcspMemberships_excludeRemoved_returnsOnlyActiveMemberships() {
        when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
                .thenReturn(testActiveAcspMembersDaos);
        when(acspMembershipListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
                .thenReturn(Collections.singletonList(testAcspMemberships.get(0)));

        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

        assertEquals(1, result.size());
        assertSame(testAcspMemberships.get(0), result.get(0));
        verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
        verify(acspMembershipListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
        verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
    }

  @Test
  void fetchActiveAcspMemberships_noMemberships_returnsEmptyList() {
        when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
                .thenReturn(Collections.emptyList());
        when(acspMembershipListMapper.daoToDto(Collections.emptyList(), testUser))
                .thenReturn(Collections.emptyList());

        List<AcspMembership> result = acspMembersService.fetchAcspMemberships(testUser, false);

        assertTrue(result.isEmpty());
        verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
        verify(acspMembershipListMapper).daoToDto(Collections.emptyList(), testUser);
        verifyNoMoreInteractions(acspMembersRepository, acspMembershipListMapper);
    }

  @Test
  void fetchActiveAcspMemberships_repositoryThrowsException_propagatesException() {
        when(acspMembersRepository.fetchActiveAcspMembersByUserId(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class, () -> acspMembersService.fetchAcspMemberships(testUser, false));
    }

    private AcspMembersDao createAcspMembersDao(String id, boolean removed) {
        AcspMembersDao dao = new AcspMembersDao();
        dao.setId(id);
        dao.setRemovedBy(removed ? "remover" : null);
        return dao;
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
        Mockito.doReturn( Page.empty( PageRequest.of(0, 20) ) ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any(Pageable.class) );

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
        Mockito.doReturn( Page.empty( PageRequest.of(0, 20) ) ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any(Pageable.class) );

        final var nonexistentAcspId = new AcspDataDao();
        nonexistentAcspId.setId( "919191" );
        acspMembersService.fetchAcspMembers( nonexistentAcspId, true, null, null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), eq( nonexistentAcspId ) );
    }

  @Test
  void fetchActiveAcspMembersWithMalformedUserIdReturnsEmptyResults() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();

        Mockito.doReturn( Page.empty( PageRequest.of(0, 20) ) ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any(Pageable.class) );

        acspMembersService.fetchAcspMembers( acspData, true, "££££", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), argThat( acspDataDaoMatcher( acspData.getId(), acspData.getAcspName(), acspData.getAcspStatus() ) ) );
    }

  @Test
  void fetchActiveAcspMembersWithNonexistentUserIdReturnsEmptyResults() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();

        Mockito.doReturn( Page.empty( PageRequest.of(0, 20) ) ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any(Pageable.class) );

        acspMembersService.fetchAcspMembers( acspData, true, "9191", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 0, 0, Set.of() ) ), argThat( acspDataDaoMatcher( acspData.getId(), acspData.getAcspName(), acspData.getAcspStatus() ) ) );
    }

  @Test
  void fetchAcspMembersAppliesActiveAcspIdAndRoleAndIncludeRemovedFiltersCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "COM002", "COM010" );

        final var pageRequest = PageRequest.of( 0, 20 );
        final var page = new PageImpl<>( acspMemberDaos, pageRequest, acspMemberDaos.size() );

        Mockito.doReturn( page ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), isNull(), any( Pageable.class ) );

        acspMembersService.fetchAcspMembers( acspData, false, null, "owner", 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 2, 1, Set.of( "COM002", "COM010" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersAppliesUserIdFilterCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "COM002" );

        final var pageRequest = PageRequest.of( 0, 20 );
        final var page = new PageImpl<>( acspMemberDaos, pageRequest, acspMemberDaos.size() );

        Mockito.doReturn( page ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any( Pageable.class ) );

        acspMembersService.fetchAcspMembers( acspData, true, "COMU002", null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 1, 1, Set.of( "COM002" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersWithoutFiltersReturnsEverything() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" );

        final var pageRequest = PageRequest.of( 0, 20 );
        final var page = new PageImpl<>( acspMemberDaos, pageRequest, acspMemberDaos.size() );

        Mockito.doReturn( page ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any( Pageable.class ) );

        acspMembersService.fetchAcspMembers( acspData, true, null, null, 0, 20 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 0, 20, 16, 1, Set.of( "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) ) ), eq( acspData ) );
    }

  @Test
  void fetchActiveAcspMembersAppliesPaginationCorrectly() {
        final var acspData = testDataManager.fetchAcspDataDaos( "COMA001" ).getFirst();
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos( "COM004", "COM005", "COM006" );

        final var pageRequest = PageRequest.of( 1, 3 );
        final var page = new PageImpl<>( acspMemberDaos, pageRequest, 16 );

        Mockito.doReturn( page ).when( acspMembersRepository ).findAllByAcspNumberUserRolesAndUserIdLike( anyString(), anySet(), anyString(), any( Pageable.class ) );

        acspMembersService.fetchAcspMembers( acspData, true, null, null, 1, 3 );
        Mockito.verify( acspMembersMapper ).daoToDto( argThat( acspMembersDaoPageMatcher( 1, 3, 16, 6, Set.of( "COM004", "COM005", "COM006" ) ) ), eq( acspData ) );
    }

  @Test
  void addAcspMember_withIndividualParameters_shouldCreateAndSaveMember() {
    String userId = "user123";
    String acspNumber = "ACSP001";
    AcspMembership.UserRoleEnum userRole = AcspMembership.UserRoleEnum.ADMIN;
    String addedByUserId = "admin456";

    AcspMembersDao expectedDao = new AcspMembersDao();
    expectedDao.setUserId(userId);
    expectedDao.setAcspNumber(acspNumber);
    expectedDao.setUserRole(userRole);
    expectedDao.setAddedBy(addedByUserId);
    expectedDao.setRemovedBy(null);
    expectedDao.setRemovedAt(null);

    when(acspMembersRepository.insert(any(AcspMembersDao.class))).thenReturn(expectedDao);

    AcspMembersDao result =
        acspMembersService.addAcspMember(userId, acspNumber, userRole, addedByUserId);

    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(acspNumber, result.getAcspNumber());
    assertEquals(userRole, result.getUserRole());
    assertEquals(addedByUserId, result.getAddedBy());
    assertNull(result.getRemovedBy());
    assertNull(result.getRemovedAt());

    verify(acspMembersRepository)
        .insert(
            argThat(
                (ArgumentMatcher<AcspMembersDao>)
                    dao ->
                        dao.getUserId().equals(userId)
                            && dao.getAcspNumber().equals(acspNumber)
                            && dao.getUserRole().equals(userRole)
                            && dao.getAddedBy().equals(addedByUserId)
                            && dao.getRemovedBy() == null
                            && dao.getRemovedAt() == null
                            && dao.getCreatedAt() != null
                            && dao.getAddedAt() != null
                            && dao.getEtag() != null));
  }

  @Test
  void addAcspMember_withRequestBodyPost_shouldCreateAndSaveMember() {
    RequestBodyPost requestBodyPost = new RequestBodyPost();
    requestBodyPost.setUserId("user123");
    requestBodyPost.setAcspNumber("ACSP001");
    requestBodyPost.setUserRole(RequestBodyPost.UserRoleEnum.ADMIN);
    String addedByUserId = "admin456";

    AcspMembersDao expectedDao = new AcspMembersDao();
    expectedDao.setUserId(requestBodyPost.getUserId());
    expectedDao.setAcspNumber(requestBodyPost.getAcspNumber());
    expectedDao.setUserRole(AcspMembership.UserRoleEnum.ADMIN);
    expectedDao.setAddedBy(addedByUserId);
    expectedDao.setRemovedBy(null);
    expectedDao.setRemovedAt(null);

    when(acspMembersRepository.insert(any(AcspMembersDao.class))).thenReturn(expectedDao);

    AcspMembersDao result = acspMembersService.addAcspMember(requestBodyPost, addedByUserId);

    assertNotNull(result);
    assertEquals(requestBodyPost.getUserId(), result.getUserId());
    assertEquals(requestBodyPost.getAcspNumber(), result.getAcspNumber());
    assertEquals(AcspMembership.UserRoleEnum.ADMIN, result.getUserRole());
    assertEquals(addedByUserId, result.getAddedBy());
    assertNull(result.getRemovedBy());
    assertNull(result.getRemovedAt());

    verify(acspMembersRepository)
        .insert(
            argThat(
                (ArgumentMatcher<AcspMembersDao>)
                    dao ->
                        dao.getUserId().equals(requestBodyPost.getUserId())
                            && dao.getAcspNumber().equals(requestBodyPost.getAcspNumber())
                            && dao.getUserRole().equals(AcspMembership.UserRoleEnum.ADMIN)
                            && dao.getAddedBy().equals(addedByUserId)
                            && dao.getRemovedBy() == null
                            && dao.getRemovedAt() == null
                            && dao.getCreatedAt() != null
                            && dao.getAddedAt() != null
                            && dao.getEtag() != null));
  }

  @Test
  void addAcspMember_shouldGenerateUniqueEtag() {
    String userId = "user123";
    String acspNumber = "ACSP001";
    AcspMembership.UserRoleEnum userRole = AcspMembership.UserRoleEnum.ADMIN;
    String addedByUserId = "admin456";

    when(acspMembersRepository.insert(any(AcspMembersDao.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AcspMembersDao result1 =
        acspMembersService.addAcspMember(userId, acspNumber, userRole, addedByUserId);
    AcspMembersDao result2 =
        acspMembersService.addAcspMember(userId, acspNumber, userRole, addedByUserId);

    assertNotNull(result1.getEtag());
    assertNotNull(result2.getEtag());
    assertNotEquals(result1.getEtag(), result2.getEtag());
  }
}
