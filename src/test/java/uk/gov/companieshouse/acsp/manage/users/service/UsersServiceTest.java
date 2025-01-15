package uk.gov.companieshouse.acsp.manage.users.service;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UsersServiceTest {

    @Mock
    private AccountsUserEndpoint accountsUserEndpoint;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @InjectMocks
    private UsersService usersService;

    @Test
    void fetchUserDetailsWithNullInputReturnsInternalServerError() {
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( (String) null ) );
    }

    @Test
    void fetchUserDetailsWithMalformedInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( accountsUserEndpoint ).getUserDetails(any());
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "$" ) );
    }

    @Test
    void fetchUserDetailsWithNonexistentUserReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not found", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).getUserDetails( any() );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "666" ) );
    }

    @Test
    void fetchUserDetailsReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).getUserDetails( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "111" ) );
    }

    @Test
    void fetchUserDetailsSuccessfullyFetchesUserData() throws ApiErrorResponseException, URIValidationException {
        final var user = new User().userId( "333" );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), user );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).getUserDetails( any() );
        final var response = usersService.fetchUserDetails( "333" );

        Assertions.assertEquals( "333", response.getUserId() );
    }


    @Test
    void searchUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> usersService.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailsWithEmptyListOrListContainingNullOrMalformedEmailReturnInternalServerError() throws ApiErrorResponseException, URIValidationException {
        final var nullList = new ArrayList<String>();
        nullList.add( null );

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad input given", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).searchUserDetails( any() );
        final var emptyList = new ArrayList<String>();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( emptyList ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( nullList ) );

        final var emailList = List.of( "$$$" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( emailList) );
    }

    @Test
    void searchUserDetailsWithOneUserRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "bruce.wayne@gotham.city" );
        final var usersList = new UsersList();
        usersList.add( new User().userId( "111" ) );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).searchUserDetails( emails );

        Assertions.assertEquals( "111", usersService.searchUserDetails( emails ).getFirst().getUserId() );
    }

    @Test
    void searchUserDetailsWithMultipleUsersRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "bruce.wayne@gotham.city", "harley.quinn@gotham.city" );
        final var usersList = new UsersList();
        usersList.addAll( List.of( new User().userId( "111" ), new User().userId( "333" ) ) );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).searchUserDetails( emails );

        Assertions.assertEquals( "111", usersService.searchUserDetails( emails ).getFirst().getUserId() );
        Assertions.assertEquals( "333", usersService.searchUserDetails( emails ).getLast().getUserId() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsEmptyList() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "the.void@space.com" );

        final var intendedResponse = new ApiResponse<>( 204, Map.of(), new UsersList() );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).searchUserDetails( emails );

        Assertions.assertTrue( usersService.searchUserDetails( emails ).isEmpty());
    }

    @Test
    void searchUserDetailsWithIncorrectlyFormattedUriThrowsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        final var emails = List.of( "" );
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( accountsUserEndpoint ).searchUserDetails( emails );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( emails ) );
    }

    @Test
    void doesUserExist_UserExists_ReturnsTrue() throws Exception {
        final var userDto = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
        Mockito.doReturn( new ApiResponse<>( 200, Map.of(), userDto ) ).when( accountsUserEndpoint ).getUserDetails( "TSU001" );
        Assertions.assertTrue( usersService.doesUserExist( "TSU001" ) );
    }

    @Test
    void doesUserExist_UserDoesNotExist_ReturnsFalse() throws Exception {
        Mockito.doThrow( new ApiErrorResponseException( new Builder(404, "Not found", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).getUserDetails( "TSU001" );
        Assertions.assertFalse( usersService.doesUserExist( "TSU001" ) );
    }

    @Test
    void doesUserExist_OtherException_Rethrows() throws Exception {
        Mockito.doThrow( new InternalServerErrorRuntimeException("Unexpected error") ).when( accountsUserEndpoint ).getUserDetails( "TSU001" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.doesUserExist( "TSU001" ) );
    }

    @Test
    void fetchUserDetailsWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> usersService.fetchUserDetails( (Stream<AcspMembersDao>) null ) );
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap(){
        Assertions.assertEquals( Map.of(), usersService.fetchUserDetails( Stream.of() ) );
    }

    @Test
    void fetchUserDetailsRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var acspMembers = testDataManager.fetchAcspMembersDaos( "TS001", "NF001" );
        final var user = testDataManager.fetchUserDtos( "TSU001" ).getFirst();

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), user );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).getUserDetails( any() );

        Assertions.assertEquals( Map.of( "TSU001", user ), usersService.fetchUserDetails( acspMembers.stream() ) );
    }

}
