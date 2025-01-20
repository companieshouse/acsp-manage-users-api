package uk.gov.companieshouse.acsp.manage.users.rest;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
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
    private PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler;

    @Mock
    private PrivateAcspProfileAcspInfoGet privateAcspProfileAcspInfoGet;

    @InjectMocks
    private AcspProfileEndpoint acspProfileEndpoint;



    @Test
    void getAcspInfoWithNullInputThrowsInternalException(){
        Mockito.doThrow(NullPointerException.class).when(privateAcspProfileResourceHandler).getAcspInfo( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileEndpoint.getAcspInfo( null ) );
    }

    @Test
    void getAcspInfoWithMalformedAcspNumberThrowsUriValidationException() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new URIValidationException( "URI pattern does not match expected URI pattern for this resource." ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> acspProfileEndpoint.getAcspInfo( "£££" ) );
    }

    @Test
    void getAcspInfoRequestWithNonexistentAcspNumberReturnsNotFound() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAcspProfileAcspInfoGet ).execute();

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> acspProfileEndpoint.getAcspInfo( "TSA001" ) );
    }

    @Test
    void getAcspInfoRequestRetrievesAcspInfo() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAcspProfileAcspInfoGet ).when( privateAcspProfileResourceHandler ).getAcspInfo( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new AcspProfile() );
        Mockito.doReturn( intendedResponse ).when( privateAcspProfileAcspInfoGet ).execute();
        final var response = acspProfileEndpoint.getAcspInfo( "TSA001" );
        Assertions.assertEquals( new AcspProfile(), response );
    }



}
