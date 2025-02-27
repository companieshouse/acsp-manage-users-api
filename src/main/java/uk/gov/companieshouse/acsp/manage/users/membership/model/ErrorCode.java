package uk.gov.companieshouse.acsp.manage.users.membership.model;

public enum ErrorCode {
    ERROR_CODE_1001( "1001" ),
    ERROR_CODE_1002( "1002" );

    private final String code;

    ErrorCode( final String code ){
       this.code = code;
    }

    public String getCode(){
       return String.format( "ERROR CODE: %s", code );
    }
}
