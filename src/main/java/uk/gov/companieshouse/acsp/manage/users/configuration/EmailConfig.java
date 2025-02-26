package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan( basePackages = "uk.gov.companieshouse.email_producer" )
public class EmailConfig {}
