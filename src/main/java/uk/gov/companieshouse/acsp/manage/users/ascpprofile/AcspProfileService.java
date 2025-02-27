package uk.gov.companieshouse.acsp.manage.users.ascpprofile;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.ParsingUtil.parseJsonTo;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.common.model.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.membership.StorageModel;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

@org.springframework.stereotype.Service
public class AcspProfileService {

    private final WebClient acspWebClient;

    public AcspProfileService( @Qualifier( "acspWebClient" ) final WebClient acspWebClient ) {
        this.acspWebClient = acspWebClient;
    }

    private Mono<AcspProfile> toFetchAcspProfileRequest( final String acspNumber, final String xRequestId ) {
        return acspWebClient.get()
                .uri( String.format( "/authorised-corporate-service-providers/%s", acspNumber ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( AcspProfile.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( "Failed to find Acsp Profile", exception );
                    }
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve Acsp Profile", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to acsp-profile-data-api: GET /authorised-corporate-service-providers/{acsp_number}. Attempting to retrieve acsp: %s", acspNumber ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to acsp-profile-data-api for acsp: %s.", acspNumber ), null ) );
    }

    public AcspProfile fetchAcspProfile( final String acspNumber ){
        return toFetchAcspProfileRequest( acspNumber, getXRequestId() ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, AcspProfile> fetchAcspProfiles( final Stream<StorageModel> memberships ) {
        final var xRequestId = getXRequestId();
        return Flux.fromStream( memberships )
                .map( StorageModel::getAcspNumber )
                .distinct()
                .flatMap( acspNumber -> toFetchAcspProfileRequest( acspNumber, xRequestId ) )
                .collectMap( AcspProfile::getNumber )
                .block( Duration.ofSeconds( 20L ) );
    }

}



