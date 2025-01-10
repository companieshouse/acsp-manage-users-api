package uk.gov.companieshouse.acsp.manage.users.rest;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;
import uk.gov.companieshouse.api.handler.acspprofile.request.PrivateAcspProfileAcspInfoGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AcspProfileEndpointTest {

    @Mock
    private ApiClientUtil apiClientUtil;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler;

    @Mock
    private PrivateAcspProfileAcspInfoGet privateAcspProfileAcspInfoGet;

    @InjectMocks
    private AcspProfileEndpoint acspProfileEndpoint;

    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField( acspProfileEndpoint, "apiUrl", "http://api.chs.local:4001" );
    }

    @Test
    void createGetAcspInfoRequestWithNoSuchElementException(){
        Assertions.assertThrows( NoSuchElementException.class, () -> acspProfileEndpoint.getAcspInfo( null ) );
    }

    @Test
    void createGetAcspInfoRequestWithMalformedAcspNumberThrowsUriValidationException() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientUtil ).getInternalApiClient( any() );
        Mockito.doReturn( privateAcspProfileResourceHandler ).when( internalApiClient ).privateAcspProfileResourceHandler();
        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new URIValidationException( "URI pattern does not match expected URI pattern for this resource." ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( URIValidationException.class, ()->acspProfileEndpoint.getAcspInfo( "£££" ) );
    }

    @Test
    void createGetAcspInfoRequestWithNonexistentAcspNumberReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientUtil ).getInternalApiClient( any() );
        Mockito.doReturn( privateAcspProfileResourceHandler ).when( internalApiClient ).privateAcspProfileResourceHandler();
        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, ()->acspProfileEndpoint.getAcspInfo("TSA001") );
    }


    @Test
    void getAcspInfoWithNullInputThrowsNoSuchElementExceptionException(){
        Assertions.assertThrows( NoSuchElementException.class, () -> acspProfileEndpoint.getAcspInfo( null ) );
    }

    @Test
    void getAcspInfoWithMalformedAcspNumberThrowsUriValidationException() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientUtil ).getInternalApiClient( any() );
        Mockito.doReturn( privateAcspProfileResourceHandler ).when( internalApiClient ).privateAcspProfileResourceHandler();
        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new URIValidationException( "URI pattern does not match expected URI pattern for this resource." ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( URIValidationException.class, () -> acspProfileEndpoint.getAcspInfo( "£££" ) );
    }

    @Test
    void getAcspInfoRequestWithNonexistentAcspNumberReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientUtil ).getInternalApiClient( any() );
        Mockito.doReturn( privateAcspProfileResourceHandler ).when( internalApiClient ).privateAcspProfileResourceHandler();
        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> acspProfileEndpoint.getAcspInfo( "TSA001" ) );
    }

    @Test
    void getAcspInfoRequestRetrievesAcspInfo() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientUtil ).getInternalApiClient( any() );
        Mockito.doReturn( privateAcspProfileResourceHandler ).when( internalApiClient ).privateAcspProfileResourceHandler();
        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new AcspProfile() );
        Mockito.doReturn( intendedResponse ).when( privateAcspProfileAcspInfoGet ).execute();
        final var response = acspProfileEndpoint.getAcspInfo( "TSA001" );

        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( new AcspProfile(), response.getData() );
    }

}
