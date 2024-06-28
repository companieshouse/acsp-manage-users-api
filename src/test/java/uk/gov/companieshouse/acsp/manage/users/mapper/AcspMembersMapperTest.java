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
public class AcspMembersMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @InjectMocks
    private AcspMembersMapper acspMembersMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_KIND = "acsp-membership";

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @BeforeEach
    void setup(){
        acspMembersMapper = new AcspMembersMapper( new BaseMapperImpl(), new MapperUtil( usersService, acspDataService ) );
    }

    @Test
    void daoToDtoWithNullInputsThrowsNullPointerException(){
        final var daos = testDataManager.fetchAcspMembersDaos( "TS001", "TS002" );
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        final var userData = testDataManager.fetchUserDtos( "TSU001", "TSU002" );

        Mockito.doReturn( userData.getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( userData.getLast() ).when( usersService ).fetchUserDetails( "TSU002" );

        final var page = new PageImpl<>( daos, PageRequest.of(0,15 ),2 );

        Assertions.assertThrows( NullPointerException.class, () -> acspMembersMapper.daoToDto( null, acspData ) );
        Assertions.assertThrows( NullPointerException.class, () -> acspMembersMapper.daoToDto( page, null ) );
    }

    @Test
    void daoToDtoPerformsMappingCorrectly(){
        final var daos = testDataManager.fetchAcspMembersDaos( "TS001", "TS002" );
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();
        final var userData = testDataManager.fetchUserDtos( "TSU001", "TSU002" );

        Mockito.doReturn( userData.getFirst() ).when( usersService ).fetchUserDetails( "TSU001" );
        Mockito.doReturn( userData.getLast() ).when( usersService ).fetchUserDetails( "TSU002" );

        final var page = new PageImpl<>( daos, PageRequest.of(0,15 ),2 );

        final var acspMembers = acspMembersMapper.daoToDto( page, acspData );
        final var items = acspMembers.getItems();
        final var links = acspMembers.getLinks();

        Assertions.assertEquals( 2, items.size() );

        Assertions.assertEquals( daos.getFirst().getEtag(), items.getFirst().getEtag() );
        Assertions.assertEquals( "TS001", items.getFirst().getId() );
        Assertions.assertEquals( "TSU001", items.getFirst().getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, items.getFirst().getUserDisplayName() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", items.getFirst().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER, items.getFirst().getUserRole() );
        Assertions.assertEquals( "TSA001", items.getFirst().getAcspNumber() );
        Assertions.assertEquals( "Toy Story", items.getFirst().getAcspName() );
        Assertions.assertEquals( "active", items.getFirst().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getFirst().getAddedAt() ), reduceTimestampResolution( items.getFirst().getAddedAt().toString() ) );
        Assertions.assertNull( items.getFirst().getAddedBy() );
        Assertions.assertNull( items.getFirst().getRemovedBy() );
        Assertions.assertNull( items.getFirst().getRemovedAt() );
        Assertions.assertEquals( DEFAULT_KIND, items.getFirst().getKind() );
        Assertions.assertEquals( "/TS001", items.getFirst().getLinks().getSelf() );

        Assertions.assertEquals( daos.getLast().getEtag(), items.getLast().getEtag() );
        Assertions.assertEquals( "TS002", items.getLast().getId() );
        Assertions.assertEquals( "TSU002", items.getLast().getUserId() );
        Assertions.assertEquals( "Woody", items.getLast().getUserDisplayName() );
        Assertions.assertEquals( "woody@toystory.com", items.getLast().getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, items.getLast().getUserRole() );
        Assertions.assertEquals( "TSA001", items.getLast().getAcspNumber() );
        Assertions.assertEquals( "Toy Story", items.getLast().getAcspName() );
        Assertions.assertEquals( "active", items.getLast().getAcspStatus().getValue() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getLast().getAddedAt() ), reduceTimestampResolution( items.getLast().getAddedAt().toString() ) );
        Assertions.assertEquals( "TSU001", items.getLast().getAddedBy() );
        Assertions.assertEquals( "TSU001", items.getLast().getRemovedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( daos.getLast().getRemovedAt() ), reduceTimestampResolution( items.getLast().getRemovedAt().toString() ) );
        Assertions.assertEquals( DEFAULT_KIND, items.getLast().getKind() );
        Assertions.assertEquals( "/TS002", items.getLast().getLinks().getSelf() );

        Assertions.assertEquals( "/acsp-members/acsps/TSA001?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 15, acspMembers.getItemsPerPage() );
        Assertions.assertEquals( 0, acspMembers.getPageNumber() );
        Assertions.assertEquals( 2, acspMembers.getTotalResults() );
        Assertions.assertEquals( 1, acspMembers.getTotalPages() );
    }

}
