package uk.gov.companieshouse.acsp.manage.users.common.Preprocessors;

import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.reduceTimestampResolution;

import java.util.Objects;

public class ReduceTimeStampResolutionPreprocessor extends Preprocessor {

    @Override
    public Object preprocess( final Object object ) {
        return Objects.isNull( object ) ? null : reduceTimestampResolution( (String) object );
    }

}
