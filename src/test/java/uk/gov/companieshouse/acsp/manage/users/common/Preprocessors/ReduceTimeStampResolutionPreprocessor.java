package uk.gov.companieshouse.acsp.manage.users.common.Preprocessors;

import java.util.Objects;

import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.reduceTimestampResolution;

public class ReduceTimeStampResolutionPreprocessor extends Preprocessor {

    @Override
    public Object preprocess( final Object object ) {
        return Objects.isNull( object ) ? null : reduceTimestampResolution( (String) object );
    }

}
