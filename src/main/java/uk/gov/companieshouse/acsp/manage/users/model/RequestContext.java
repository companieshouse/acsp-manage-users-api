package uk.gov.companieshouse.acsp.manage.users.model;

public class RequestContext<T>{

    private final ThreadLocal<T> requestDetailsThreadLocal;

    RequestContext() {
        requestDetailsThreadLocal = new ThreadLocal<>();
    }

    public void setRequestDetails( final T requestDetails ) {
        requestDetailsThreadLocal.set(requestDetails);
    }

    public void clear() {
        requestDetailsThreadLocal.remove();
    }

    public T getRequestDetails() {
        return requestDetailsThreadLocal.get();
    }

}
