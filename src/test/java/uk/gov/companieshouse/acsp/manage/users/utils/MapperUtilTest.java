package uk.gov.companieshouse.acsp.manage.users.utils;

import org.junit.jupiter.api.Assertions;
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
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
public class MapperUtilTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @InjectMocks
    private MapperUtil mapperUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @Test
    void enrichAcspMembershipWithUserDetailsWithNullInputReturnsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> mapperUtil.enrichAcspMembershipWithUserDetails( null ) );
    }

    @Test
    void enrichAcspMembershipWithUserDetailsWithoutDisplayNameCorrectlyEnriches(){
        final var userData = testDataManager.fetchUserDtos( "TSU001" ).getFirst();

        Mockito.doReturn( userData ).when( usersService ).fetchUserDetails( "TSU001" );

        final var acspMembership = new AcspMembership().userId( "TSU001" );
        mapperUtil.enrichAcspMembershipWithUserDetails( acspMembership );

        Assertions.assertEquals( "TSU001", acspMembership.getUserId() );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName() );
    }

    @Test
    void enrichAcspMembershipWithUserDetailsWithDisplayNameCorrectlyEnriches(){
        final var userData = testDataManager.fetchUserDtos( "TSU002" ).getFirst();

        Mockito.doReturn( userData ).when( usersService ).fetchUserDetails( "TSU002" );

        final var acspMembership = new AcspMembership().userId( "TSU002" );
        mapperUtil.enrichAcspMembershipWithUserDetails( acspMembership );

        Assertions.assertEquals( "TSU002", acspMembership.getUserId() );
        Assertions.assertEquals( "woody@toystory.com", acspMembership.getUserEmail() );
        Assertions.assertEquals( "Woody", acspMembership.getUserDisplayName() );
    }

    @Test
    void enrichAcspMembershipWithAcspDataWillNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> mapperUtil.enrichAcspMembershipWithAcspData( null ) );
    }

    @Test
    void enrichAcspMembershipWithAcspDataEnrichesMembership(){
        final var acspData = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();

        Mockito.doReturn( acspData ).when( acspDataService ).fetchAcspData( "TSA001" );

        final var acspMembership = new AcspMembership().acspNumber( "TSA001" );
        mapperUtil.enrichAcspMembershipWithAcspData( acspMembership );

        Assertions.assertEquals( "TSA001", acspMembership.getAcspNumber() );
        Assertions.assertEquals( "Toy Story", acspMembership.getAcspName() );
        Assertions.assertEquals( "active", acspMembership.getAcspStatus().getValue() );

    }

}
