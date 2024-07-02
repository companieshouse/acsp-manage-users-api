package uk.gov.companieshouse.acsp.manage.users.repositories;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

  @Query(value = "{ 'user_id': ?0 }")
  List<AcspMembersDao> fetchAllAcspMembersByUserId(final String userId);

  @Query(value = "{ 'user_id': ?0, 'removed_by': { $exists: false } }")
  List<AcspMembersDao> fetchActiveAcspMembersByUserId(final String userId);
}
