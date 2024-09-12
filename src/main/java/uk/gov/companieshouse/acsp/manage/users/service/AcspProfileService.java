package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AcspProfileEndpoint;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspProfileService {

    private final AcspProfileEndpoint acspProfileEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    public AcspProfileService( final AcspProfileEndpoint acspProfileEndpoint ) {
        this.acspProfileEndpoint = acspProfileEndpoint;
    }

    public Supplier<AcspProfile> createFetchAcspProfileRequest( final String acspNumber ){
        final var request = acspProfileEndpoint.createGetAcspInfoRequest( acspNumber );
        return () -> {
            try {
                LOG.debug( String.format( "Attempting to fetch profile for Acsp %s", acspNumber ) );
                return request.execute().getData();
            } catch ( ApiErrorResponseException exception ){
                if( exception.getStatusCode() == 404 ) {
                    LOG.error( String.format( "Could not find profile for Acsp %s", acspNumber ) );
                    throw new NotFoundRuntimeException( "acsp-manage-users-api", "Failed to find Acsp Profile" );
                } else {
                    LOG.error( String.format( "Failed to retrieve profile for Acsp %s", acspNumber ) );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve Acsp Profile" );
                }
            } catch( URIValidationException exception ){
                LOG.error( String.format( "Failed to fetch profile for Acsp %s, because uri was incorrectly formatted", acspNumber ) );
                throw new InternalServerErrorRuntimeException( "Invalid uri for acsp-profile-data-api service" );
            } catch ( Exception exception ){
                LOG.error( String.format( "Failed to retrieve profile for Acsp %s", acspNumber ) );
                throw new InternalServerErrorRuntimeException( "Failed to retrieve Acsp Profile" );
            }
        };
    }

    public AcspProfile fetchAcspProfile( final String acspNumber ) {
        return createFetchAcspProfileRequest( acspNumber ).get();
    }

    public Map<String, AcspProfile> fetchAcspProfiles( final Stream<AcspMembersDao> acspMembers ){
        final Map<String, AcspProfile> acsps = new ConcurrentHashMap<>();
        acspMembers.map( AcspMembersDao::getAcspNumber )
                .distinct()
                .map( this::createFetchAcspProfileRequest )
                .parallel()
                .map( Supplier::get )
                .forEach( acsp -> acsps.put( acsp.getNumber(), acsp ) );
        return acsps;
    }

}
