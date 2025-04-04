package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum.ACTIVE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipCollectionMappers;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

@Service
public class AcspMembersService {

    private final AcspMembersRepository acspMembersRepository;
    private final AcspMembershipCollectionMappers acspMembershipCollectionMappers;

    public AcspMembersService( final AcspMembersRepository acspMembersRepository, final AcspMembershipCollectionMappers acspMembershipCollectionMappers ) {
        this.acspMembersRepository = acspMembersRepository;
        this.acspMembershipCollectionMappers = acspMembershipCollectionMappers;
    }

    @Transactional( readOnly = true )
    public Optional<AcspMembersDao> fetchMembershipDao( final String membershipId ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch membership for id: %s", membershipId ), null );
        final var membership = acspMembersRepository.findById( membershipId );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched membership for id: %s", membershipId ), null );
        return membership;
    }

    @Transactional( readOnly = true )
    public Optional<AcspMembership> fetchMembership( final String membershipId ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch membership for id: %s", membershipId ), null );
        final var membership = acspMembersRepository.findById( membershipId ).map( dao -> acspMembershipCollectionMappers.daoToDto( dao, null, null ) );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched membership with id: %s", membershipId ), null );
        return membership;
    }

    @Transactional( readOnly = true )
    public List<AcspMembersDao> fetchMembershipDaos( final String userId, final boolean includeRemoved ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch memberships for user with id %s", userId ), null );
        final var memberships = includeRemoved ? acspMembersRepository.fetchActiveAndRemovedMembershipsForUserId( userId ) : acspMembersRepository.fetchActiveMembershipForUserId( userId ).map( List::of ).orElse( List.of() );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched memberships for user with id %s", userId ), null );
        return memberships;
    }

    @Transactional( readOnly = true )
    public AcspMembershipsList fetchMemberships( final User user, final boolean includeRemoved, final String acspNumber ) {
        final var loggingAcspNumber = Objects.nonNull( acspNumber ) ? String.format( " and Acsp %s", acspNumber ) : "";
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch memberships for user %s%s", Optional.of( user ).orElseThrow( () -> new NullPointerException( "User cannot be null" ) ).getEmail(), loggingAcspNumber ), null );

        final var membershipDaos = Optional
                .ofNullable( acspNumber )
                .map( any -> includeRemoved ? acspMembersRepository.fetchActiveAndRemovedMemberships( user.getUserId(), acspNumber ) : acspMembersRepository.fetchActiveMembership( user.getUserId(), acspNumber ).stream().toList() )
                .orElseGet( () -> includeRemoved ? acspMembersRepository.fetchActiveAndRemovedMembershipsForUserId( user.getUserId() ) : acspMembersRepository.fetchActiveMembershipForUserId( user.getUserId() ).map( List::of ).orElse( List.of() ) );

        final var memberships = acspMembershipCollectionMappers.daoToDto( membershipDaos, user, null );

        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched memberships for user %s%s", user.getEmail(), loggingAcspNumber ), null );
        return new AcspMembershipsList().items( memberships );
    }

    @Transactional( readOnly = true )
    public AcspMembershipsList fetchMembershipsForAcspNumberAndRole( final AcspProfile acspProfile, final String userRole, final boolean includeRemoved, final int pageIndex, final int itemsPerPage ) {
        LOGGER.debugContext( getXRequestId(), "Attempting to fetch memberships", null );

        final var pageable = PageRequest.of( pageIndex, itemsPerPage );
        final var membershipDaos = Optional
                .ofNullable( userRole )
                .map( role -> includeRemoved ? acspMembersRepository.fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole( acspProfile.getNumber(), role, pageable ) : acspMembersRepository.fetchActiveMembershipsForAcspNumberAndUserRole( acspProfile.getNumber(), role, pageable ) )
                .orElseGet( () -> includeRemoved ? acspMembersRepository.fetchActiveAndRemovedMembershipsForAcspNumber( acspProfile.getNumber(), pageable ) : acspMembersRepository.fetchActiveMembershipsForAcspNumber( acspProfile.getNumber(), pageable ) );
        final var memberships = acspMembershipCollectionMappers.daoToDto( membershipDaos, null, acspProfile );

        LOGGER.debugContext( getXRequestId(), String.format( "Successfully retrieved members for Acsp %s", acspProfile.getNumber() ), null );
        return memberships;
    }

    @Transactional( readOnly = true )
    public Optional<AcspMembersDao> fetchActiveAcspMembership( final String userId, final String acspNumber ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch active membership for user %s and Acsp %s", userId, acspNumber ), null );
        final var membership = acspMembersRepository.fetchActiveMembership( userId, acspNumber );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched active membership for user %s and Acsp %s", userId, acspNumber ), null );
        return membership;
    }

    @Transactional( readOnly = true )
    public int fetchNumberOfActiveOwners( final String acspNumber ) {
        return acspMembersRepository.fetchNumberOfActiveOwners( acspNumber );
    }

    @Transactional
    public AcspMembership createMembership( final User user, final AcspProfile acspProfile, final UserRoleEnum userRole, final String addedByUserId ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create membership for user %s and Acsp %s", user.getUserId(), acspProfile.getNumber() ), null );

        final var now = LocalDateTime.now();
        final var proposedMembership = new AcspMembersDao()
                .userId( user.getUserId() )
                .acspNumber( acspProfile.getNumber() )
                .userRole( userRole.getValue() )
                .createdAt( now )
                .addedAt( now )
                .addedBy( addedByUserId )
                .etag( generateEtag() )
                .status( ACTIVE.getValue() );
        final var completedMembership = acspMembersRepository.insert( proposedMembership );

        final var membership = acspMembershipCollectionMappers.daoToDto( completedMembership, user, acspProfile );

        LOGGER.debugContext( getXRequestId(), String.format( "Successfully created membership for user %s and Acsp %s", user.getUserId(), acspProfile.getNumber() ), null );
        return membership;
    }

    private static <T> Function<Update, Update> enrichUpdate( final boolean when, final String key, final Supplier<T> value ){
        return update -> when ? update.set( key, value.get() ) : update;
    }

    @Transactional
    public void updateMembership( final String membershipId, final UserStatusEnum userStatus, final UserRoleEnum userRole, final String updatedBy ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update membership for id: %s", membershipId ), null );
        if ( Objects.isNull( membershipId ) ) {
            throw new InternalServerErrorRuntimeException( "Cannot update Acsp Membership", new Exception( "membershipId is null" ) );
        }

        final var isChangingRole = Objects.nonNull( userRole );
        final var isRemovingMembership = Objects.nonNull( userStatus );
        final var numbRecordsUpdated = Optional.of( new Update() )
                .map( enrichUpdate( true, "etag", GenerateEtagUtil::generateEtag ) )
                .map( enrichUpdate( isChangingRole, "user_role", () -> userRole.getValue() ) )
                .map( enrichUpdate( isRemovingMembership, "status", () -> userStatus.getValue() ) )
                .map( enrichUpdate( isRemovingMembership, "removed_by", () -> updatedBy ) )
                .map( enrichUpdate( isRemovingMembership, "removed_at", LocalDateTime::now ) )
                .map( update -> acspMembersRepository.updateAcspMembership( membershipId, update ) )
                .filter( numRecordsUpdated -> numRecordsUpdated != 0 )
                .orElseThrow( () -> new InternalServerErrorRuntimeException( String.format( "Failed to update Acsp Membership %s", membershipId ), new Exception( String.format( "Failed to update Acsp Membership with id: %s", membershipId ) ) ) );

        LOGGER.debugContext( getXRequestId(), String.format( "Successfully updated %d records (Acsp Membership with id: %s)", numbRecordsUpdated, membershipId ), null );
    }

}
