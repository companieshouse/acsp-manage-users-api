package uk.gov.companieshouse.acsp.manage.users.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspDataServiceTest {

    @Mock
    private AcspDataRepository acspDataRepository;

    @InjectMocks
    private AcspDataService acspDataService;

    @Test
    void createFetchAcspDataRequest(){
        acspDataService.createFetchAcspDataRequest( "TSA001" );
    }

    @Test
    void fetchAcspDetails(){
        Assertions.assertThrows( NullPointerException.class, () -> acspDataService.fetchAcspDetails( null ) );
    }

}
