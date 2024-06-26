package uk.gov.companieshouse.acsp.manage.users.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;

@Repository
public interface AcspDataRepository extends MongoRepository<AcspDataDao, String> {

}
