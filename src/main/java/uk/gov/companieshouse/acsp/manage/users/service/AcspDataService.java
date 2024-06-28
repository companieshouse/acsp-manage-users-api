package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

@Service
public class AcspDataService {

    private final AcspDataRepository acspDataRepository;

    public AcspDataService( final AcspDataRepository acspDataRepository ) {
        this.acspDataRepository = acspDataRepository;
    }

    @Transactional( readOnly = true )
    public AcspDataDao fetchAcspData( final String acspNumber ){
        final var acspDataOptional = acspDataRepository.findById( acspNumber );
        if ( acspDataOptional.isEmpty() ) {
            throw new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, String.format( "Acsp %s was not found.", acspNumber ) );
        }
        return acspDataOptional.get();
    }

}
