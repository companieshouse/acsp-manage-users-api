package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.List;
import java.util.Objects;
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipMapper;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipsListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspMembersService {

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipListMapper;
  private final AcspMembershipsListMapper acspMembershipsListMapper;
  private final AcspMembershipMapper acspMembershipMapper;

  public AcspMembersService(
      final AcspMembersRepository acspMembersRepository,
      final AcspMembershipListMapper acspMembershipListMapper,
      final AcspMembershipsListMapper acspMembershipsListMapper,
      final AcspMembershipMapper acspMembershipMapper) {
    this.acspMembersRepository = acspMembersRepository;
    this.acspMembershipListMapper = acspMembershipListMapper;
    this.acspMembershipsListMapper = acspMembershipsListMapper;
    this.acspMembershipMapper = acspMembershipMapper;
  }

  @Transactional(readOnly = true)
  public AcspMembershipsList findAllByAcspNumberAndRole(
      final String acspNumber,
      final AcspDataDao acspDataDao,
      final String role,
      final boolean includeRemoved,
      final int pageIndex,
      final int itemsPerPage) {
    final Pageable pageable = PageRequest.of(pageIndex, itemsPerPage);
    Page<AcspMembersDao> acspMemberDaos;

    if (Objects.nonNull(role)) {
      if (includeRemoved) {
        acspMemberDaos =
            acspMembersRepository.findAllByAcspNumberAndUserRole(acspNumber, role, pageable);
      } else {
        acspMemberDaos =
            acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
                acspNumber, role, pageable);
      }
    } else {
      if (includeRemoved) {
        acspMemberDaos = acspMembersRepository.findAllByAcspNumber(acspNumber, pageable);
      } else {
        acspMemberDaos = acspMembersRepository.findAllNotRemovedByAcspNumber(acspNumber, pageable);
      }
    }

    return acspMembershipsListMapper.daoToDto(acspMemberDaos, acspDataDao);
  }

  @Transactional(readOnly = true)
  public AcspMembershipsList fetchAcspMemberships(final User user, final boolean includeRemoved) {
    LOG.debug(
        String.format(
            "Fetching ACSP memberships from the repository for user ID: %s, include removed: %b",
            user.getUserId(), includeRemoved));

    List<AcspMembersDao> acspMembers;
    if (includeRemoved) {
      acspMembers = acspMembersRepository.fetchAllAcspMembersByUserId(user.getUserId());
    } else {
      acspMembers = acspMembersRepository.fetchActiveAcspMembersByUserId(user.getUserId());
    }

    final var acspMembershipsList = new AcspMembershipsList();
    acspMembershipsList.setItems(acspMembershipListMapper.daoToDto(acspMembers, user));
    return acspMembershipsList;
  }

    @Transactional( readOnly = true )
    public Optional<AcspMembersDao> fetchMembershipDao( final String membershipId ){
      return acspMembersRepository.findById( membershipId );
    }

    @Transactional( readOnly = true )
    public Optional<AcspMembership> fetchMembership( final String membershipId ){
       return fetchMembershipDao( membershipId ).map( acspMembershipMapper::daoToDto );
    }

    @Transactional( readOnly = true )
    public int fetchNumberOfActiveOwners( final String acspNumber ){
       return acspMembersRepository.fetchNumberOfActiveOwners( acspNumber );
    }

    @Transactional( readOnly = true )
    public Optional<AcspMembersDao> fetchActiveAcspMembership( final String userId, final String acspNumber ){
       return acspMembersRepository.fetchActiveAcspMembership( userId, acspNumber );
    }

    @Transactional
    public void updateMembership( final String membershipId, final UserStatusEnum userStatus, final UserRoleEnum userRole, final String requestingUserId ){
        if ( Objects.isNull( membershipId ) ){
            throw new IllegalArgumentException( "membershipId cannot be null" );
        }

        final var update = new Update();
        update.set( "etag", generateEtag() );

        if ( Objects.nonNull( userRole ) ){
            update.set( "user_role", userRole.getValue() );
        }

        if ( Objects.nonNull( userStatus ) ){
           update.set( "status", userStatus.getValue() );
           update.set( "removed_by", requestingUserId );
           update.set( "removed_at", LocalDateTime.now() );
        }

        final var numRecordsUpdated = acspMembersRepository.updateAcspMembership( membershipId, update );
        if ( numRecordsUpdated == 0 ){
            LOG.error( String.format( "Failed to update Acsp Membership %s", membershipId ) );
            throw new InternalServerErrorRuntimeException( String.format( "Failed to update Acsp Membership %s", membershipId ) );
        }
    }

}
