package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembersMapper;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembers;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import java.util.List;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;
  private final AcspMembershipListMapper acspMembershipListMapper;
  private final AcspMembersMapper acspMembersMapper;

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public AcspMembersService( final AcspMembersRepository acspMembersRepository, final AcspMembershipListMapper acspMembershipListMapper, final AcspMembersMapper acspMembersMapper ) {
      this.acspMembersRepository = acspMembersRepository;
      this.acspMembershipListMapper = acspMembershipListMapper;
      this.acspMembersMapper = acspMembersMapper;
  }

  @Transactional(readOnly = true)
  public List<AcspMembership> fetchAcspMemberships(final User user, final boolean includeRemoved) {
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

    return acspMembershipListMapper.daoToDto(acspMembers, user);
  }

    @Transactional( readOnly = true )
    public AcspMembers fetchAcspMembers( final AcspDataDao acspData, final boolean includeRemoved, final String userId, final String role, final int pageIndex, final int itemsPerPage ) {
        final var acspNumber = acspData.getId();

        if ( Objects.isNull( acspNumber ) ){
            throw new NullPointerException( "acspNumber is null." );
        }

        final var userRoles = Objects.nonNull( role ) ? Set.of( UserRoleEnum.fromValue( role ) ) : Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD );
        final var userIdRegex = Optional.ofNullable( userId ).orElse( "" );
        final var pageable = PageRequest.of( pageIndex, itemsPerPage );

        Page<AcspMembersDao> acspMembers;
        if ( includeRemoved ) {
            acspMembers = acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( acspNumber, userRoles, userIdRegex, pageable );
        } else {
            acspMembers = acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( acspNumber, userRoles, userIdRegex, null, pageable );
        }

        return acspMembersMapper.daoToDto( acspMembers, acspData );
    }

}
