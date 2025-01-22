package uk.gov.companieshouse.acsp.manage.users.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.acsp.manage.users.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspProfileService {

    private final WebClient acspWebClient;

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public AcspProfileService( @Qualifier( "acspWebClient" ) final WebClient acspWebClient ) {
        this.acspWebClient = acspWebClient;
    }

    private Mono<AcspProfile> toFetchAcspProfileRequest( final String acspNumber ) {
        return acspWebClient.get()
                .uri( String.format( "/authorised-corporate-service-providers/%s", acspNumber ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( AcspProfile.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception ){
                        if ( NOT_FOUND.equals( exception.getStatusCode() ) ){
                            LOG.errorContext( getXRequestId(), String.format( "Could not find profile for Acsp id: %s", acspNumber ), exception, null );
                            return new NotFoundRuntimeException( APPLICATION_NAMESPACE, "Failed to find Acsp Profile" );
                        }
                    }
                    LOG.errorContext( getXRequestId(), String.format( "Failed to retrieve profile for Acsp id: %s", acspNumber ), (Exception) throwable, null );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve Acsp Profile" );
                } )
                .doOnSubscribe( onSubscribe -> LOG.infoContext( getXRequestId(), String.format( "Sending request to acsp-profile-data-api: GET /authorised-corporate-service-providers/{acsp_number}. Attempting to retrieve acsp: %s", acspNumber ), null ) )
                .doFinally( signalType -> LOG.infoContext( getXRequestId(), String.format( "Finished request to acsp-profile-data-api for acsp: %s.", acspNumber ), null ) );
    }

    public AcspProfile fetchAcspProfile( final String acspNumber ){
        return toFetchAcspProfileRequest( acspNumber ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, AcspProfile> fetchAcspProfiles( final Stream<AcspMembersDao> acspMembers ) {
        return Flux.fromStream( acspMembers )
                .map( AcspMembersDao::getAcspNumber )
                .distinct()
                .flatMap( this::toFetchAcspProfileRequest )
                .collectMap( AcspProfile::getNumber )
                .block( Duration.ofSeconds( 20L ) );
    }

}



