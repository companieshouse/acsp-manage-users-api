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

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
public class AcspMembershipListMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @InjectMocks
    private AcspMembershipListMapper acspMembershipListMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_KIND = "acsp-membership";

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @BeforeEach
    void setup(){
        acspMembershipListMapper = new AcspMembershipListMapper( new BaseMapperImpl(), new MapperUtil( usersService, acspDataService ) );
    }

    @Test
    void daoToDtoWithNullInputsThrowNullPointerException(){
        final var daos = testDataManager.fetchAcspMembersDaos( "TS001", "NF001" );
        final var userData = testDataManager.fetchUserDtos( "TSU001" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> acspMembershipListMapper.daoToDto( null, userData ) );
        Assertions.assertThrows( NullPointerException.class, () -> acspMembershipListMapper.daoToDto( daos, null ) );
    }

    @Test
    void daoToDtoWithoutDisplayNameDoesMappingCorrectly(){
        final var daos = testDataManager.fetchAcspMembersDaos( "TS001", "NF001" );
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001", "NFA001" );
        final var userData = testDataManager.fetchUserDtos( "TSU001" ).getFirst();

        Mockito.doReturn( acspData.getFirst() ).when( acspDataService ).fetchAcspData( "TSA001" );
        Mockito.doReturn( acspData.getLast() ).when( acspDataService ).fetchAcspData( "NFA001" );

        final var dtos = acspMembershipListMapper.daoToDto( daos, userData );

        Assertions.assertEquals( 2, dtos.size() );

        Assertions.assertEquals( daos.getFirst().getEtag(), dtos.getFirst().getEtag() );
        Assertions.assertEquals( "TS001", dtos.getFirst().getId() );
        Assertions.assertEquals( "TSU001", dtos.getFirst().getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dtos.getFirst().getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", dtos.getFirst().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER, dtos.getFirst().getUserRole() );
        Assertions.assertEquals( "TSA001", dtos.getFirst().getAcspNumber() );
        Assertions.assertEquals( "Toy Story", dtos.getFirst().getAcspName() );
        Assertions.assertEquals( "active", dtos.getFirst().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getFirst().getAddedAt() ), reduceTimestampResolution( dtos.getFirst().getAddedAt().toString() ) );
        Assertions.assertNull( dtos.getFirst().getAddedBy() );
        Assertions.assertNull( dtos.getFirst().getRemovedBy() );
        Assertions.assertNull( dtos.getFirst().getRemovedAt() );
        Assertions.assertEquals( DEFAULT_KIND, dtos.getFirst().getKind() );
        Assertions.assertEquals( "/TS001", dtos.getFirst().getLinks().getSelf() );

        Assertions.assertEquals( daos.getLast().getEtag(), dtos.getLast().getEtag() );
        Assertions.assertEquals( "NF001", dtos.getLast().getId() );
        Assertions.assertEquals( "TSU001", dtos.getLast().getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dtos.getLast().getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", dtos.getLast().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, dtos.getLast().getUserRole() );
        Assertions.assertEquals( "NFA001", dtos.getLast().getAcspNumber() );
        Assertions.assertEquals( "Netflix", dtos.getLast().getAcspName() );
        Assertions.assertEquals( "active", dtos.getLast().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getLast().getAddedAt() ), reduceTimestampResolution( dtos.getLast().getAddedAt().toString() ) );
        Assertions.assertEquals( "TSU002", dtos.getLast().getAddedBy() );
        Assertions.assertEquals( "TSU002", dtos.getLast().getRemovedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getLast().getRemovedAt() ), reduceTimestampResolution( dtos.getLast().getRemovedAt().toString() ) );
        Assertions.assertEquals( DEFAULT_KIND, dtos.getLast().getKind() );
        Assertions.assertEquals( "/NF001", dtos.getLast().getLinks().getSelf() );
    }

    @Test
    void daoToDtoWithDisplayNameDoesMappingCorrectly(){
        final var daos = testDataManager.fetchAcspMembersDaos( "TS002", "NF002" );
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001", "NFA001" );
        final var userData = testDataManager.fetchUserDtos( "TSU002" ).getFirst();

        Mockito.doReturn( acspData.getFirst() ).when( acspDataService ).fetchAcspData( "TSA001" );
        Mockito.doReturn( acspData.getLast() ).when( acspDataService ).fetchAcspData( "NFA001" );

        final var dtos = acspMembershipListMapper.daoToDto( daos, userData );

        Assertions.assertEquals( 2, dtos.size() );

        Assertions.assertEquals( daos.getFirst().getEtag(), dtos.getFirst().getEtag() );
        Assertions.assertEquals( "TS002", dtos.getFirst().getId() );
        Assertions.assertEquals( "TSU002", dtos.getFirst().getUserId() );
        Assertions.assertEquals( "Woody", dtos.getFirst().getUserDisplayName() );
        Assertions.assertEquals( "woody@toystory.com", dtos.getFirst().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, dtos.getFirst().getUserRole() );
        Assertions.assertEquals( "TSA001", dtos.getFirst().getAcspNumber() );
        Assertions.assertEquals( "Toy Story", dtos.getFirst().getAcspName() );
        Assertions.assertEquals( "active", dtos.getFirst().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getFirst().getAddedAt() ), reduceTimestampResolution( dtos.getFirst().getAddedAt().toString() ) );
        Assertions.assertEquals( "TSU001", dtos.getFirst().getAddedBy() );
        Assertions.assertEquals( "TSU001", dtos.getFirst().getRemovedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getFirst().getRemovedAt() ), reduceTimestampResolution( dtos.getFirst().getRemovedAt().toString() ) );
        Assertions.assertEquals( DEFAULT_KIND, dtos.getFirst().getKind() );
        Assertions.assertEquals( "/TS002", dtos.getFirst().getLinks().getSelf() );

        Assertions.assertEquals( daos.getLast().getEtag(), dtos.getLast().getEtag() );
        Assertions.assertEquals( "NF002", dtos.getLast().getId() );
        Assertions.assertEquals( "TSU002", dtos.getLast().getUserId() );
        Assertions.assertEquals( "Woody", dtos.getLast().getUserDisplayName() );
        Assertions.assertEquals( "woody@toystory.com", dtos.getLast().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER, dtos.getLast().getUserRole() );
        Assertions.assertEquals( "NFA001", dtos.getLast().getAcspNumber() );
        Assertions.assertEquals( "Netflix", dtos.getLast().getAcspName() );
        Assertions.assertEquals( "active", dtos.getLast().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getLast().getAddedAt() ), reduceTimestampResolution( dtos.getLast().getAddedAt().toString() ) );
        Assertions.assertNull( dtos.getLast().getAddedBy() );
        Assertions.assertNull( dtos.getLast().getRemovedBy() );
        Assertions.assertNull( dtos.getLast().getRemovedAt() );
        Assertions.assertEquals( DEFAULT_KIND, dtos.getLast().getKind() );
        Assertions.assertEquals( "/NF002", dtos.getLast().getLinks().getSelf() );
    }

    @Test
    void daoToDtoWithEmptyListReturnsEmptyList(){
        final var userData = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
        Assertions.assertTrue( acspMembershipListMapper.daoToDto( List.of(), userData ).isEmpty() );
    }

}
