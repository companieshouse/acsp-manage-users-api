package uk.gov.companieshouse.acsp.manage.users.model;

import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public enum SpringRole {

    KEY_ROLE ( "KEY" ),
    BASIC_OAUTH_ROLE ( "BASIC_OAUTH" ),
    ACSP_OWNER_ROLE ( "ACSP_OWNER" ),
    ACSP_ADMIN_ROLE ( "ACSP_ADMIN" ),
    ACSP_STANDARD_ROLE ( "ACSP_STANDARD" ),
    ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE( "ADMIN_WITH_ACSP_SEARCH_PRIVILEGE" ),
    UNKNOWN_ROLE ( "UNKNOWN" );

    private final String value;

    SpringRole( final String springRole ){ value = springRole; }

    public String getValue(){
        return value;
    }

    public static String[] getValues( final SpringRole... springRoles ){
        final var roles = new String[ springRoles.length ];
        for ( int roleIndex = 0; roleIndex < springRoles.length; roleIndex++ ){
            roles[ roleIndex ] = springRoles[ roleIndex ].getValue();
        }
        return roles;
    }

    public static SpringRole fromUserRoleEnum( final UserRoleEnum role ){
        return switch ( role ){
            case OWNER -> ACSP_OWNER_ROLE;
            case ADMIN -> ACSP_ADMIN_ROLE;
            case STANDARD -> ACSP_STANDARD_ROLE;
        };
    }

}
