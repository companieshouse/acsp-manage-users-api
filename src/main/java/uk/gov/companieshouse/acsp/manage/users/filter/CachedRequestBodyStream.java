package uk.gov.companieshouse.acsp.manage.users.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;

public class CachedRequestBodyStream extends ServletInputStream {

    private final ByteArrayInputStream body;

    public CachedRequestBodyStream( byte[] body ){
        this.body = new ByteArrayInputStream( body );
    }

    @Override
    public int read() {
        return body.read();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return body.available() == 0;
    }

    @Override
    public void setReadListener( final ReadListener readListener ) {
        throw new UnsupportedOperationException( "Not implemented" );
    }

}
