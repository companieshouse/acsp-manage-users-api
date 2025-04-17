package uk.gov.companieshouse.acsp.manage.users.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.ParsingUtil.parseJsonTo;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@Service
public class UsersService {

    private final WebClient usersWebClient;

    private UsersService( @Qualifier( "usersWebClient" ) final WebClient usersWebClient ){
        this.usersWebClient = usersWebClient;
    }

    private Mono<User> toFetchUserDetailsRequest( final String userId, final String xRequestId ) {
        return usersWebClient.get()
                .uri( String.format( "/users/%s", userId ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( User.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( "Failed to find user", exception );
                    }
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve user details", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to accounts-user-api: GET /users/{user_id}. Attempting to retrieve user: %s", userId ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to accounts-user-api for user: %s", userId ), null ) );
    }

    public User fetchUserDetails( final String userId ){
        return toFetchUserDetailsRequest( userId, getXRequestId() ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, User> fetchUserDetails( final Stream<AcspMembersDao> memberships ){
        final var xRequestId = getXRequestId();
        return Flux.fromStream( memberships )
                .filter( membership -> Objects.nonNull( membership.getUserId() ) )
                .map( AcspMembersDao::getUserId )
                .distinct()
                .flatMap( userId -> toFetchUserDetailsRequest( userId, xRequestId ) )
                .collectMap( User::getUserId )
                .block( Duration.ofSeconds( 20L ) );
    }

    public UsersList searchUserDetails( final List<String> emails ) {
        final var xRequestId = getXRequestId();
        return usersWebClient.get()
                .uri( "/users/search?user_email=" + String.join( "&user_email=", emails ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( UsersList.class ) )
                .onErrorMap( throwable -> { throw new InternalServerErrorRuntimeException( "Failed to retrieve user details", (Exception) throwable ); } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join( ", ", emails ) ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to accounts-user-api for users: %s", String.join( ", ", emails ) ), null ) )
                .block( Duration.ofSeconds( 20L ) );
    }

    public User retrieveUserDetails( final String userId, final String userEmail ){
        if ( Objects.nonNull( userId ) ) {
            return fetchUserDetails( userId );
        }

        return Optional.ofNullable( userEmail )
                .map( List::of )
                .map( this::searchUserDetails )
                .filter( users -> !users.isEmpty() )
                .map( UsersList::getFirst )
                .orElse( null );
    }

}
