package uk.gov.companieshouse.acsp.manage.users.service;

import static org.mockito.ArgumentMatchers.any;

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
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UsersServiceTest {

    @Mock
    private AccountsUserEndpoint accountsUserEndpoint;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @InjectMocks
    private UsersService usersService;

    @Test
    void doesUserExist_UserExists_ReturnsTrue(){
        final var userDto = testDataManager.fetchUserDtos( "TSU001" ).getFirst();
        Mockito.doReturn( userDto ).when( accountsUserEndpoint ).getUserDetails( "TSU001" );
        Assertions.assertTrue( usersService.doesUserExist( "TSU001" ) );
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

        Mockito.doReturn( user ).when( accountsUserEndpoint ).getUserDetails( any() );

        Assertions.assertEquals( Map.of( "TSU001", user ), usersService.fetchUserDetails( acspMembers.stream() ) );
    }

}
