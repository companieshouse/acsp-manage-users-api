package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

@Service
public class AcspDataService {

  private final AcspDataRepository acspDataRepository;

  public AcspDataService(final AcspDataRepository acspDataRepository) {
    this.acspDataRepository = acspDataRepository;
  }

  @Transactional( readOnly = true )
  public AcspDataDao fetchAcspData( final String acspNumber ) {
    return createFetchAcspDataRequest( acspNumber ).get();
  }

  public Supplier<AcspDataDao> createFetchAcspDataRequest( final String acspNumber ){
    return () -> {
      final var acspDataOptional = acspDataRepository.findById( acspNumber );
      if ( acspDataOptional.isEmpty() ) {
        throw new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, String.format("Acsp %s was not found.", acspNumber) );
      }
      return acspDataOptional.get();
    };
  }

  @Transactional( readOnly = true )
  public Map<String, AcspDataDao> fetchAcspDetails( final Stream<AcspMembersDao> acspMembers ){
    final Map<String, AcspDataDao> acsps = new ConcurrentHashMap<>();
    acspMembers.map( AcspMembersDao::getAcspNumber )
            .distinct()
            .map( this::createFetchAcspDataRequest )
            .parallel()
            .map( Supplier::get )
            .forEach( acsp -> acsps.put( acsp.getId(), acsp ) );
    return acsps;
  }

}
