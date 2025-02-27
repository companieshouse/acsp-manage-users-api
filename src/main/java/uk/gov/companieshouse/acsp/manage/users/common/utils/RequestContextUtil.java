package uk.gov.companieshouse.acsp.manage.users.common.utils;

import static uk.gov.companieshouse.acsp.manage.users.common.model.context.RequestContext.getRequestContext;
import static uk.gov.companieshouse.acsp.manage.users.common.model.Constants.OAUTH2;
import static uk.gov.companieshouse.acsp.manage.users.common.model.Constants.UNKNOWN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;
import uk.gov.companieshouse.acsp.manage.users.common.model.context.RequestContextData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public final class RequestContextUtil {

    private RequestContextUtil(){}

    private static <T> T getFieldFromRequestContext( final Function<RequestContextData, T> getterMethod, final T defaultValue ){
        return Optional.ofNullable( getRequestContext() ).map( getterMethod ).orElse( defaultValue );
    }

    public static String getXRequestId(){
        return getFieldFromRequestContext( RequestContextData::getXRequestId, UNKNOWN );
    }

    public static String getEricIdentity(){
        return getFieldFromRequestContext( RequestContextData::getEricIdentity, UNKNOWN );
    }

    public static String getEricIdentityType(){
        return getFieldFromRequestContext( RequestContextData::getEricIdentityType, UNKNOWN );
    }

    public static String getEricAuthorisedKeyRoles(){
        return getFieldFromRequestContext( RequestContextData::getEricAuthorisedKeyRoles, UNKNOWN );
    }

    public static String getActiveAcspNumber(){
        return getFieldFromRequestContext( RequestContextData::getActiveAcspNumber, UNKNOWN );
    }

    public static UserRoleEnum getActiveAcspRole(){
        return getFieldFromRequestContext( RequestContextData::getActiveAcspRole, null );
    }

    public static HashSet<String> getAdminPrivileges(){
        return getFieldFromRequestContext( RequestContextData::getAdminPrivileges, new HashSet<>() );
    }

    public static User getUser(){
        return getFieldFromRequestContext( RequestContextData::getUser, null );
    }

    public static boolean isOAuth2Request(){
        return getEricIdentityType().equals( OAUTH2 );
    }

    public static boolean isActiveMemberOfAcsp( final String acspNumber ){
        return getActiveAcspNumber().equals( acspNumber );
    }

    private static boolean canManageMembership( final UserRoleEnum role ){
        return switch ( role ){
            case OWNER -> OWNER.equals( getActiveAcspRole() );
            case ADMIN, STANDARD -> OWNER.equals( getActiveAcspRole() ) || ADMIN.equals( getActiveAcspRole() );
        };
    }

    public static boolean canCreateMembership( final UserRoleEnum role ){
        return canManageMembership( role );
    }

    public static boolean canRemoveMembership( final UserRoleEnum role ){
        return canManageMembership( role );
    }

    public static boolean canChangeRole( final UserRoleEnum from, final UserRoleEnum to ) {
        return canManageMembership( from ) && canManageMembership( to );
    }

    }
