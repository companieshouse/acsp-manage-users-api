package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.acsp.manage.users.common.ComparisonUtils;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipCollectionMappers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static uk.gov.companieshouse.acsp.manage.users.common.ComparisonUtils.updateMatches;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.ACTIVE;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.PENDING;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.REMOVED;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspMembersServiceTest {

    @Mock
    private AcspMembersRepository acspMembersRepository;

    @Mock
    private AcspMembershipCollectionMappers acspMembershipCollectionMappers;

    @InjectMocks
    private AcspMembersService acspMembersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setXRequestId( request ).build() );
    }

    @Nested
    class FetchAcspMemberships {

        @Test
        void fetchAcspMembershipsReturnsAllAcspMembersIfIncludeRemovedTrue() {
            final var user = testDataManager.fetchUserDtos(  "TSU001" ).getFirst();
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "TS001", "NF001" );
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "TS001", "NF001" );

            Mockito.doReturn( acspMembersDaos ).when( acspMembersRepository ).fetchActiveAndRemovedMembershipsForUserId( "TSU001" );
            Mockito.doReturn( acspMembershipDtos ).when( acspMembershipCollectionMappers ).daoToDto( acspMembersDaos, user, null );

            final var result = acspMembersService.fetchMemberships( user, true, null );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 2, result.getItems().size() );
            Assertions.assertSame( acspMembershipDtos, result.getItems() );
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMembershipsForUserId( "TSU001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( acspMembersDaos, user, null );
        }

        @Test
        void fetchAcspMembershipsReturnsActiveAcspMembersIfIncludeRemovedFalse() {
            final var user = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "TS001" );

            Mockito.doReturn( Optional.of( acspMembersDaos ) ).when( acspMembersRepository ).fetchActiveMembershipForUserId( "TSU001" );
            Mockito.doReturn( acspMembershipDtos ).when( acspMembershipCollectionMappers ).daoToDto( List.of( acspMembersDaos ), user, null );

            final var result = acspMembersService.fetchMemberships( user, false, null );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 1, result.getItems().size() );
            Assertions.assertSame( acspMembershipDtos.getFirst(), result.getItems().getFirst() );
            Mockito.verify( acspMembersRepository ).fetchActiveMembershipForUserId( "TSU001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( List.of( acspMembersDaos ), user, null );
        }

        @Test
        void fetchAcspMembershipsReturnsEmptyListIfNoMemberships() {
            final var user = testDataManager.fetchUserDtos(  "TSU001" ).getFirst();

            Mockito.doReturn( List.of() ).when( acspMembersRepository ).fetchActiveAndRemovedMembershipsForUserId( "TSU001" );
            Mockito.doReturn( List.of() ).when( acspMembershipCollectionMappers ).daoToDto( List.of(), user, null );

            final var result = acspMembersService.fetchMemberships( user, true, null );

            Assertions.assertNotNull( result );
            Assertions.assertTrue( result.getItems().isEmpty() );
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMembershipsForUserId( "TSU001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( List.of(), user, null );
        }

    }

    @Nested
    class FindAllByAcspNumberAndRole {

        @Test
        void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedTrue() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "TS002" );
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "TS002" );
            final var acspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

            Mockito.doReturn( new PageImpl<>( acspMembersDaos ) ).when( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole("TSA001", "admin", PageRequest.of(0, 10 ) );
            Mockito.doReturn( new AcspMembershipsList().items( acspMembershipDtos ) ).when( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspProfile );

            final var result = acspMembersService.fetchMembershipsForAcspNumberAndRole( acspProfile, "admin", true, 0, 10 );

            Assertions.assertNotNull( result );
            Assertions.assertEquals(1, result.getItems().size() );
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole( "TSA001", "admin", PageRequest.of(0, 10 ) );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspProfile );
        }

        @Test
        void findAllByAcspNumberAndRoleWithRoleAndIncludeRemovedFalse() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM002" );
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "COM001", "COM002" );
            final var acspAcspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( new PageImpl<>( acspMembersDaos ) ).when( acspMembersRepository ).fetchActiveMembershipsForAcspNumberAndUserRole( "COMA001", "owner", PageRequest.of(0, 10 ) );
            Mockito.doReturn( new AcspMembershipsList().items( acspMembershipDtos ) ).when( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );

            final var result = acspMembersService.fetchMembershipsForAcspNumberAndRole( acspAcspProfile, "owner", false, 0, 10 );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 2, result.getItems().size() );
            Mockito.verify( acspMembersRepository ).fetchActiveMembershipsForAcspNumberAndUserRole( "COMA001", "owner", PageRequest.of(0, 10 ) );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );
        }

        @Test
        void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedTrue() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM001", "COM002", "COM003" );
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "COM001", "COM002", "COM003" );
            final var acspAcspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( new PageImpl<>( acspMembersDaos ) ).when( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumber( "COMA001", PageRequest.of(0, 10 ) );
            Mockito.doReturn( new AcspMembershipsList().items( acspMembershipDtos ) ).when( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );

            final var result = acspMembersService.fetchMembershipsForAcspNumberAndRole( acspAcspProfile, null, true, 0, 10 );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 3, result.getItems().size() );
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumber( "COMA001", PageRequest.of(0, 10 ) );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );
        }

        @Test
        void findAllByAcspNumberAndRoleWithoutRoleAndIncludeRemovedFalse() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "COM002" );
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos( "COM002" );
            final var acspAcspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

            Mockito.doReturn( new PageImpl<>( acspMembersDaos ) ).when( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumber( "COMA001", PageRequest.of(0, 10 ) );
            Mockito.doReturn( new AcspMembershipsList().items( acspMembershipDtos ) ).when( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );

            final var result = acspMembersService.fetchMembershipsForAcspNumberAndRole( acspAcspProfile, null, true, 0, 10 );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 1, result.getItems().size()) ;
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMembershipsForAcspNumber( "COMA001", PageRequest.of(0, 10 ) );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( new PageImpl<>( acspMembersDaos ), null, acspAcspProfile );
        }
    }

    @Nested
    class FetchMembership {

        @Test
        void fetchMembershipWithNullMembershipIdThrowsIllegalArgumentException() {
            Mockito.doThrow( new IllegalArgumentException( "Cannot be null" ) ).when( acspMembersRepository ).findById( isNull() );
            assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchMembership( null ) );
        }

        @Test
        void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional() {
            Mockito.doReturn( Optional.empty() ).when( acspMembersRepository ).findById( "$$$" );
            Assertions.assertTrue( acspMembersService.fetchMembership( "$$$" ).isEmpty() );
            Mockito.verify( acspMembersRepository ).findById( "$$$" );
        }

        @Test
        void fetchMembershipRetrievesMembership() {
            final var acspMemberDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
            final var acspMembershipDto = testDataManager.fetchAcspMembershipDtos( "TS001" ).getFirst();

            Mockito.doReturn( Optional.of( acspMemberDao ) ).when( acspMembersRepository ).findById( "TS001" );
            Mockito.doReturn( acspMembershipDto ).when( acspMembershipCollectionMappers ).daoToDto( acspMemberDao,null,null );

            final var result = acspMembersService.fetchMembership( "TS001" );

            Assertions.assertTrue( result.isPresent() );
            Assertions.assertSame( acspMembershipDto, result.get());
            Mockito.verify( acspMembersRepository ).findById( "TS001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( acspMemberDao,null,null );
        }
    }

    @Nested
    class FetchAcspMembershipsWithAcspNumber {

        @Test
        void fetchAcspMembershipsWithAcspNumberReturnsAllMembersIfIncludeRemovedTrue() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos( "TS001" );
            final var acspMembershipsDtos = testDataManager.fetchAcspMembershipDtos( "TS001" );
            final var user = testDataManager.fetchUserDtos(  "TSU001" ).getFirst();

            Mockito.doReturn( acspMembersDaos ).when( acspMembersRepository ).fetchActiveAndRemovedMemberships( "TSU001", "TSA001" );
            Mockito.doReturn( acspMembershipsDtos ).when( acspMembershipCollectionMappers ).daoToDto( acspMembersDaos, user,null );

            final var result = acspMembersService.fetchMemberships( user, true, "TSA001" );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 1, result.getItems().size() );
            Assertions.assertSame( acspMembershipsDtos, result.getItems() );
            Mockito.verify( acspMembersRepository ).fetchActiveAndRemovedMemberships( "TSU001", "TSA001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( acspMembersDaos, user, null );
        }

        @Test
        void fetchAcspMembershipsWithAcspNumberReturnsActiveMembersIfIncludeRemovedFalse() {
            final var acspMembersDao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
            final var acspMembershipsDtos = testDataManager.fetchAcspMembershipDtos( "TS001" );
            final var user = testDataManager.fetchUserDtos(  "TSU001" ).getFirst();

            Mockito.doReturn( Optional.of( acspMembersDao ) ).when( acspMembersRepository ).fetchActiveMembership( "TSU001", "TSA001" );
            Mockito.doReturn( acspMembershipsDtos ).when( acspMembershipCollectionMappers ).daoToDto( List.of( acspMembersDao ), user,null );

            final var result = acspMembersService.fetchMemberships( user, false, "TSA001" );

            Assertions.assertNotNull( result );
            Assertions.assertEquals( 1, result.getItems().size() );
            Assertions.assertSame( acspMembershipsDtos, result.getItems() );
            Mockito.verify( acspMembersRepository ).fetchActiveMembership( "TSU001", "TSA001" );
            Mockito.verify( acspMembershipCollectionMappers ).daoToDto( List.of( acspMembersDao ), user, null );
        }
    }

    @Test
    void fetchMembershipDaoWithNullMembershipIdThrowsIllegalArgumentException() {
        Mockito.doThrow( new IllegalArgumentException( "Cannot be null" ) ).when( acspMembersRepository ).findById( isNull() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembersService.fetchMembershipDao( null ) );
    }

    @Test
    void fetchMembershipDaoWithMalformedOrNonExistentMembershipIdReturnsEmptyOptional() {
        Assertions.assertFalse( acspMembersService.fetchMembershipDao( "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchMembershipDao( "TS001" ).isPresent() );
    }

    @Test
    void fetchMembershipDaoRetrievesMembership() {
        acspMembersService.fetchMembershipDao( "TS001" );
        Mockito.verify( acspMembersRepository ).findById( "TS001" );
    }

    @Test
    void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero() {
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners(null ) );
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners("£££" ) );
        Assertions.assertEquals( 0, acspMembersService.fetchNumberOfActiveOwners("TS001" ) );
    }

    @Test
    void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp() {
        acspMembersService.fetchNumberOfActiveOwners( "COMA001" );
        Mockito.verify( acspMembersRepository ).fetchNumberOfActiveOwners( "COMA001" );
    }

    @Test
    void fetchActiveAcspMembershipWithNullOrMalformedOrNonexistentUserIdOrAcspNumberReturnsEmptyOptional() {
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( null, "TSA001" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "£££", "TSA001" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", null ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", "£££" ).isPresent() );
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU001", "TSA001" ).isPresent() );
    }

    @Test
    void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional() {
        Assertions.assertFalse( acspMembersService.fetchActiveAcspMembership( "TSU002", "TSA001" ).isPresent() );
    }

    @Test
    void fetchActiveAcspMembershipRetrievesMembership() {
        acspMembersService.fetchActiveAcspMembership( "TSU001", "TSA001" );
        Mockito.verify( acspMembersRepository ).fetchActiveMembership( "TSU001", "TSA001" );
    }

    @Test
    void updateMembershipWithNullMembershipIdThrowsInternalServerErrorRuntimeException() {
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership( null, MembershipStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
    }

    @Test
    void updateMembershipWithMalformedOrNonexistentMembershipIdThrowsInternalServerErrorRuntimeException() {
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership( "£££", MembershipStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership( "TS001", MembershipStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" ) );
    }

    @Test
    void updateMembershipWithNullUserStatusAndNotNullUserRoleOnlyUpdatesEtagAndRole() {
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), any( Update.class ) );
        acspMembersService.updateMembership("TS001", null, UserRoleEnum.STANDARD, "TSU002" );
        Mockito.verify(acspMembersRepository).updateAcspMembership( eq("TS001"), argThat( updateMatches( Map.of("user_role", UserRoleEnum.STANDARD.getValue() ) ) ) );
    }

    @Test
    void updateMembershipWithNotNullUserStatusAndNullUserRoleOnlyUpdatesEtagAndStatusAndRemovedAtAndRemovedBy() {
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), any( Update.class ) );
        acspMembersService.updateMembership( "TS001", MembershipStatusEnum.REMOVED, null, "TSU002" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership(eq( "TS001" ), argThat( updateMatches( Map.of("status", UserStatusEnum.REMOVED.getValue(), "removed_by", "TSU002") ) ) );
    }

    @Test
    void updateMembershipWithNotNullUserStatusAndNotNullUserRoleOnlyUpdatesEverything() {
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), any( Update.class ) );
        acspMembersService.updateMembership( "TS001", MembershipStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership( eq( "TS001" ), argThat( updateMatches( Map.of("user_role", UserRoleEnum.STANDARD.getValue(), "status", UserStatusEnum.REMOVED.getValue(), "removed_by", "TSU002" ) ) ) );
    }

    @Test
    void updateMembershipWithActiveActivatesMembership(){
        Mockito.doReturn( 1 ).when( acspMembersRepository ).updateAcspMembership( eq( "WIT005" ), any( Update.class ) );
        acspMembersService.updateMembership( "WIT005", MembershipStatusEnum.ACTIVE, null, "WITU404" );
        Mockito.verify( acspMembersRepository ).updateAcspMembership( eq( "WIT005" ), argThat( updateMatches( Map.of( "user_id", "WITU404", "status", MembershipStatusEnum.ACTIVE.getValue() ) ) ) );
    }

    @Test
    void addAcspMembershipReturnsAddedAcspMembership() {
        final var acspMembersDao = testDataManager.fetchAcspMembersDaos( "COM004" ).getFirst();
        final var acspMembershipDto = testDataManager.fetchAcspMembershipDtos( "COM004" ).getFirst();
        final var userDto = testDataManager.fetchUserDtos( "COMU004" ).getFirst();
        final var acspProfile = testDataManager.fetchAcspProfiles( "COMA001" ).getFirst();

        Mockito.doReturn( acspMembersDao ).when( acspMembersRepository ).insert( any( AcspMembersDao.class ) );
        Mockito.doReturn( acspMembershipDto ).when( acspMembershipCollectionMappers ).daoToDto( acspMembersDao, userDto, acspProfile );

        final var result = acspMembersService.createMembership( userDto, acspProfile, UserRoleEnum.ADMIN, "COMU002" );

        assertEquals( "COMU004", result.getUserId() );
    }

    @Test
    void fetchMembershipDaosRetrievesMembershipForUserIdAndIncludeRemovedTrue(){
        Mockito.doReturn( testDataManager.fetchAcspMembersDaos( "WIT006" ) ).when( acspMembersRepository ).fetchMembershipsForUserAndStatus( eq( "WITU005" ), isNull(), eq( Set.of( ACTIVE.getValue(), PENDING.getValue(), REMOVED.getValue() ) ) );
        final var memberships = acspMembersService.fetchMembershipDaos( "WITU005", null, true );
        Assertions.assertEquals( 1, memberships.size() );
        Assertions.assertEquals( "WIT006", memberships.getFirst().getId() );
    }

    @Test
    void fetchMembershipDaosRetrievesMembershipForUserEmailAndIncludeRemovedTrue(){
        Mockito.doReturn( testDataManager.fetchAcspMembersDaos( "WIT007" ) ).when( acspMembersRepository ).fetchMembershipsForUserAndStatus( isNull(), eq( "margarita.witcher@inugami-example.com" ), eq( Set.of( ACTIVE.getValue(), PENDING.getValue(), REMOVED.getValue() ) ) );
        final var memberships = acspMembersService.fetchMembershipDaos( null, "margarita.witcher@inugami-example.com", true );
        Assertions.assertEquals( 1, memberships.size() );
        Assertions.assertEquals( "WIT007", memberships.getFirst().getId() );
    }

    private static Stream<Arguments> fetchMembershipDaosNonexistentScenarios(){
        return Stream.of(
                Arguments.of( "404UserId", null, true ),
                Arguments.of( null, "404UserEmail@test.com", true ),
                Arguments.of( null, "margarita.witcher@inugami-example.com", false )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchMembershipDaosNonexistentScenarios" )
    void fetchMembershipDaosRetrieveEmptyListWhenQueryUnsatisfied( final String userId, final String userEmail, final boolean includeRemoved ){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "WIT005", "WIT006", "WIT007" ) );
        Assertions.assertTrue( acspMembersService.fetchMembershipDaos( userId, userEmail, includeRemoved ).isEmpty() );
    }

    private static Stream<Arguments> createInvitationNullInputScenarios(){
        final var acspProfile = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();

        return Stream.of(
                Arguments.of( null, acspProfile, UserRoleEnum.ADMIN ),
                Arguments.of( "dijkstra.witcher@inugami-example.com", null, UserRoleEnum.ADMIN ),
                Arguments.of( "dijkstra.witcher@inugami-example.com", acspProfile, null )
        );
    }

    @ParameterizedTest
    @MethodSource( "createInvitationNullInputScenarios" )
    void createInvitationWithNullInputsThrowNullPointerExceptions( final String userEmail, final AcspProfile acspProfile, final UserRoleEnum userRoleEnum ){
        Assertions.assertThrows( NullPointerException.class, () -> acspMembersService.createInvitation( userEmail, acspProfile, userRoleEnum, "WITU001" ) );
    }

    @Test
    void createInvitationCreatesPendingMembership(){
        final var acspProfile = testDataManager.fetchAcspProfiles( "WITA001" ).getFirst();
        Mockito.doReturn( testDataManager.fetchAcspMembersDaos( "WIT005" ).getFirst() ).when( acspMembersRepository ).insert( any( AcspMembersDao.class ) );
        Mockito.doReturn( testDataManager.fetchAcspMembershipDtos( "WIT005" ).getFirst() ).when( acspMembershipCollectionMappers ).daoToDto( any( AcspMembersDao.class ), isNull(), eq( acspProfile ) );

        final var membership = acspMembersService.createInvitation( "dijkstra.witcher@inugami-example.com", acspProfile, UserRoleEnum.ADMIN, "WITU001" );

        Assertions.assertNotNull( membership.getEtag() );
        Assertions.assertEquals( "WIT005", membership.getId() );
        Assertions.assertNull( membership.getUserId() );
        Assertions.assertEquals( "Not Provided", membership.getUserDisplayName() );
        Assertions.assertEquals( "dijkstra.witcher@inugami-example.com", membership.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, membership.getUserRole() );
        Assertions.assertEquals( "WITA001", membership.getAcspNumber() );
        Assertions.assertEquals( "Witcher", membership.getAcspName() );
        Assertions.assertEquals( AcspStatusEnum.ACTIVE, membership.getAcspStatus() );
        Assertions.assertEquals( MembershipStatusEnum.PENDING, membership.getMembershipStatus() );
        Assertions.assertNull( membership.getAddedAt() );
        Assertions.assertNotNull( membership.getInvitedAt() );
        Assertions.assertNull( membership.getAcceptedAt() );
        Assertions.assertEquals( "WITU001", membership.getAddedBy() );
        Assertions.assertNull( membership.getRemovedBy() );
        Assertions.assertNull( membership.getRemovedAt() );
        Assertions.assertEquals( "acsp-membership", membership.getKind() );
    }

}
