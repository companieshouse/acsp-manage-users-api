package uk.gov.companieshouse.acsp.manage.users.model;

import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_TOKEN_PERMISSIONS;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class RequestContextData {

    private final String xRequestId;
    private final String ericIdentity;
    private final String ericIdentityType;
    private final List<String> ericAuthorisedRoles;
    private final String ericAuthorisedTokenPermissions;
    private User user;
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String UNKNOWN = "unknown";


    public RequestContextData( final HttpServletRequest request ){
        xRequestId = getRequestHeader( request, X_REQUEST_ID );
        ericIdentity = getRequestHeader( request, ERIC_IDENTITY );
        ericIdentityType = getRequestHeader( request, ERIC_IDENTITY_TYPE );
        ericAuthorisedRoles = getEricAuthorisedRoles( request );
        ericAuthorisedTokenPermissions = getRequestHeader( request, ERIC_AUTHORISED_TOKEN_PERMISSIONS );
    }

    private List<String> getEricAuthorisedRoles( final HttpServletRequest request ){
        return Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_ROLES ) )
                .map( roles -> roles.split( " " ) )
                .map( Arrays::asList )
                .orElse( List.of() );
    }

    public String getXRequestId(){
        return Optional.ofNullable( xRequestId ).orElse( UNKNOWN );
    }

    public String getEricIdentity(){
        return Optional.ofNullable( ericIdentity ).orElse( UNKNOWN );
    }

    public String getEricIdentityType(){
        return Optional.ofNullable( ericIdentityType ).orElse( UNKNOWN );
    }

    public List<String> getEricAuthorisedRoles(){
        return ericAuthorisedRoles;
    }

    public String getEricAuthorisedTokenPermissions(){
        return Optional.ofNullable( ericAuthorisedTokenPermissions ).orElse( "" );
    }

    public void setUser( final User user ){
        this.user = user;
    }

    public User getUser(){
        return user;
    }

}
