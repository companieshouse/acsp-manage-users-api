package uk.gov.companieshouse.acsp.manage.users.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.function.Function;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;

public class ParsingUtil {

    public static <T> Function<String, T> parseJsonTo( Class<T> clazz ) {
        return json -> {
            final var objectMapper = new ObjectMapper();
            objectMapper.registerModule( new JavaTimeModule() );
            try {
                return objectMapper.readValue( json, clazz );
            } catch ( IOException e ){
                throw new InternalServerErrorRuntimeException( "Unable to parse json" );
            }
        };
    }

}
