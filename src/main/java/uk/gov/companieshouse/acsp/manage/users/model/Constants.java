package uk.gov.companieshouse.acsp.manage.users.model;

import java.util.regex.Pattern;

public final class Constants {

  private Constants(){}

  public static final String ACSP_MEMBERS_READ = "acsp_members=read";
  public static final String KEY_ROLE = "KEY";
  public static final Pattern ACSP_NUMBER_PATTERN = Pattern.compile("(?<=^|\\s)acsp_number=([0-9A-Za-z-_]{0,32})(?=\\s|$)");
  public static final String ACSP_MEMBERS_OWNERS = "acsp_members_owners=create,update,delete";
  public static final String ACSP_MEMBERS_ADMINS = "acsp_members_admins=create,update,delete";
  public static final String ACSP_MEMBERS_STANDARD = "acsp_members_standard=create,update,delete";
  public static final String ACSP_MEMBERS_READ_PERMISSION = "acsp_members=read";
  public static final String ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE = "ADMIN_WITH_ACSP_SEARCH_PRIVILEGE";
  public static final String ACSP_SEARCH_ADMIN_SEARCH = "/admin/acsp/search";
  public static final String ACSP_OWNER_ROLE = "ACSP_OWNER";
  public static final String ACSP_ADMIN_ROLE = "ACSP_ADMIN";
  public static final String ACSP_STANDARD_ROLE = "ACSP_STANDARD";
  public static final String END_POINT_URL_TEMPLATE = "/acsps/%s/memberships";
  public static final String PAGINATION_URL_TEMPLATE =  "%s?page_index=%d&items_per_page=%d";
  public static final String UNKNOWN = "unknown";
  public static final String KEY = "key";
  public static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";

}
