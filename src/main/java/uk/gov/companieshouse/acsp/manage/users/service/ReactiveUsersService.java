package uk.gov.companieshouse.acsp.manage.users.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.acsp.manage.users.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ReactiveUsersService {

    private final WebClient usersWebClient;

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private ReactiveUsersService( @Qualifier( "usersWebClient" ) final WebClient usersWebClient ){
        this.usersWebClient = usersWebClient;
    }

    private Mono<User> toFetchUserDetailsRequest( final String userId ) {
        return usersWebClient.get()
                .uri( String.format( "/users/%s", userId ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( User.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception ){
                        if ( NOT_FOUND.equals( exception.getStatusCode() ) ){
                            LOG.errorContext( getXRequestId(), String.format( "Could not find user details for user with id %s", userId ), exception, null );
                            return new NotFoundRuntimeException( APPLICATION_NAMESPACE, "Failed to find user" );
                        }
                    }
                    LOG.errorContext( getXRequestId(), String.format( "Failed to retrieve user details for user with id %s", userId ), (Exception) throwable, null );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve user details" );
                } )
                .doOnSubscribe( onSubscribe -> LOG.infoContext( getXRequestId(), String.format( "Sending request to accounts-user-api: GET /users/{user_id}. Attempting to retrieve user: %s", userId ), null ) )
                .doFinally( signalType -> LOG.infoContext( getXRequestId(), String.format( "Finished request to accounts-user-api for user: %s", userId ), null ) );
    }

    public User fetchUserDetails( final String userId ){
        return toFetchUserDetailsRequest( userId ).block( Duration.ofSeconds( 20L ) );
    }

    public boolean doesUserExist( final String userId ) {
        return Boolean.TRUE.equals( toFetchUserDetailsRequest( userId )
                .map( Objects::nonNull )
                .onErrorResume( NotFoundRuntimeException.class, throwable -> Mono.just( false ) )
                .block( Duration.ofSeconds( 20L ) ) );
    }

    public Map<String, User> fetchUserDetails( final Stream<AcspMembersDao> acspMembers ){
        return Flux.fromStream( acspMembers )
                .map( AcspMembersDao::getUserId )
                .distinct()
                .flatMap( this::toFetchUserDetailsRequest )
                .collectMap( User::getUserId )
                .block( Duration.ofSeconds( 20L ) );
    }

    public UsersList searchUserDetails( final List<String> emails ) {
        return usersWebClient.get()
                .uri( "/users/search?user_email=" + String.join( "&user_email=", emails ) )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( UsersList.class ) )
                .onErrorMap( throwable -> {
                    LOG.errorContext( getXRequestId(), "Failed to retrieve user details", (Exception) throwable, null );
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve user details" );
                } )
                .doOnSubscribe( onSubscribe -> LOG.infoContext( getXRequestId(), String.format( "Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join( ", ", emails ) ), null ) )
                .doFinally( signalType -> LOG.infoContext( getXRequestId(), String.format( "Finished request to accounts-user-api for users: %s", String.join( ", ", emails ) ), null ) )
                .block( Duration.ofSeconds( 20L ) );
    }

}
