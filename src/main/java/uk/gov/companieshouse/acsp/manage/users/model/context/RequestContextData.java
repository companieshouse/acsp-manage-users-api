package uk.gov.companieshouse.acsp.manage.users.model.context;

import static uk.gov.companieshouse.acsp.manage.users.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_TOKEN_PERMISSIONS;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public class RequestContextData {

    private final String xRequestId;
    private final String ericIdentity;
    private final String ericIdentityType;
    private final String ericAuthorisedKeyRoles;
    private final String activeAcspNumber;
    private final UserRoleEnum activeAcspRole;
    private final HashSet<String> adminPrivileges;
    private final User user;

    private static final String ACSP_MEMBERS_OWNERS = "acsp_members_owners=create,update,delete";
    private static final String ACSP_MEMBERS_ADMINS = "acsp_members_admins=create,update,delete";
    private static final String ACSP_MEMBERS_READ_PERMISSION = "acsp_members=read";
    private static final String X_REQUEST_ID = "X-Request-Id";

    protected RequestContextData( final String xRequestId, final String ericIdentity, final String ericIdentityType, final String ericAuthorisedKeyRoles, final String activeAcspNumber, final UserRoleEnum activeAcspRole, final HashSet<String> adminPrivileges, final User user ){
        this.xRequestId = xRequestId;
        this.ericIdentity = ericIdentity;
        this.ericIdentityType = ericIdentityType;
        this.ericAuthorisedKeyRoles = ericAuthorisedKeyRoles;
        this.activeAcspNumber = activeAcspNumber;
        this.activeAcspRole = activeAcspRole;
        this.adminPrivileges = adminPrivileges;
        this.user = user;
    }

    public String getXRequestId(){
        return xRequestId;
    }

    public String getEricIdentity(){
        return ericIdentity;
    }

    public String getEricIdentityType(){
        return ericIdentityType;
    }

    public String getEricAuthorisedKeyRoles(){
        return ericAuthorisedKeyRoles;
    }

    public String getActiveAcspNumber(){
        return activeAcspNumber;
    }

    public UserRoleEnum getActiveAcspRole(){
        return activeAcspRole;
    }

    public HashSet<String> getAdminPrivileges(){
        return adminPrivileges;
    }

    public User getUser(){
        return user;
    }

    public static final class RequestContextDataBuilder {
        private String xRequestId = UNKNOWN;
        private String ericIdentity = UNKNOWN;
        private String ericIdentityType = UNKNOWN;
        private String ericAuthorisedKeyRoles = UNKNOWN;
        private HashSet<String> adminPrivileges = new HashSet<>();
        private String activeAcspNumber = UNKNOWN;
        private UserRoleEnum activeAcspRole;
        private User user;

        private static final Pattern ACSP_NUMBER_PATTERN = Pattern.compile( "(?<=^|\\s)acsp_number=([0-9A-Za-z-_]{0,32})(?=\\s|$)" );

        public RequestContextDataBuilder setXRequestId( final HttpServletRequest request ){
            xRequestId = Optional.ofNullable( getRequestHeader( request, X_REQUEST_ID ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricIdentity( final HttpServletRequest request ){
            ericIdentity = Optional.ofNullable( getRequestHeader( request, ERIC_IDENTITY ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricIdentityType( final HttpServletRequest request ){
            ericIdentityType = Optional.ofNullable( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setEricAuthorisedKeyRoles( final HttpServletRequest request ){
            ericAuthorisedKeyRoles = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_KEY_ROLES ) ).orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setActiveAcspNumber( final HttpServletRequest request ){
            activeAcspNumber = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_TOKEN_PERMISSIONS ) )
                    .map( ACSP_NUMBER_PATTERN::matcher )
                    .map( matcher -> matcher.find() ? matcher.group( 1 ) : null )
                    .orElse( UNKNOWN );
            return this;
        }

        public RequestContextDataBuilder setActiveAcspRole( final HttpServletRequest request ){
            final var ericAuthorisedTokenPermissions = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_TOKEN_PERMISSIONS ) ).orElse( UNKNOWN );

            final var roleMap = new LinkedHashMap<String, UserRoleEnum>();
            roleMap.put( ACSP_MEMBERS_OWNERS, UserRoleEnum.OWNER );
            roleMap.put( ACSP_MEMBERS_ADMINS, UserRoleEnum.ADMIN );
            roleMap.put( ACSP_MEMBERS_READ_PERMISSION, UserRoleEnum.STANDARD );

            activeAcspRole = roleMap.entrySet().stream()
                    .filter( entry -> ericAuthorisedTokenPermissions.contains( entry.getKey() ) )
                    .map( Map.Entry::getValue )
                    .findFirst()
                    .orElse( null );

            return this;
        }

        public RequestContextDataBuilder setAdminPrivileges( final HttpServletRequest request ){
            adminPrivileges = Optional.ofNullable( getRequestHeader( request, ERIC_AUTHORISED_ROLES ) )
                    .map( roles -> roles.split(" ") )
                    .map( roles -> Arrays.stream( roles ).collect( Collectors.toCollection( HashSet::new ) ) )
                    .orElse( new HashSet<>() );
            return this;
        }

        public RequestContextDataBuilder setUser( final User user ){
            this.user = user;
            return this;
        }

        public RequestContextData build(){
            return new RequestContextData( xRequestId, ericIdentity, ericIdentityType, ericAuthorisedKeyRoles, activeAcspNumber, activeAcspRole, adminPrivileges, user );
        }

    }

}
