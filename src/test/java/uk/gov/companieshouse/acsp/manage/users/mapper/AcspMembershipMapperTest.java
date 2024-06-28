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
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.MapperUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspMembershipMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @InjectMocks
    private AcspMembershipMapper acspMembershipMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

    @BeforeEach
    void setup(){
        acspMembershipMapper  = new AcspMembershipMapper( new BaseMapperImpl(), new MapperUtil( usersService, acspDataService ) );
    }

    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( acspMembershipMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoAppliedToPartialDaoSuccessfullyMapsToDto(){
        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        final var userData = testDataManager.fetchUserDtos( "TSU001" ).getFirst();

        Mockito.doReturn( acspData ).when( acspDataService ).fetchAcspData( "TSA001" );
        Mockito.doReturn( userData ).when( usersService ).fetchUserDetails( "TSU001" );

        final var dto = acspMembershipMapper.daoToDto( dao );

        Assertions.assertEquals( dao.getEtag(), dto.getEtag() );
        Assertions.assertEquals( "TS001", dto.getId() );
        Assertions.assertEquals( "TSU001", dto.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dto.getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", dto.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER, dto.getUserRole() );
        Assertions.assertEquals( "TSA001", dto.getAcspNumber() );
        Assertions.assertEquals( "Toy Story", dto.getAcspName() );
        Assertions.assertEquals( "active", dto.getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getAddedAt() ), reduceTimestampResolution( dto.getAddedAt().toString() ) );
        Assertions.assertNull( dto.getAddedBy() );
        Assertions.assertNull( dto.getRemovedBy() );
        Assertions.assertNull( dto.getRemovedAt() );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
        Assertions.assertEquals( "/TS001", dto.getLinks().getSelf() );
    }

    @Test
    void daoToDtoAppliedToCompleteDaoSuccessfullyMapsToDto(){
        final var dao = testDataManager.fetchAcspMembersDaos( "TS002" ).getFirst();
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        final var userData = testDataManager.fetchUserDtos( "TSU002" ).getFirst();

        Mockito.doReturn( acspData ).when( acspDataService ).fetchAcspData( "TSA001" );
        Mockito.doReturn( userData ).when( usersService ).fetchUserDetails( "TSU002" );

        final var dto = acspMembershipMapper.daoToDto( dao );

        Assertions.assertEquals( dao.getEtag(), dto.getEtag() );
        Assertions.assertEquals( "TS002", dto.getId() );
        Assertions.assertEquals( "TSU002", dto.getUserId() );
        Assertions.assertEquals( "Woody", dto.getUserDisplayName() );
        Assertions.assertEquals( "woody@toystory.com", dto.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, dto.getUserRole() );
        Assertions.assertEquals( "TSA001", dto.getAcspNumber() );
        Assertions.assertEquals( "Toy Story", dto.getAcspName() );
        Assertions.assertEquals( "active", dto.getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getAddedAt() ), reduceTimestampResolution( dto.getAddedAt().toString() ) );
        Assertions.assertEquals( "TSU001", dto.getAddedBy() );
        Assertions.assertEquals( "TSU001", dto.getRemovedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getRemovedAt() ), reduceTimestampResolution( dto.getRemovedAt().toString() ) );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
        Assertions.assertEquals( "/TS002", dto.getLinks().getSelf() );
    }

}
