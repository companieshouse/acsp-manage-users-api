package uk.gov.companieshouse.acsp.manage.users.testresources.Preprocessors;

import java.util.Objects;
import uk.gov.companieshouse.acsp.manage.users.testresources.ParsingUtils;

public class ReduceTimeStampResolutionPreprocessor extends Preprocessor {

    @Override
    public Object preprocess( final Object object ) {
        return Objects.isNull( object ) ? null : ParsingUtils.reduceTimestampResolution( (String) object );
    }

}
