package uk.gov.companieshouse.acsp.manage.users.membership.controller;

import java.util.Set;
import java.util.function.Function;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.acsp.manage.users.ascpprofile.AcspProfileService;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.email.EmailService;
import uk.gov.companieshouse.acsp.manage.users.membership.MembershipService;
import uk.gov.companieshouse.acsp.manage.users.user.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.ExceptionUtil.invokeAndMapException;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.canCreateMembership;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.isActiveMemberOfAcsp;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.acsp.manage.users.common.model.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.acsp.manage.users.membership.model.ErrorCode.ERROR_CODE_1001;
import static uk.gov.companieshouse.acsp.manage.users.membership.model.ErrorCode.ERROR_CODE_1002;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.ADMIN;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.OWNER;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum.STANDARD;

@Controller
public class AcspMembershipsExternalController implements AcspMembershipsInterface {

    private final UsersService usersService;
    private final AcspProfileService acspProfileService;
    private final MembershipService acspMembersService;
    private final EmailService emailService;

    public AcspMembershipsExternalController( final UsersService usersService, final AcspProfileService acspProfileService, final MembershipService acspMembersService, final EmailService emailService ) {
        this.usersService = usersService;
        this.acspProfileService = acspProfileService;
        this.acspMembersService = acspMembersService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<AcspMembership> addMemberForAcsp( final String xRequestId, final String targetAcspNumber, final RequestBodyPost requestBody ) {
        final var targetUserId = requestBody.getUserId();
        final var targetUserRole = UserRoleEnum.fromValue( requestBody.getUserRole().getValue() );

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with acsp_number=%s, user_id=%s, user_role=%s ", targetAcspNumber, targetUserId, targetUserRole.getValue() ), null );

        final var targetUser = invokeAndMapException( (Function<String, User>) usersService::fetchUserDetails, NotFoundRuntimeException.class, new BadRequestRuntimeException( ERROR_CODE_1001.getCode(), new Exception( "Cannot find user" ) ) ).apply( targetUserId );
        final var targetAcspProfile = invokeAndMapException( acspProfileService::fetchAcspProfile, NotFoundRuntimeException.class, new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Cannot find Acsp" ) ) ).apply( targetAcspNumber );

        final var memberships = acspMembersService.fetchMembershipDaos( targetUserId, false );
        if ( !memberships.isEmpty() ) {
            throw new BadRequestRuntimeException( ERROR_CODE_1002.getCode(), new Exception( String.format( "%s user already has an active Acsp membership", targetUserId ) ) );
        }

        if ( isOAuth2Request() && ( !isActiveMemberOfAcsp( targetAcspNumber ) || !canCreateMembership( targetUserRole ) ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "User %s is not permitted to create %s membership", getEricIdentity(), targetUserRole.getValue() ) ) );
        }

        final var membership = acspMembersService.createMembership( targetUser, targetAcspProfile, targetUserRole, isOAuth2Request() ? getEricIdentity() : null );

        if ( isOAuth2Request() ){
            final var requestingUserDisplayName = Optional.ofNullable( getUser().getDisplayName() ).orElse( getUser().getEmail() );
            emailService.sendConfirmYouAreAMemberEmail( targetUser.getEmail(), requestingUserDisplayName, targetAcspProfile.getName(), targetUserRole ).subscribe();
        }

        return new ResponseEntity<>( membership, CREATED );
    }

    @Override
    public ResponseEntity<AcspMembershipsList> findMembershipsForUserAndAcsp( final String xRequestId, final String acspNumber, final Boolean includeRemoved, final RequestBodyLookup requestBody ) {
        final var userEmail = Optional
                .ofNullable( requestBody )
                .map( RequestBodyLookup::getUserEmail )
                .orElseThrow( () -> new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "User email was not provided." ) ) );

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with acsp_number=%s, include_removed=%s, user_email=%s", acspNumber, includeRemoved, userEmail ), null );

        final var user = Optional
                .ofNullable( usersService.searchUserDetails( List.of( userEmail ) ) )
                .filter( users -> !users.isEmpty() )
                .map( UsersList::getFirst )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "User %s was not found", userEmail ) ) ) );

        acspProfileService.fetchAcspProfile( acspNumber );

        final var memberships = acspMembersService.fetchMemberships( user, includeRemoved, acspNumber );

        return new ResponseEntity<>( memberships, OK );
    }

    @Override
    public ResponseEntity<AcspMembershipsList> getMembersForAcsp( final String acspNumber, final String xRequestId, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage, final String role ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with acsp_number=%s, include_removed=%b, page_index=%d, items_per_page=%d, role=%s", acspNumber, includeRemoved, pageIndex, itemsPerPage, role ), null );

        if ( Objects.nonNull( role ) && !Set.of( OWNER.getValue(), ADMIN.getValue(), STANDARD.getValue() ).contains( role ) ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Role was invalid: %s", role ) ) );
        }

        if ( pageIndex < 0 || itemsPerPage <= 0 ) {
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "pageIndex was less than 0 or itemsPerPage was less than or equal to 0" ) );
        }

        final var acspProfile = acspProfileService.fetchAcspProfile( acspNumber );
        final var acspMembershipsList = acspMembersService.fetchMembershipsForAcspNumberAndRole( acspProfile, role, includeRemoved, pageIndex, itemsPerPage );

        return new ResponseEntity<>( acspMembershipsList, OK );
    }

}
