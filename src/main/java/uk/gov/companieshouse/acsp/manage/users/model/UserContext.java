package uk.gov.companieshouse.acsp.manage.users.model;

import uk.gov.companieshouse.api.accounts.user.model.User;

public class UserContext extends RequestContext<User> {

    private static final UserContext INSTANCE = new UserContext();

    private UserContext() {
        super();
    }

    public static UserContext getInstance() {
        return INSTANCE;
    }

}
