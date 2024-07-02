package uk.gov.companieshouse.acsp.manage.users.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

import java.util.Optional;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

    @Query(value = "{ 'id': ?0 }")
    Optional<AcspMembersDao> fetchAcspMemberById(final String Id);

}
