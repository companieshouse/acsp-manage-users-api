package uk.gov.companieshouse.acsp.manage.users.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UsersServiceTest {

    @Mock
    private WebClient usersWebClient;

    @InjectMocks
    private UsersService usersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    public enum UriType {
        URI,
        STRING,
        FUNCTION
    }

    @BeforeEach
    void setup(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setXRequestId( request ).build() );
    }

    private void mockWebClientSuccessResponse( final String uri, final Mono<String> jsonResponse, UriType uriType ) {
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( usersWebClient ).get();

        switch (uriType) {
            case URI -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(URI.class));
            }
            case STRING -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
            }
            case FUNCTION -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(Function.class));
            }
        }
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchUserDetails( final String userId, UriType uriType ) throws JsonProcessingException {
        final var user = testDataManager.fetchUserDtos( userId ).getFirst();
        final var uri = String.format( "/users/%s", userId );
        final var jsonResponse = new ObjectMapper().writeValueAsString( user );
        mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ), uriType );
    }

    private void mockWebClientErrorResponse( final String uri, int responseCode, UriType uriType ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        switch (uriType) {
            case URI -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(URI.class));
            }
            case STRING -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
            }
            case FUNCTION -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(Function.class));
            }
        }

        Mockito.doReturn( requestHeadersUriSpec ).when( usersWebClient ).get();
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.error( new WebClientResponseException( responseCode, "Error", null, null, null ) ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchUserDetailsErrorResponse( final String userId, int responseCode, UriType uriType ){
        final var uri = String.format( "/users/%s", userId );
        mockWebClientErrorResponse( uri, responseCode, uriType );
    }

    private void mockWebClientJsonParsingError( final String uri, UriType uriType ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        switch (uriType) {
            case URI -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(URI.class));
            }
            case STRING -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
            }
            case FUNCTION -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(Function.class));
            }
        }

        Mockito.doReturn( requestHeadersUriSpec ).when( usersWebClient ).get();
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.just( "}{" ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchUserDetailsJsonParsingError( final String userId, UriType uriType ){
        final var uri = String.format( "/users/%s", userId );
        mockWebClientJsonParsingError( uri, uriType );
    }

    private void mockWebClientForSearchUserDetails( UriType uriType, final String... userIds ) throws JsonProcessingException {
        final var users = testDataManager.fetchUserDtos( userIds );
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", users.stream().map( User::getEmail ).toList() ) );
        final var jsonResponse = new ObjectMapper().writeValueAsString( users );
        mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ), uriType );
    }

    private void mockWebClientForSearchUserDetailsErrorResponse( final String userEmail, int responseCode, UriType uriType ){
        final var uri = String.format( "/users/search?user_email=%s", userEmail );
        mockWebClientErrorResponse( uri, responseCode, uriType );
    }

    private void mockWebClientForSearchUserDetailsNonexistentEmail( UriType uriType, String... emails ) {
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockWebClientSuccessResponse( uri, Mono.empty(), uriType );
    }

    private void mockWebClientForSearchUserDetailsJsonParsingError( UriType uriType, final String... emails ){
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockWebClientJsonParsingError( uri, uriType );
    }

    @Test
    void fetchUserDetailsForNullOrNonexistentUserReturnsNotFoundRuntimeException() {
        mockWebClientForFetchUserDetailsErrorResponse( null, 404, UriType.STRING );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( (String) null ) );

        mockWebClientForFetchUserDetailsErrorResponse( "404User", 404, UriType.STRING );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "404User" ) );
    }

    @Test
    void fetchUserDetailsWithMalformedUserIdReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchUserDetailsErrorResponse( "£$@123", 400, UriType.STRING );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "£$@123" ) );
    }

    @Test
    void fetchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchUserDetailsJsonParsingError( "WITU001", UriType.STRING );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "WITU001" ) );
    }

    @Test
    void fetchUserDetailsReturnsSpecifiedUser() throws JsonProcessingException {
        mockWebClientForFetchUserDetails( "WITU001", UriType.STRING );
        Assertions.assertEquals( "Geralt of Rivia", usersService.fetchUserDetails( "WITU001" ).getDisplayName() );
    }

    @Test
    void fetchUserDetailsWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.fetchUserDetails( (Stream<AcspMembersDao>) null ) );
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, usersService.fetchUserDetails( Stream.of() ).size() );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasNonExistentUserReturnsNotFoundRuntimeException(){
        final var membership = new AcspMembersDao();
        membership.setUserId( "404User" );
        mockWebClientForFetchUserDetailsErrorResponse( "404User", 404, UriType.STRING );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( membership ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamThatHasMalformedUserIdReturnsInternalServerErrorRuntimeException(){
        final var membership = new AcspMembersDao();
        membership.setUserId( "£$@123" );
        mockWebClientForFetchUserDetailsErrorResponse( "£$@123", 400, UriType.STRING );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( membership ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();
        mockWebClientForFetchUserDetailsJsonParsingError( "WITU001", UriType.STRING );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( Stream.of( membership ) ) );
    }

    @Test
    void fetchUserDetailsWithStreamReturnsMap() throws JsonProcessingException {
        final var membership = testDataManager.fetchAcspMembersDaos( "WIT001" ).getFirst();
        mockWebClientForFetchUserDetails( "WITU001", UriType.STRING );
        final var users = usersService.fetchUserDetails( Stream.of( membership, membership ) );

        Assertions.assertEquals( 1, users.size() );
        Assertions.assertTrue( users.containsKey( "WITU001" ) );
        Assertions.assertTrue( users.values().stream().map( User::getUserId ).toList().contains( "WITU001" ) );
    }

    @Test
    void searchUserDetailsWithNullListThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailWithNullOrMalformedUserEmailThrowsInternalServerErrorRuntimeException() {
        final var emails = new ArrayList<String>();
        emails.add( null );
        mockWebClientForSearchUserDetailsErrorResponse( null, 400, UriType.FUNCTION );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( emails ) );

        mockWebClientForSearchUserDetailsErrorResponse( "£$@123", 400, UriType.FUNCTION );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "£$@123" ) ) );
    }

    @Test
    void searchUserDetailsReturnsUsersList() throws JsonProcessingException {
        mockWebClientForSearchUserDetails( UriType.FUNCTION, "WITU001" );
        final var result = usersService.searchUserDetails( List.of( "geralt@witcher.com" ) );
        Assertions.assertEquals( 1, result.size() );
        Assertions.assertEquals( "Geralt of Rivia", result.getFirst().getDisplayName() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNull() {
        mockWebClientForSearchUserDetailsNonexistentEmail( UriType.FUNCTION, "404@email.com" );
        Assertions.assertNull( usersService.searchUserDetails( List.of( "404@email.com" ) ) );
    }

    @Test
    void searchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForSearchUserDetailsJsonParsingError( UriType.FUNCTION, "geralt@witcher.com" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "geralt@witcher.com" ) ) );
    }

}
