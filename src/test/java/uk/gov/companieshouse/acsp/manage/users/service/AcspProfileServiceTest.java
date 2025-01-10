package uk.gov.companieshouse.acsp.manage.users.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.rest.AcspProfileEndpoint;
import uk.gov.companieshouse.api.acspprofile.Status;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspprofile.request.PrivateAcspProfileAcspInfoGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspProfileServiceTest {

    @Mock
    private AcspProfileEndpoint acspProfileEndpoint;

    @InjectMocks
    private AcspProfileService acspProfileService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void fetchAcspProfileRequestWithNullInputThrowsInternalServerException() {
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileService.fetchAcspProfile( null ) );
    }

    @Test
    void createFetchAcspProfileRequestWithMalformedAcspNumberThrowsInternalServerErrorRuntimeException() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new URIValidationException( "acspNumber was malformed" ) ).when( acspProfileEndpoint ).getAcspInfo("£££");

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, ()->acspProfileService.fetchAcspProfile( "£££" ) );
    }

    @Test
    void createFetchAcspProfileRequestWithNonexistentAcspNumberReturnsNotFoundRuntimeException() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );
        Assertions.assertThrows( NotFoundRuntimeException.class, ()->acspProfileService.fetchAcspProfile( "TSA001" ) );
    }

    @Test
    void createFetchAcspProfileRequestThrowsInternalServerErrorRuntimeExceptionWhenRetrievalFails() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something went wrong", new HttpHeaders() ) ) ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class,()-> acspProfileService.fetchAcspProfile( "TSA001" ) );
    }

    @Test
    void createFetchAcspProfileRequestThrowsInternalServerErrorRuntimeExceptionWhenSomethingUnexpectedHappens() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new NullPointerException() ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileService.fetchAcspProfile( "TSA001" ) );

    }

    @Test
    void createFetchAcspProfileRequestRetrievesAcspProfile() throws ApiErrorResponseException, URIValidationException {
        final var acspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), acspProfile );
        Mockito.doReturn( intendedResponse ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );;

        final var response = acspProfileService.fetchAcspProfile( "TSA001" );
        Assertions.assertEquals( "TSA001", response.getNumber() );
        Assertions.assertEquals( "Toy Story", response.getName() );
        Assertions.assertEquals( Status.ACTIVE, response.getStatus() );
    }


    @Test
    void fetchAcspProfileWithNonexistentAcspNumberReturnsNotFoundRuntimeException() throws ApiErrorResponseException, URIValidationException {

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> acspProfileService.fetchAcspProfile( "TSA001" ) );
    }

    @Test
    void fetchAcspProfileThrowsInternalServerErrorRuntimeExceptionWhenRetrievalFails() throws ApiErrorResponseException, URIValidationException {

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something went wrong", new HttpHeaders() ) ) ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );;

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileService.fetchAcspProfile( "TSA001" ) );
    }

    @Test
    void fetchAcspProfileThrowsInternalServerErrorRuntimeExceptionWhenSomethingUnexpectedHappens() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new NullPointerException() ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileService.fetchAcspProfile( "TSA001" ) );
    }

    @Test
    void fetchAcspProfileRetrievesAcspProfile() throws ApiErrorResponseException, URIValidationException {
        final var acspProfile = testDataManager.fetchAcspProfiles( "TSA001" ).getFirst();

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), acspProfile );
        Mockito.doReturn( intendedResponse ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

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

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), acspProfile );
        Mockito.doReturn( intendedResponse ).when( acspProfileEndpoint ).getAcspInfo( "TSA001" );

        Assertions.assertEquals( Map.of( "TSA001", acspProfile ), acspProfileService.fetchAcspProfiles( acspMembers.stream() ) );
    }

}
