package uk.gov.companieshouse.acsp.manage.users.repositories;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

  @Query(
      "{ 'acsp_number': ?0, 'user_role': { $in: ?1 }, 'user_id': { $regex: ?2 }, 'removed_by': ?3 }")
  Page<AcspMembersDao> findAllByAcspNumberUserRolesAndUserIdLike(
      final String acspNumber,
      final Set<UserRoleEnum> userRoles,
      final String userId,
      final String removed_by,
      final Pageable pageable);

  @Query("{ 'acsp_number': ?0, 'user_role': { $in: ?1 }, 'user_id': { $regex: ?2 } }")
  Page<AcspMembersDao> findAllByAcspNumberUserRolesAndUserIdLike(
      final String acspNumber,
      final Set<UserRoleEnum> userRoles,
      final String userId,
      final Pageable pageable);

  @Query(value = "{ 'user_id': ?0 }")
  List<AcspMembersDao> fetchAllAcspMembersByUserId(final String userId);

  @Query(value = "{ 'user_id': ?0, 'removed_by': { $exists: false } }")
  List<AcspMembersDao> fetchActiveAcspMembersByUserId(final String userId);
}
