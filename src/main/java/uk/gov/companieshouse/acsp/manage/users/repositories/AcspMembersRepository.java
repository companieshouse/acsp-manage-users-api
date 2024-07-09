package uk.gov.companieshouse.acsp.manage.users.repositories;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import org.springframework.data.mongodb.core.query.Update;

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

  @Query(value = "{ 'user_id': ?0, 'removed_by': null }")
  Optional<AcspMembersDao> fetchActiveAcspMemberByUserId(final String userId);

  @Query(value = "{ 'user_id': ?0, 'acsp_number': ?1, 'removed_by': null }")
  Optional<AcspMembersDao> fetchActiveAcspMemberByUserIdAndAcspNumber(
      final String userId, final String acspNumber);

  @Query(value = "{ 'user_id': ?0, 'acsp_number': ?1 }")
  List<AcspMembersDao> findByUserIdAndAcspNumber(final String userId, final String acspNumber);

  @Query(value = "{ 'user_id': ?0, 'removed_by': null }")
  List<AcspMembersDao> fetchActiveAcspMembersByUserId(final String userId);

  @Query( "{ 'acsp_number': ?0, 'user_id': ?1 }" )
  Optional<AcspMembersDao> fetchAcspMembership( final String acspNumber, final String userId );

  @Query( "{ '_id': ?0 }" )
  int updateAcspMembership( final String acspMemberId, final Update update );

}


