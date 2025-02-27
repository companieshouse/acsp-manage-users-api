package uk.gov.companieshouse.acsp.manage.users.common.database;

import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Optional;

@org.springframework.context.annotation.Configuration
@EnableMongoRepositories( "uk.gov.companieshouse.acsp.manage.users.membership" )
@EnableMongoAuditing( dateTimeProviderRef = "mongodbDatetimeProvider" )
public class MongoConfig {

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener( final LocalValidatorFactoryBean factory ) {
        return new ValidatingMongoEventListener( factory );
    }

    @Bean( name = "mongodbDatetimeProvider" )
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of( LocalDateTime.now() );
    }

}
