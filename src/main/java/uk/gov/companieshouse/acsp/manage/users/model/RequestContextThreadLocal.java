package uk.gov.companieshouse.acsp.manage.users.model;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class RequestContextThreadLocal extends ThreadLocal<RequestContextData> {

    private void createRequestContextDataIfNecessary(){
        if ( Objects.isNull( get() ) ) {
            set( new RequestContextData() );
        }
    }

    public void setRequestDetails( final HttpServletRequest request ){
        createRequestContextDataIfNecessary();
        get().setRequestContextData( request );
    }

    public void setLoggedUser( final User user ){
        createRequestContextDataIfNecessary();
        get().setUser( user );
    }

}
