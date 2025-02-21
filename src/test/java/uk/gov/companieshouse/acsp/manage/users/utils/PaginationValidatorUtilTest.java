package uk.gov.companieshouse.acsp.manage.users.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.RequestContextData.RequestContextDataBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationValidatorUtilTest {

    @BeforeEach
    void setup(){
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setXRequestId( request ).build() );
    }

    @Test
    void testValidateAndGetParamsWithValidParams() {
        final var params = PaginationValidatorUtil.validateAndGetParams( 1, 10 );
        assertEquals( 1, params.pageIndex );
        assertEquals( 10, params.itemsPerPage );
    }

    @Test
    void testValidateAndGetParamsWithNullParams() {
        final var params = PaginationValidatorUtil.validateAndGetParams( null, null );
        assertEquals( 0, params.pageIndex );
        assertEquals( 15, params.itemsPerPage );
    }

    @Test
    void testValidateAndGetParamsWithNegativePageIndex() {
        final var exception = assertThrows( BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams( -1, 10 ) );
        assertEquals( "Please check the request and try again", exception.getMessage() );
    }

    @Test
    void testValidateAndGetParamsWithZeroItemsPerPage() {
        final var exception = assertThrows( BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams( 1, 0 ) );
        assertEquals( "Please check the request and try again", exception.getMessage() );
    }

    @Test
    void testValidateAndGetParamsWithNegativeItemsPerPage() {
        final var exception = assertThrows( BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams( 1, -1 ) );
        assertEquals( "Please check the request and try again", exception.getMessage() );
    }
}
