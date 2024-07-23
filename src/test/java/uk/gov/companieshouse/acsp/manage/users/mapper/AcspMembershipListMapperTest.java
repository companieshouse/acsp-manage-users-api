package uk.gov.companieshouse.acsp.manage.users.mapper;

import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembershipListMapperTest {

    @Mock private UsersService usersService;

    @Mock private AcspDataService acspDataService;

    @InjectMocks private AcspMembershipListMapper acspMembershipListMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @BeforeEach
    void setup() {
        acspMembershipListMapper =
                new AcspMembershipListMapper(
                        new BaseMapperImpl(), new MapperUtil(usersService, acspDataService));
    }

    @Test
    void daoToDtoWithNullInputsThrowNullPointerException() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();

        Assertions.assertThrows(
                NullPointerException.class, () -> acspMembershipListMapper.daoToDto(null, userData));
        Assertions.assertThrows(
                NullPointerException.class, () -> acspMembershipListMapper.daoToDto(daos, null));
    }

    @Test
    void daoToDtoWithEmptyListReturnsEmptyList() {
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        Assertions.assertTrue(acspMembershipListMapper.daoToDto(List.of(), userData).isEmpty());
    }

    @Test
    void daoToDtoReturnsCorrectNumberOfItems() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);

        Assertions.assertEquals(2, dtos.size());
    }

    @Test
    void daoToDtoMapsBasicFieldsCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);
        final var firstDto = dtos.getFirst();

        Assertions.assertEquals(daos.getFirst().getEtag(), firstDto.getEtag());
        Assertions.assertEquals("TS001", firstDto.getId());
        Assertions.assertEquals("TSU001", firstDto.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, firstDto.getUserDisplayName());
        Assertions.assertEquals("buzz.lightyear@toystory.com", firstDto.getUserEmail());
    }

    @Test
    void daoToDtoMapsAcspDataCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);
        final var firstDto = dtos.getFirst();

        Assertions.assertEquals("TSA001", firstDto.getAcspNumber());
        Assertions.assertEquals("Toy Story", firstDto.getAcspName());
        Assertions.assertEquals("live", firstDto.getAcspStatus().getValue());
    }

    @Test
    void daoToDtoMapsDateFieldsCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);
        final var firstDto = dtos.getFirst();
        final var lastDto = dtos.getLast();

        Assertions.assertEquals(
                localDateTimeToNormalisedString(daos.getFirst().getAddedAt()),
                reduceTimestampResolution(firstDto.getAddedAt().toString()));
        Assertions.assertEquals(
                localDateTimeToNormalisedString(daos.getLast().getRemovedAt()),
                reduceTimestampResolution(lastDto.getRemovedAt().toString()));
    }

    @Test
    void daoToDtoMapsNullableFieldsCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);
        final var firstDto = dtos.getFirst();
        final var lastDto = dtos.getLast();

        Assertions.assertNull(firstDto.getAddedBy());
        Assertions.assertNull(firstDto.getRemovedBy());
        Assertions.assertNull(firstDto.getRemovedAt());

        Assertions.assertEquals("TSU002", lastDto.getAddedBy());
        Assertions.assertEquals("TSU002", lastDto.getRemovedBy());
    }

    @Test
    void daoToDtoMapsUserRoleCorrectly() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS001", "NF001");
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);

        Assertions.assertEquals(UserRoleEnum.OWNER, dtos.getFirst().getUserRole());
        Assertions.assertEquals(UserRoleEnum.ADMIN, dtos.getLast().getUserRole());
    }

    @Test
    void daoToDtoWithDisplayNameUsesProvidedName() {
        final var daos = testDataManager.fetchAcspMembersDaos("TS002", "NF002");
        final var userData = testDataManager.fetchUserDtos("TSU002").getFirst();
        mockAcspDataService();

        final var dtos = acspMembershipListMapper.daoToDto(daos, userData);

        Assertions.assertEquals("Woody", dtos.getFirst().getUserDisplayName());
        Assertions.assertEquals("Woody", dtos.getLast().getUserDisplayName());
    }

    private void mockAcspDataService() {
        final var acspData = testDataManager.fetchAcspDataDaos("TSA001", "NFA001");
        Mockito.doReturn(acspData.getFirst()).when(acspDataService).fetchAcspData("TSA001");
        Mockito.doReturn(acspData.getLast()).when(acspDataService).fetchAcspData("NFA001");
    }
}