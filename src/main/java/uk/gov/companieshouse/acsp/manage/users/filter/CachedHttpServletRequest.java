package uk.gov.companieshouse.acsp.manage.users.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class CachedHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedHttpServletRequest( final HttpServletRequest request ) throws IOException {
        super( request );
        final var inputStream = super.getInputStream();
        final var byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ( ( length = inputStream.read( buffer ) ) != -1 ) {
            byteArrayOutputStream.write( buffer, 0, length );
        }
        body = byteArrayOutputStream.toByteArray();
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader( new InputStreamReader( getInputStream() ) );
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedRequestBodyStream( body );
    }

}