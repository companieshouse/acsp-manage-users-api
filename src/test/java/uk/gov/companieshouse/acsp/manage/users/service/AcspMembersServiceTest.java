package uk.gov.companieshouse.acsp.manage.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipsListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;

import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembersServiceTest {

  @Mock private AcspMembersRepository acspMembersRepository;

  @Mock private AcspMembershipListMapper acspMembershipListMapper;

  @Mock private AcspMembershipsListMapper acspMembershipsListMapper;

  @Mock
  private AcspMembershipMapper acspMembershipMapper;

  @InjectMocks private AcspMembersService acspMembersService;

  private TestDataManager testDataManager;
  private User testUser;
  private List<AcspMembersDao> testActiveAcspMembersDaos;
  private List<AcspMembersDao> testAllAcspMembersDaos;
  private List<AcspMembership> testAcspMemberships;
  private AcspDataDao testAcspDataDao;

  @BeforeEach
  void setUp() {
    testDataManager = TestDataManager.getInstance();
    testUser = new User();
    testUser.setUserId("COMU002");
    testActiveAcspMembersDaos = List.of(createAcspMembersDao("1", false));
    testAllAcspMembersDaos =
        Arrays.asList(createAcspMembersDao("1", false), createAcspMembersDao("2", true));
    testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
    testUser.setEmail("test@example.com");
    testUser.setDisplayName("Test User");
    testActiveAcspMembersDaos = List.of(createAcspMembersDao("1", false));
    testAllAcspMembersDaos = Arrays.asList(createAcspMembersDao("1", false),
            createAcspMembersDao("2", true));
    testAcspMemberships = Arrays.asList(new AcspMembership(), new AcspMembership());
    testAcspDataDao = new AcspDataDao();
    testAcspDataDao.setId("ACSP001");
    testAcspDataDao.setAcspName("Test ACSP");
    testAcspDataDao.setAcspStatus("ACTIVE");
  }

  private ArgumentMatcher<Update> updateMatches( final Map<String, Object> expectedKeyValuePairs ){
    return update -> {
      final var document = update.getUpdateObject().get("$set", Document.class);
      return expectedKeyValuePairs.entrySet()
              .stream()
              .map( entry -> {
                final var value = document.get( entry.getKey() );
                final var expectedValue = entry.getValue();
                return value.equals( expectedValue );
              } )
              .reduce( (firstIsCorrect, secondIsCorrect) -> firstIsCorrect && secondIsCorrect )
              .get();
    };
  }

  private AcspMembersDao createAcspMembersDao(String id, boolean removed) {
    AcspMembersDao dao = new AcspMembersDao();
    dao.setId(id);
    dao.setRemovedBy(removed ? "remover" : null);
    return dao;
  }

  @Nested
  class FetchAcspMemberships {
    @Test
    void fetchAcspMembershipsReturnsAllAcspMembersIfIncludeRemovedTrue() {
      when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(testAllAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testAllAcspMembersDaos, testUser))
          .thenReturn(testAcspMemberships);

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      assertSame(testAcspMemberships, result.getItems());
      verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(testAllAcspMembersDaos, testUser);
    }

    @Test
    void fetchAcspMembershipsReturnsActiveAcspMembersIfIncludeRemovedFalse() {
      when(acspMembersRepository.fetchActiveAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(testActiveAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
          .thenReturn(Collections.singletonList(testAcspMemberships.get(0)));

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false);

      assertNotNull(result);
      assertEquals(1, result.getItems().size());
      assertSame(testAcspMemberships.get(0), result.getItems().get(0));
      verify(acspMembersRepository).fetchActiveAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
    }

    @Test
    void fetchAcspMembershipsReturnsEmptyListIfNoMemberships() {
      when(acspMembersRepository.fetchAllAcspMembersByUserId(testUser.getUserId()))
          .thenReturn(Collections.emptyList());
      when(acspMembershipListMapper.daoToDto(Collections.emptyList(), testUser))
          .thenReturn(Collections.emptyList());

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true);

      assertNotNull(result);
      assertTrue(result.getItems().isEmpty());
      verify(acspMembersRepository).fetchAllAcspMembersByUserId(testUser.getUserId());
      verify(acspMembershipListMapper).daoToDto(Collections.emptyList(), testUser);
    }
  }

  @Nested
  class FindAllByAcspNumberAndRole {
    private AcspDataDao acspDataDao;
    private Pageable pageable;
    private Page<AcspMembersDao> pageResult;

    @BeforeEach
    void setUp() {
      acspDataDao = new AcspDataDao();
      acspDataDao.setId("ACSP001");
      pageable = PageRequest.of(0, 10);
      pageResult = new PageImpl<>(testAllAcspMembersDaos);
    }

    @Test
    void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedTrue() {
      when(acspMembersRepository.findAllByAcspNumberAndUserRole("ACSP001", "standard", pageable))
              .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, testAcspDataDao))
              .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result = acspMembersService.findAllByAcspNumberAndRole(
              "ACSP001", testAcspDataDao, "standard", true, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllByAcspNumberAndUserRole("ACSP001", "standard", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, testAcspDataDao);
    }


    @Test
    void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedFalse() {
      when(acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
              "ACSP001", "standard", pageable))
          .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole(
              "ACSP001", acspDataDao, "standard", false, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository)
          .findAllNotRemovedByAcspNumberAndUserRole("ACSP001", "standard", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }

    @Test
    void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedTrue() {
      when(acspMembersRepository.findAllByAcspNumber("ACSP001", pageable)).thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("ACSP001", acspDataDao, null, true, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllByAcspNumber("ACSP001", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }

    @Test
    void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedFalse() {
      when(acspMembersRepository.findAllNotRemovedByAcspNumber("ACSP001", pageable))
          .thenReturn(pageResult);
      when(acspMembershipsListMapper.daoToDto(pageResult, acspDataDao))
          .thenReturn(new AcspMembershipsList().items(testAcspMemberships));

      AcspMembershipsList result =
          acspMembersService.findAllByAcspNumberAndRole("ACSP001", acspDataDao, null, false, 0, 10);

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      verify(acspMembersRepository).findAllNotRemovedByAcspNumber("ACSP001", pageable);
      verify(acspMembershipsListMapper).daoToDto(pageResult, acspDataDao);
    }
  }

  @Nested
  class FetchMembership {
    @Test
    void fetchMembershipWithNullMembershipIdThrowsIllegalArgumentException() {
      Mockito.doThrow(new IllegalArgumentException("Cannot be null"))
          .when(acspMembersRepository)
          .findById(isNull());
      assertThrows(IllegalArgumentException.class, () -> acspMembersService.fetchMembership(null));
    }

    @Test
    void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional() {
      when(acspMembersRepository.findById("$$$")).thenReturn(Optional.empty());

      Optional<AcspMembership> result = acspMembersService.fetchMembership("$$$");

      assertFalse(result.isPresent());
      verify(acspMembersRepository).findById("$$$");
    }

    @Test
    void fetchMembershipRetrievesMembership() {
      AcspMembersDao acspMemberDao = testDataManager.fetchAcspMembersDaos("TS001").get(0);
      AcspMembership expectedMembership = new AcspMembership();

      when(acspMembersRepository.findById("TS001")).thenReturn(Optional.of(acspMemberDao));
      when(acspMembershipMapper.daoToDto(acspMemberDao)).thenReturn(expectedMembership);

      Optional<AcspMembership> result = acspMembersService.fetchMembership("TS001");

      assertTrue(result.isPresent());
      assertSame(expectedMembership, result.get());
      verify(acspMembersRepository).findById("TS001");
      verify(acspMembershipMapper).daoToDto(acspMemberDao);
    }
  }

  @Nested
  class FetchAcspMembershipsWithAcspNumber {

    @Test
    void fetchAcspMembershipsWithAcspNumberReturnsAllMembersIfIncludeRemovedTrue() {
      when(acspMembersRepository.fetchAllAcspMembersByUserIdAndAcspNumber(testUser.getUserId(),
              "ACSP001"))
              .thenReturn(testAllAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testAllAcspMembersDaos, testUser))
              .thenReturn(testAcspMemberships);

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, true,
              "ACSP001");

      assertNotNull(result);
      assertEquals(2, result.getItems().size());
      assertSame(testAcspMemberships, result.getItems());
      verify(acspMembersRepository).fetchAllAcspMembersByUserIdAndAcspNumber(testUser.getUserId(),
              "ACSP001");
      verify(acspMembershipListMapper).daoToDto(testAllAcspMembersDaos, testUser);
    }

    @Test
    void fetchAcspMembershipsWithAcspNumberReturnsActiveMembersIfIncludeRemovedFalse() {
      when(acspMembersRepository.fetchActiveAcspMembersByUserIdAndAcspNumber(testUser.getUserId(),
              "ACSP001"))
              .thenReturn(testActiveAcspMembersDaos);
      when(acspMembershipListMapper.daoToDto(testActiveAcspMembersDaos, testUser))
              .thenReturn(Collections.singletonList(testAcspMemberships.get(0)));

      AcspMembershipsList result = acspMembersService.fetchAcspMemberships(testUser, false,
              "ACSP001");

      assertNotNull(result);
      assertEquals(1, result.getItems().size());
      assertSame(testAcspMemberships.get(0), result.getItems().get(0));
      verify(acspMembersRepository).fetchActiveAcspMembersByUserIdAndAcspNumber(
              testUser.getUserId(), "ACSP001");
      verify(acspMembershipListMapper).daoToDto(testActiveAcspMembersDaos, testUser);
    }
  }

    @Test
    void fetchMembershipDaoWithNullMembershipIdThrowsIllegalArgumentException(){
        Mockito.doThrow( new IllegalArgumentException( "Cannot be null" ) ).when( acspMembersRepository ).findById( isNull() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchMembershipDao( null ) );
    }

    @Test
    void fetchMembershipDaoWithMalformedOrNonExistentMembershipIdReturnsEmptyOptional(){
        Assertions.assertFalse( acspMembersService.fetchMembershipDao( "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchMembershipDao( "TS001" ).isPresent() );
    }

    @Test
    void fetchMembershipDaoRetrievesMembership(){
        acspMembersService.fetchMembershipDao( "TS001" );
        Mockito.verify( acspMembersRepository ).findById( eq( "TS001" ) );
    }

    @Test
    void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero(){
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( null ) );
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( "£££" ) );
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners( "TS001" ) );
    }

    @Test
    void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp(){
        acspMembersService.fetchNumberOfActiveOwners( "COMA001" );
        Mockito.verify( acspMembersRepository ).fetchNumberOfActiveOwners( eq( "COMA001" ) );
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
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU002", "TSA001" ).isPresent() );
    }

    @Test
    void fetchActiveAcspMembershipRetrievesMembership(){
        acspMembersService.fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.verify( acspMembersRepository ).fetchActiveAcspMembership( eq( "TSU001" ), eq( "TSA001" ) );
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
    void updateMembershipWithNullUserStatusAndNotNullUserRoleOnlyUpdatesEtagAndRole(){
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq("TS001" ), any( Update.class ) );
        acspMembersService.updateMembership( "TS001", null, UserRoleEnum.STANDARD, "TSU002" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), argThat( updateMatches( Map.of( "user_role", UserRoleEnum.STANDARD.getValue() ) ) ) );
    }

    @Test
    void updateMembershipWithNotNullUserStatusAndNullUserRoleOnlyUpdatesEtagAndStatusAndRemovedAtAndRemovedBy(){
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq("TS001" ), any( Update.class ) );
        acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, null, "TSU002" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), argThat( updateMatches( Map.of( "status", UserStatusEnum.REMOVED.getValue(), "removed_by", "TSU002" ) ) ) );
    }

    @Test
    void updateMembershipWithNotNullUserStatusAndNotNullUserRoleOnlyUpdatesEverything(){
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq("TS001" ), any( Update.class ) );
        acspMembersService.updateMembership( "TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), argThat( updateMatches( Map.of( "user_role", UserRoleEnum.STANDARD.getValue(), "status", UserStatusEnum.REMOVED.getValue(), "removed_by", "TSU002" ) ) ) );
    }

}
