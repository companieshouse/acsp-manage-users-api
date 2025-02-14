package uk.gov.companieshouse.acsp.manage.users.model;

public class RequestDataContext extends RequestContext<RequestDetails> {

    private static final RequestDataContext INSTANCE = new RequestDataContext();

    private RequestDataContext() {
        super();
    }

    public static RequestDataContext getInstance() {
        return INSTANCE;
    }

}
