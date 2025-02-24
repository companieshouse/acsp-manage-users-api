package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class PaginationValidatorUtil {

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
  private static final int DEFAULT_PAGE_INDEX = 0;
  private static final int DEFAULT_ITEMS_PER_PAGE = 15;

  private PaginationValidatorUtil() {
    // private instructor to hide the implicit public one
  }

  public static class PaginationParams {
    public final int pageIndex;
    public final int itemsPerPage;

    private PaginationParams(int pageIndex, int itemsPerPage) {
      this.pageIndex = pageIndex;
      this.itemsPerPage = itemsPerPage;
    }
  }

  public static PaginationParams validateAndGetParams(Integer pageIndex, Integer itemsPerPage) {
    int validatedPageIndex = (pageIndex == null) ? DEFAULT_PAGE_INDEX : pageIndex;
    int validatedItemsPerPage = (itemsPerPage == null) ? DEFAULT_ITEMS_PER_PAGE : itemsPerPage;

    if (validatedPageIndex < 0) {
      throw new BadRequestRuntimeException("Please check the request and try again", new Exception( "pageIndex was less than 0" ));
    }

    if (validatedItemsPerPage <= 0) {
      throw new BadRequestRuntimeException("Please check the request and try again", new Exception( "itemsPerPage was less than or equal to 0" ));
    }

    return new PaginationParams(validatedPageIndex, validatedItemsPerPage);
  }
}
