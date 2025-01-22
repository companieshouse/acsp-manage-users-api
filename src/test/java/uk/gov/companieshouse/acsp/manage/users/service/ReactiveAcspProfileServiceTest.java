package uk.gov.companieshouse.acsp.manage.users.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class ReactiveAcspProfileServiceTest {

    @Mock
    private WebClient acspWebClient;

    @InjectMocks
    private ReactiveAcspProfileService reactiveAcspProfileService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockWebClientSuccessResponse( final String uri, final Mono<String> jsonResponse ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( acspWebClient ).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchAcspProfile( final String acspNumber ) throws JsonProcessingException {
        final var acsp = testDataManager.fetchAcspProfiles( acspNumber ).getFirst();
        final var uri = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
        final var jsonResponse = new ObjectMapper().writeValueAsString( acsp );
        mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ) );
    }

    private void mockWebClientErrorResponse( final String uri, int responseCode ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( acspWebClient ).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.error( new WebClientResponseException( responseCode, "Error", null, null, null ) ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchAcspProfileErrorResponse( final String acspNumber, int responseCode ){
        final var uri = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
        mockWebClientErrorResponse( uri, responseCode );
    }

    private void mockWebClientJsonParsingError( final String uri ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( acspWebClient ).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.just( "}{" ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchAcspProfileJsonParsingError( final String acspNumber ){
        final var uri = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
        mockWebClientJsonParsingError( uri );
    }

    @Test
    void fetchAcspProfileForNullOrMalformedOrNonexistentAcspReturnsNotFoundRuntimeException() {
        mockWebClientForFetchAcspProfileErrorResponse( null, 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfile( null ) );

        mockWebClientForFetchAcspProfileErrorResponse( "!@£", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfile( "!@£" ) );

        mockWebClientForFetchAcspProfileErrorResponse( "404Acsp", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfile( "404Acsp" ) );
    }

    @Test
    void fetchAcspProfileWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchAcspProfileJsonParsingError( "WITA001" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfile( "WITA001" ) );
    }

    @Test
    void fetchAcspProfileReturnsSpecifiedAcsp() throws JsonProcessingException {
        mockWebClientForFetchAcspProfile( "WITA001" );
        Assertions.assertEquals( "Witcher", reactiveAcspProfileService.fetchAcspProfile( "WITA001" ).getName() );
    }

    @Test
    void fetchAcspProfilesWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> reactiveAcspProfileService.fetchAcspProfiles( null ) );
    }

    @Test
    void fetchAcspProfilesWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, reactiveAcspProfileService.fetchAcspProfiles( Stream.of() ).size() );
    }

    @Test
    void fetchAcspProfilesWithStreamThatHasNonExistentAcspReturnsNotFoundRuntimeException(){
        final var membership = new AcspMembersDao();
        membership.setAcspNumber( "404Acsp" );
        mockWebClientForFetchAcspProfileErrorResponse( "404Acsp", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfiles( Stream.of( membership ) ) );
    }

    @Test
    void fetchAcspProfilesWithStreamThatHasMalformedAcspNumberReturnsInternalServerErrorRuntimeException(){
        final var membership = new AcspMembersDao();
        membership.setAcspNumber( "£$@123" );
        mockWebClientForFetchAcspProfileErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfiles( Stream.of( membership ) ) );
    }

    @Test
    void fetchAcspProfilesWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();
        mockWebClientForFetchAcspProfileJsonParsingError( "WITA001" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> reactiveAcspProfileService.fetchAcspProfiles( Stream.of( membership ) ) );
    }

    @Test
    void fetchAcspProfilesWithStreamReturnsMap() throws JsonProcessingException {
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();
        mockWebClientForFetchAcspProfile( "WITA001" );
        final var acsps = reactiveAcspProfileService.fetchAcspProfiles( Stream.of( membership, membership ) );

        Assertions.assertEquals( 1, acsps.size() );
        Assertions.assertTrue( acsps.containsKey( "WITA001" ) );
        Assertions.assertTrue( acsps.values().stream().map( AcspProfile::getNumber ).toList().contains( "WITA001" ) );
    }

}
