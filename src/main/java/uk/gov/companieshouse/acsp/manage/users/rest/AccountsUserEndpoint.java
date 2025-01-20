package uk.gov.companieshouse.acsp.manage.users.rest;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AccountsUserEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final String FAILED_TO_RETRIEVE_USER_DETAILS = "Failed to retrieve user details";


    private final PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler;

    @Autowired
    public AccountsUserEndpoint(PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler) {
        this.privateAccountsUserResourceHandler = privateAccountsUserResourceHandler;
    }

    public UsersList searchUserDetails(final List<String> emails)  {
        final var xRequestId = getXRequestId();
        final var searchUserDetailsUrl = "/users/search";
      try {
        LOG.infoContext(xRequestId,
          String.format("Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join(", ", emails)),
          null);
        return privateAccountsUserResourceHandler
                .searchUserDetails(searchUserDetailsUrl, emails)
                .execute()
                .getData();
      }  catch ( ApiErrorResponseException exception ) {
          LOG.errorContext(xRequestId, FAILED_TO_RETRIEVE_USER_DETAILS, exception, null);
          throw new InternalServerErrorRuntimeException(FAILED_TO_RETRIEVE_USER_DETAILS);

      } catch ( URIValidationException exception ) {
          LOG.errorContext(xRequestId, new Exception(
              String.format("Search failed to fetch user details for users (%s), because uri was incorrectly formatted", String.join(", ", emails))),
            null);
          throw new InternalServerErrorRuntimeException("Invalid uri for accounts-user-api service");
      } finally {
        LOG.infoContext(xRequestId, String.format("Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join(", ", emails)),
          null);
      }
    }


    public User getUserDetails(final String userId) {
        final var xRequestId = getXRequestId();
        final var getUserDetailsUrl = String.format("/users/%s", userId);
      try {
        LOG.infoContext(xRequestId,
          String.format("Sending request to accounts-user-api: GET /users/{user_id}. Attempting to retrieve user: %s", userId), null);
        return privateAccountsUserResourceHandler
                .getUserDetails(getUserDetailsUrl)
                .execute()
                .getData();
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
        LOG.errorContext(xRequestId, String.format("Failed to fetch user details for user %s", userId),
          exception, null);
        throw new InternalServerErrorRuntimeException(FAILED_TO_RETRIEVE_USER_DETAILS);
      }

      finally {
        LOG.infoContext(xRequestId, String.format("Finished request to accounts-user-api for user: %s", userId), null);

      }
    }

}
