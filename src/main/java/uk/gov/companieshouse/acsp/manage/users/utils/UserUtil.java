package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getUser;

import java.util.Optional;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

public final class UserUtil {

    private UserUtil(){};

    public static boolean isRequestingUser( final AcspMembersDao targetMembership ){
        final var idMatches = Optional.ofNullable( targetMembership )
                .map( AcspMembersDao::getUserId )
                .filter( userId -> userId.equals( getEricIdentity() ) )
                .isPresent();

        final var emailMatch = Optional.ofNullable( targetMembership )
                .map( AcspMembersDao::getUserEmail )
                .filter( userEmail -> userEmail.equals( getUser().getEmail() ) )
                .isPresent();

        return idMatches || emailMatch;
    }

}
