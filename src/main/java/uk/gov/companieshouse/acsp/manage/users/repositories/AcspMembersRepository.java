package uk.gov.companieshouse.acsp.manage.users.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

    @Query(value = "{ 'user_id': ?0 }")
    Stream<AcspMembersDao> fetchAcspMembersByUserId(final String userId);

    @Query(value = "{ 'user_id': ?0 }")
    Optional<AcspMembersDao> fetchAcspMemberByUserId(final String userId);

    @Query(value = "{ 'user_id': ?0, 'acsp_number': ?1 }")
    Optional<AcspMembersDao> fetchAcspMemberByUserIdAndAcspNumber(final String userId, final String acspNumber);

}
