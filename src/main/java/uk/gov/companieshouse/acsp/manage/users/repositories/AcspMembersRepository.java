package uk.gov.companieshouse.acsp.manage.users.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

import java.util.List;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

  @Query("{ 'acsp_number': ?0, 'removed_by': null }")
  Page<AcspMembersDao> findAllNotRemovedByAcspNumber(
      final String acspNumber, final Pageable pageable);

  @Query("{ 'acsp_number': ?0 }")
  Page<AcspMembersDao> findAllByAcspNumber(final String acspNumber, final Pageable pageable);

  @Query(value = "{ 'user_id': ?0 }")
  List<AcspMembersDao> fetchAllAcspMembersByUserId(final String userId);

  @Query(value = "{ 'user_id': ?0, 'removed_by': null }")
  List<AcspMembersDao> fetchActiveAcspMembersByUserId(final String userId);
}
