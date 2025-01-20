package uk.gov.companieshouse.acsp.manage.users.rest;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserFindUserBasedOnEmailGet;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AccountsUserEndpointTest {


    @Mock
    private PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler;

    @Mock
    private PrivateAccountsUserFindUserBasedOnEmailGet privateAccountsUserFindUserBasedOnEmailGet;

    @Mock
    private PrivateAccountsUserUserGet privateAccountsUserUserGet;

    @InjectMocks
    private AccountsUserEndpoint accountsUserEndpoint;

    @Test
    void searchUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailsWithEmptyListOrNullElementOrMalformedEmailReturnsBadRequest() throws Exception {
        final var listWithNull = new ArrayList<String>( );
        listWithNull.add( null );

        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.searchUserDetails( List.of() ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class , () -> accountsUserEndpoint.searchUserDetails( listWithNull ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class , () -> accountsUserEndpoint.searchUserDetails( List.of( "xxx" ) ) );
    }

    @Test
    void searchUserDetailsFetchesSpecifiedUsers() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

        final var usersList = new UsersList();
        usersList.add( new User().userId("111") );
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();
        final var response = accountsUserEndpoint.searchUserDetails( List.of( "111" ) );

        Assertions.assertEquals( "111", response.getFirst().getUserId() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNoContent() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

        final var intendedResponse = new ApiResponse<>( 204, Map.of(), new UsersList() );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();
        final var response = accountsUserEndpoint.searchUserDetails( List.of( "666" ) );

        Assertions.assertTrue( response.isEmpty() );
    }

    @Test
    void getUserDetailsWithNullInputThrowsInternalServerException(){
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( null ) );
    }

    @Test
    void getUserDetailsWithMalformedInputReturnsBadRequest() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "$" ) );
    }

    @Test
    void getUserDetailsFetchesUser() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new User().userId( "111" ) );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserUserGet ).execute();
        final var response = accountsUserEndpoint.getUserDetails( "111" );

        Assertions.assertEquals( "111", response.getUserId() );
    }

    @Test
    void getUserDetailsWithNonexistentUserReturnsNotFound() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "666" ) );
    }

    @Test
    void createGetUserDetailsRequestWithNullInputThrowsInternaServerErrorException(){
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( null ) );
    }

    @Test
    void createGetUserDetailsRequestWithMalformedInputReturnsBadRequest() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "$" ) );
    }

    @Test
    void createGetUserDetailsRequestFetchesUser() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new User().userId( "111" ) );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserUserGet ).execute();
        final var response = accountsUserEndpoint.getUserDetails( "111" );

        Assertions.assertEquals( "111", response.getUserId() );
    }

    @Test
    void createGetUserDetailsRequestWithNonexistentUserReturnsNotFound() throws Exception {

        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "666" ) );
    }
    @Test
    void fetchUserDetailsWithMalformedInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "$" ) );
    }

    @Test
    void fetchUserDetailsWithNullInputReturnsInternalServerError() {
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( null ) );
    }


    @Test
    void fetchUserDetailsWithNonexistentUserReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "666" ) );
    }

    @Test
    void fetchUserDetailsReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> accountsUserEndpoint.getUserDetails( "111" ) );
    }

    @Test
    void fetchUserDetailsSuccessfullyFetchesUserData() throws ApiErrorResponseException, URIValidationException {
        final var user = new User().userId( "333" );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), user );
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        Mockito.doReturn( intendedResponse ).when( privateAccountsUserUserGet ).execute();
        final var response = accountsUserEndpoint.getUserDetails( "333" );

        Assertions.assertEquals( "333", response.getUserId() );
    }




    @Test
    void searchUserDetailsWithOneUserRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "bruce.wayne@gotham.city" );
        final var usersList = new UsersList();
        usersList.add( new User().userId( "111" ) );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertEquals( "111", accountsUserEndpoint.searchUserDetails( emails ).getFirst().getUserId() );
    }

    @Test
    void searchUserDetailsWithMultipleUsersRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "bruce.wayne@gotham.city", "harley.quinn@gotham.city" );
        final var usersList = new UsersList();
        usersList.addAll( List.of( new User().userId( "111" ), new User().userId( "333" ) ) );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertEquals( "111", accountsUserEndpoint.searchUserDetails( emails ).getFirst().getUserId() );
        Assertions.assertEquals( "333", accountsUserEndpoint.searchUserDetails( emails ).getLast().getUserId() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsEmptyList() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "the.void@space.com" );
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new UsersList() );
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );


        Mockito.doReturn( intendedResponse).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertTrue( accountsUserEndpoint.searchUserDetails( emails ).isEmpty());
    }


}
