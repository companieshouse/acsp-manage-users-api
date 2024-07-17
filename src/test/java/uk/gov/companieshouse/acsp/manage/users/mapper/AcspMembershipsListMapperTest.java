package uk.gov.companieshouse.acsp.manage.users.mapper;

import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspMembershipsListMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @InjectMocks
    private AcspMembershipsListMapper acspMembersMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @BeforeEach
    void setup() {
        acspMembersMapper = new AcspMembershipsListMapper(new BaseMapperImpl(), new MapperUtil(usersService, acspDataService));
    }

    @Test
    void daoToDtoWithNullInputsThrowsNullPointerException() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        Assertions.assertThrows(NullPointerException.class, () -> acspMembersMapper.daoToDto(null, acspData));
        Assertions.assertThrows(NullPointerException.class, () -> acspMembersMapper.daoToDto(page, null));
    }

    @Test
    void daoToDtoMapsCorrectNumberOfItems() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);

        Assertions.assertEquals(2, acspMembers.getItems().size());
    }

    @Test
    void daoToDtoMapsFirstItemCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);
        final var firstItem = acspMembers.getItems().getFirst();
        final var firstDao = daos.getFirst();

        Assertions.assertEquals(firstDao.getEtag(), firstItem.getEtag());
        Assertions.assertEquals("TS001", firstItem.getId());
        Assertions.assertEquals("TSU001", firstItem.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, firstItem.getUserDisplayName());
        Assertions.assertEquals("buzz.lightyear@toystory.com", firstItem.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.OWNER, firstItem.getUserRole());
    }

    @Test
    void daoToDtoMapsLastItemCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);
        final var lastItem = acspMembers.getItems().getLast();
        final var lastDao = daos.getLast();

        Assertions.assertEquals(lastDao.getEtag(), lastItem.getEtag());
        Assertions.assertEquals("TS002", lastItem.getId());
        Assertions.assertEquals("TSU002", lastItem.getUserId());
        Assertions.assertEquals("Woody", lastItem.getUserDisplayName());
        Assertions.assertEquals("woody@toystory.com", lastItem.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.ADMIN, lastItem.getUserRole());
    }

    @Test
    void daoToDtoMapsAcspDataCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);
        final var firstItem = acspMembers.getItems().getFirst();

        Assertions.assertEquals("TSA001", firstItem.getAcspNumber());
        Assertions.assertEquals("Toy Story", firstItem.getAcspName());
        Assertions.assertEquals("active", firstItem.getAcspStatus().getValue());
    }

    @Test
    void daoToDtoMapsDateFieldsCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);
        final var firstItem = acspMembers.getItems().getFirst();
        final var lastItem = acspMembers.getItems().getLast();

        Assertions.assertEquals(
                localDateTimeToNormalisedString(daos.getFirst().getAddedAt()),
                reduceTimestampResolution(firstItem.getAddedAt().toString()));
        Assertions.assertEquals(
                localDateTimeToNormalisedString(daos.getLast().getRemovedAt()),
                reduceTimestampResolution(lastItem.getRemovedAt().toString()));
    }

    @Test
    void daoToDtoMapsPaginationCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "TS002");
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001").getFirst();
        final var page = new PageImpl<>(daos, PageRequest.of(0, 15), 2);

        mockUserService();

        final var acspMembers = acspMembersMapper.daoToDto(page, acspData);

        Assertions.assertEquals(
                "/acsps/TSA001/memberships?page_index=0&items_per_page=15",
                acspMembers.getLinks().getSelf());
        Assertions.assertEquals("", acspMembers.getLinks().getNext());
        Assertions.assertEquals(15, acspMembers.getItemsPerPage());
        Assertions.assertEquals(0, acspMembers.getPageNumber());
        Assertions.assertEquals(2, acspMembers.getTotalResults());
        Assertions.assertEquals(1, acspMembers.getTotalPages());
    }

    private void mockUserService() {
        final var userData = testDataManager.fetchUserDtos("TSU001", "TSU002");
        Mockito.doReturn(userData.getFirst()).when(usersService).fetchUserDetails("TSU001");
        Mockito.doReturn(userData.getLast()).when(usersService).fetchUserDetails("TSU002");
    }

}