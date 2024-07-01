package uk.gov.companieshouse.acsp.manage.users.repositories;

import java.util.stream.Stream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

  @Query(value = "{ 'user_id': ?0 }")
  Stream<AcspMembersDao> fetchAcspMembersByUserId(final String userId);
}
