package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.rest.AcspProfileEndpoint;
import uk.gov.companieshouse.api.acspprofile.Status;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspProfileServiceTest {

    @Mock
    private AcspProfileEndpoint acspProfileEndpoint;

    @InjectMocks
    private AcspProfileService acspProfileService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();


    @Test
    void fetchAcspProfileRetrievesAcspProfile() throws ApiErrorResponseException, URIValidationException {
        final var acspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

        Mockito.doReturn( acspProfile ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        final var response = acspProfileService.fetchAcspProfile( "TSA001" );
        Assertions.assertEquals( "TSA001", response.getNumber() );
        Assertions.assertEquals( "Toy Story", response.getName() );
        Assertions.assertEquals( Status.ACTIVE, response.getStatus() );
    }

    @Test
    void fetchAcspProfilesWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> acspProfileService.fetchAcspProfiles( null ) );
    }

    @Test
    void fetchAcspProfilesWithEmptyStreamReturnsEmptyMap(){
        Assertions.assertEquals( Map.of(), acspProfileService.fetchAcspProfiles( Stream.of() ) );
    }

    @Test
    void fetchAcspProfilesRetrievesAcspProfiles() throws ApiErrorResponseException, URIValidationException {
        final var acspMembers = testDataManager.fetchAcspMembersDaos( "TS001", "TS002" );
        final var acspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

        Mockito.doReturn( acspProfile ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertEquals( Map.of( "TSA001", acspProfile ), acspProfileService.fetchAcspProfiles( acspMembers.stream() ) );
    }

}
