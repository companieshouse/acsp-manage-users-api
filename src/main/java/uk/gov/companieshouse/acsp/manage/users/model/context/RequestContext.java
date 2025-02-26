package uk.gov.companieshouse.acsp.manage.users.model.context;

import java.util.Objects;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;

public final class RequestContext {

    private static ThreadLocal<RequestContextData> requestContextDataThreadLocal;

    private RequestContext(){}

    public static void setRequestContext( final RequestContextData requestContext ){
        requestContextDataThreadLocal = new ThreadLocal<>();
        requestContextDataThreadLocal.set( requestContext );
    }

    public static RequestContextData getRequestContext(){
        return Objects.nonNull( requestContextDataThreadLocal ) ? requestContextDataThreadLocal.get() : new RequestContextDataBuilder().build();
    }

    public static void clear(){
        requestContextDataThreadLocal.remove();
    }

}
