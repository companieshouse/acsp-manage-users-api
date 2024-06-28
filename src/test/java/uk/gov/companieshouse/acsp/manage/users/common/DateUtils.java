package uk.gov.companieshouse.acsp.manage.users.common;

import java.time.LocalDateTime;

public class DateUtils {

    public static String reduceTimestampResolution( final String timestamp ) {
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    public static String localDateTimeToNormalisedString( final LocalDateTime localDateTime ) {
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

}
