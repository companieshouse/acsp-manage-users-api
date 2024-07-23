package uk.gov.companieshouse.acsp.manage.users.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil.PaginationParams;

class PaginationValidatorUtilTest {

    @Test
    void testValidateAndGetParamsWithValidParams() {
        PaginationParams params = PaginationValidatorUtil.validateAndGetParams(1, 10);
        assertEquals(1, params.pageIndex);
        assertEquals(10, params.itemsPerPage);
    }

    @Test
    void testValidateAndGetParamsWithNullParams() {
        PaginationParams params = PaginationValidatorUtil.validateAndGetParams(null, null);
        assertEquals(0, params.pageIndex);
        assertEquals(15, params.itemsPerPage);
    }

    @Test
    void testValidateAndGetParamsWithNegativePageIndex() {
        Exception exception = assertThrows(BadRequestRuntimeException.class, () ->
                PaginationValidatorUtil.validateAndGetParams(-1, 10)
        );
        assertEquals("Please check the request and try again", exception.getMessage());
    }

    @Test
    void testValidateAndGetParamsWithZeroItemsPerPage() {
        Exception exception = assertThrows(BadRequestRuntimeException.class, () ->
                PaginationValidatorUtil.validateAndGetParams(1, 0)
        );
        assertEquals("Please check the request and try again", exception.getMessage());
    }

    @Test
    void testValidateAndGetParamsWithNegativeItemsPerPage() {
        Exception exception = assertThrows(BadRequestRuntimeException.class, () ->
                PaginationValidatorUtil.validateAndGetParams(1, -1)
        );
        assertEquals("Please check the request and try again", exception.getMessage());
    }
}
