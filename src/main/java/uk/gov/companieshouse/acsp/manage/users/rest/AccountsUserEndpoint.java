package uk.gov.companieshouse.acsp.manage.users.rest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AccountsUserEndpoint {

    private final PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler;

    @Autowired
    public AccountsUserEndpoint(PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler) {
        this.privateAccountsUserResourceHandler = privateAccountsUserResourceHandler;
    }

    public ApiResponse<UsersList> searchUserDetails(final List<String> emails) throws ApiErrorResponseException, URIValidationException {
        final var searchUserDetailsUrl = "/users/search";
        return privateAccountsUserResourceHandler
                .searchUserDetails(searchUserDetailsUrl, emails)
                .execute();
    }

    public ApiResponse<User> getUserDetails(final String userId) throws ApiErrorResponseException, URIValidationException {
        final var getUserDetailsUrl = String.format("/users/%s", userId);
        return privateAccountsUserResourceHandler
                .getUserDetails(getUserDetailsUrl).execute();
    }

}
