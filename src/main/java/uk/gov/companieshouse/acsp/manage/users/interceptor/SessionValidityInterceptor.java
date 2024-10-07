package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.fetchRequestingUsersActiveAcspNumber;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.fetchRequestingUsersRole;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class SessionValidityInterceptor implements HandlerInterceptor  {

    private final AcspMembersService acspMembersService;

    private static final Logger LOGGER = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    public SessionValidityInterceptor( final AcspMembersService acspMembersService ) {
        this.acspMembersService = acspMembersService;
    }

    private boolean sessionIsValid( final String requestingUserId, final String requestingUsersActiveAcspNumber ){
        final var requestingUsersActiveMembershipOptional = acspMembersService.fetchActiveAcspMembership( requestingUserId, requestingUsersActiveAcspNumber );
        if ( requestingUsersActiveMembershipOptional.isEmpty() ){
            return false;
        }
        final var requestingUsersActiveMembership = requestingUsersActiveMembershipOptional.get();

        final var currentUserRole = UserRoleEnum.fromValue( requestingUsersActiveMembership.getUserRole() );
        final var sessionUserRole = fetchRequestingUsersRole();
        return currentUserRole.equals( sessionUserRole );
    }

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        if ( !isOAuth2Request() ) {
            return true;
        }

        final var requestingUserId = request.getHeader( ERIC_IDENTITY );
        final var requestingUsersActiveAcspNumber = fetchRequestingUsersActiveAcspNumber();
        if ( sessionIsValid( requestingUserId, requestingUsersActiveAcspNumber ) ){
            return true;
        }

        LOGGER.debugRequest( request, "Session is invalid", null );
        response.setStatus( 403 );
        return false;
    }

}