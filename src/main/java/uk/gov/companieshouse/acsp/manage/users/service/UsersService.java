package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class UsersService {

  private final AccountsUserEndpoint accountsUserEndpoint;

  private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public UsersService( AccountsUserEndpoint accountsUserEndpoint ) {
    this.accountsUserEndpoint = accountsUserEndpoint;
  }

  private final String FAILED_TO_RETRIEVE_USER_DETAILS = "Failed to retrieve user details";

  public User fetchUserDetails( final String userId ) {

    final String xRequestId = getXRequestId();
    try {
      LOG.infoContext(xRequestId,
        String.format("Sending request to accounts-user-api: GET /users/{user_id}. Attempting to retrieve user: %s", userId), null);
      final var userApiResponse = accountsUserEndpoint.getUserDetails(userId);
      if(!userApiResponse.hasErrors()){
        return userApiResponse.getData();
      }
      throw new InternalServerErrorRuntimeException(String.format("Error from accounts-user-api: GET /users/{user_id}. for user: %s", userId));

    } catch ( ApiErrorResponseException exception ) {
      if ( exception.getStatusCode() == 404 ) {
        LOG.errorContext(xRequestId, String.format("Could not find user details for user with id %s", userId), exception, null);
        throw new NotFoundRuntimeException("acsp-manage-users-api", "Failed to find user");
      } else {
        LOG.errorContext(xRequestId, String.format("Failed to retrieve user details for user with id %s", userId), exception, null);
        throw new InternalServerErrorRuntimeException(FAILED_TO_RETRIEVE_USER_DETAILS);
      }
    } catch ( URIValidationException exception ) {
      LOG.errorContext(xRequestId, String.format("Failed to fetch user details for user %s, because uri was incorrectly formatted", userId),
        exception, null);
      throw new InternalServerErrorRuntimeException("Invalid uri for accounts-user-api service");
    } catch ( Exception exception ) {
      LOG.errorContext(getXRequestId(), String.format("Unexpected error while checking if user %s exists", userId), exception, null);
      throw new InternalServerErrorRuntimeException(FAILED_TO_RETRIEVE_USER_DETAILS);
    } finally {
      LOG.infoContext(xRequestId, String.format("Finished request to accounts-user-api for user: %s", userId), null);

    }
  }

  public boolean doesUserExist( final String userId ) {
    try {
      final var user = fetchUserDetails(userId);
      return Objects.nonNull(user);
    } catch ( NotFoundRuntimeException e ) {
      LOG.debugContext(getXRequestId(), String.format("User %s does not exist", userId), null);
      return false;
    } catch ( Exception e ) {
      LOG.errorContext(getXRequestId(), new Exception(String.format("Unexpected error while checking if user %s exists", userId)), null);
      throw e;
    }
  }


  public UsersList searchUserDetails( final List<String> emails ) {
    final String xRequestId = getXRequestId();
    try {
      LOG.debugContext(xRequestId,
        String.format("Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join(", ", emails)),
        null);
      return accountsUserEndpoint.searchUserDetails(emails)
        .getData();
    } catch ( ApiErrorResponseException exception ) {
      LOG.errorContext(xRequestId, FAILED_TO_RETRIEVE_USER_DETAILS, exception, null);
      throw new InternalServerErrorRuntimeException(FAILED_TO_RETRIEVE_USER_DETAILS);

    } catch ( URIValidationException exception ) {
      LOG.errorContext(getXRequestId(), new Exception(
          String.format("Search failed to fetch user details for users (%s), because uri was incorrectly formatted", String.join(", ", emails))),
        null);
      throw new InternalServerErrorRuntimeException("Invalid uri for accounts-user-api service");
    } catch ( Exception exception ) {
      LOG.errorContext(getXRequestId(),
        new Exception(String.format("Search failed to retrieve user details for: %s", String.join(", ", emails))),
        null);
      throw new InternalServerErrorRuntimeException("Search failed to retrieve user details");
    }

  }

  public Map<String, User> fetchUserDetails( final Stream<AcspMembersDao> acspMembers ) {

    return acspMembers.map(AcspMembersDao::getUserId)
      .distinct()
      .map(this::fetchUserDetails)
      .collect(Collectors.toMap(User::getUserId, user -> user));

  }

}
