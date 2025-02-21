package uk.gov.companieshouse.acsp.manage.users.model;

public final class Constants {

  private Constants(){}

  public static final String ACSP_MEMBERS_OWNERS = "acsp_members_owners=create,update,delete";
  public static final String ACSP_MEMBERS_ADMINS = "acsp_members_admins=create,update,delete";
  public static final String ACSP_MEMBERS_READ_PERMISSION = "acsp_members=read";
  public static final String ACSP_SEARCH_ADMIN_SEARCH = "/admin/acsp/search";
  public static final String END_POINT_URL_TEMPLATE = "/acsps/%s/memberships";
  public static final String PAGINATION_URL_TEMPLATE =  "%s?page_index=%d&items_per_page=%d";
  public static final String UNKNOWN = "unknown";
  public static final String KEY = "key";
  public static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
  public static final String X_REQUEST_ID = "X-Request-Id";
  public static final String OAUTH2 = "oauth2";

}
