package uk.gov.companieshouse.acsp.manage.users.utils;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ExceptionUtil {

    private ExceptionUtil(){}

    public static <T, U, P extends Exception, Q extends Supplier<RuntimeException>> Function<T, U> invokeAndMapException( final Function<T, U> method, final Class<P> fromException, final Q toException ){
        return input -> {
            try {
                return method.apply( input );
            } catch ( Exception exception ){
                if ( fromException.isInstance( exception ) ){
                    throw toException.get();
                } else {
                    throw exception;
                }
            }
        };
    }

}
