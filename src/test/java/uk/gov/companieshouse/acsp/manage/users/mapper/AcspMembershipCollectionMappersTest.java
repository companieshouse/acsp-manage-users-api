package uk.gov.companieshouse.acsp.manage.users.mapper;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembershipCollectionMappersTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    private AcspMembershipMapper acspMembershipMapper;

    private AcspMembershipCollectionMappers acspMembershipCollectionMappers;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

    @BeforeEach
    void setup(){
        acspMembershipMapper = new AcspMembershipMapperImpl();
        acspMembershipMapper.usersService = usersService;
        acspMembershipMapper.acspDataService = acspDataService;
        acspMembershipCollectionMappers = new AcspMembershipCollectionMappers( acspMembershipMapper, usersService, acspDataService);
    }

    @Test
    void daoToDtoWithNullInputThrowNullPointerException() {
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        Assertions.assertThrows(NullPointerException.class, () -> acspMembershipCollectionMappers.daoToDto( (List<AcspMembersDao>) null, userData, acspData ) );
    }

    @Test
    void daoToDtoWithEmptyListReturnsEmptyList() {
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        Assertions.assertEquals(List.of(), acspMembershipCollectionMappers.daoToDto( List.of(), userData, acspData ) );
    }

    @Test
    void daoToDtoWithoutUserDataOrAcspDataSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS002").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU002").getFirst();

        Mockito.doReturn(acspData).when(acspDataService).fetchAcspData("TSA001");
        Mockito.doReturn( Map.of( "TSU002", userData ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );

        final var dtos = acspMembershipCollectionMappers.daoToDto(List.of(dao), null, null);
        final var dto = dtos.getFirst();

        Assertions.assertEquals( 1, dtos.size() );
        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS002", dto.getId());
        Assertions.assertEquals("TSU002", dto.getUserId());
        Assertions.assertEquals("Woody", dto.getUserDisplayName());
        Assertions.assertEquals("woody@toystory.com", dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.ADMIN, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals("Toy Story", dto.getAcspName());
        Assertions.assertEquals("active", dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertEquals("TSU001", dto.getAddedBy());
        Assertions.assertEquals("TSU001", dto.getRemovedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getRemovedAt()), reduceTimestampResolution(dto.getRemovedAt().toString()));
        Assertions.assertEquals(MembershipStatusEnum.REMOVED, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoWithUserDataAndAcspDataSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS002").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU002").getFirst();

        final var dtos = acspMembershipCollectionMappers.daoToDto(List.of(dao), userData, acspData);
        final var dto = dtos.getFirst();

        Assertions.assertEquals( 1, dtos.size() );
        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS002", dto.getId());
        Assertions.assertEquals("TSU002", dto.getUserId());
        Assertions.assertEquals("Woody", dto.getUserDisplayName());
        Assertions.assertEquals("woody@toystory.com", dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.ADMIN, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals("Toy Story", dto.getAcspName());
        Assertions.assertEquals("active", dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertEquals("TSU001", dto.getAddedBy());
        Assertions.assertEquals("TSU001", dto.getRemovedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getRemovedAt()), reduceTimestampResolution(dto.getRemovedAt().toString()));
        Assertions.assertEquals(MembershipStatusEnum.REMOVED, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoPageWithNullInputThrowNullPointerException() {
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        Assertions.assertThrows(NullPointerException.class, () -> acspMembershipCollectionMappers.daoToDto( (Page<AcspMembersDao>) null, userData, acspData ) );
    }

    @Test
    void daoToDtoWithEmptyPageReturnsEmptyAcspMembershipsList() {
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        Assertions.assertTrue( acspMembershipCollectionMappers.daoToDto( Page.empty(), userData, acspData ).getItems().isEmpty() );
    }

    @Test
    void daoToDtoWithNullAcspDataThrowsIllegalArgumentException(){
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var page = new PageImpl<>(daos, PageRequest.of(4, 2), 12);
        Assertions.assertThrows( IllegalArgumentException.class, () -> acspMembershipCollectionMappers.daoToDto(page, null, null ) );
    }

    @Test
    void daoToDtoWithoutUserDataSuccessfullyMapsToDtoForOnlyPage() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var userData = testDataManager.fetchUserDtos("TSU001", "TSU002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        Mockito.doReturn( Map.of( "TSU001", userData.getFirst(), "TSU002", userData.getLast() ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );

        final var dtos = acspMembershipCollectionMappers.daoToDto(page, null, acspData);
        final var firstDto = dtos.getItems().getFirst();
        final var secondDto = dtos.getItems().getLast();

        final var links = dtos.getLinks();

        Assertions.assertEquals( 2, dtos.getItems().size() );
        Assertions.assertEquals( "TS001", firstDto.getId() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", firstDto.getUserEmail() );
        Assertions.assertEquals( "Toy Story", firstDto.getAcspName() );
        Assertions.assertEquals( "TS002", secondDto.getId() );
        Assertions.assertEquals( "woody@toystory.com", secondDto.getUserEmail() );
        Assertions.assertEquals( "Toy Story", secondDto.getAcspName() );

        Assertions.assertEquals( 15, dtos.getItemsPerPage() );
        Assertions.assertEquals( 0, dtos.getPageNumber() );
        Assertions.assertEquals( 2, dtos.getTotalResults() );
        Assertions.assertEquals( 1, dtos.getTotalPages() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( "/acsps/TSA001/memberships?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getPrevious() );
    }

    @Test
    void daoToDtoWithUserDataSuccessfullyMapsToDtoForMiddlePage() {
        final var firstDao = testDataManager.fetchAcspMembersDaos("TS001" ).getFirst();
        final var secondDao = testDataManager.fetchAcspMembersDaos("TS001" ).getFirst();
        secondDao.setId( "TS002" );
        secondDao.setStatus( "removed" );
        secondDao.setRemovedBy( "TSU002" );
        secondDao.setRemovedAt( LocalDateTime.now() );
        final var daos = List.of( firstDao, secondDao );

        final var userData = testDataManager.fetchUserDtos("TSU001" );
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(4, 2), 12);

        final var dtos = acspMembershipCollectionMappers.daoToDto(page, userData.getFirst(), acspData);
        final var firstDto = dtos.getItems().getFirst();
        final var secondDto = dtos.getItems().getLast();

        final var links = dtos.getLinks();

        Assertions.assertEquals( 2, dtos.getItems().size() );
        Assertions.assertEquals( "TS001", firstDto.getId() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", firstDto.getUserEmail() );
        Assertions.assertEquals( "Toy Story", firstDto.getAcspName() );
        Assertions.assertEquals( "TS002", secondDto.getId() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", secondDto.getUserEmail() );
        Assertions.assertEquals( "Toy Story", secondDto.getAcspName() );

        Assertions.assertEquals( 2, dtos.getItemsPerPage() );
        Assertions.assertEquals( 4, dtos.getPageNumber() );
        Assertions.assertEquals( 12, dtos.getTotalResults() );
        Assertions.assertEquals( 6, dtos.getTotalPages() );
        Assertions.assertEquals( "/acsps/TSA001/memberships?page_index=5&items_per_page=2", links.getNext() );
        Assertions.assertEquals( "/acsps/TSA001/memberships?page_index=4&items_per_page=2", links.getSelf() );
        Assertions.assertEquals( "/acsps/TSA001/memberships?page_index=3&items_per_page=2", links.getPrevious() );
    }

}
