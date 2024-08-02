package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspDataRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspDataServiceTest {

    @Mock
    private AcspDataRepository acspDataRepository;

    @InjectMocks
    private AcspDataService acspDataService;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void fetchAcspDetails(){
        final var acspMember = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        final var acsp = testDataManager.fetchAcspDataDaos( "TSA001" ).getFirst();

        Mockito.doReturn( Optional.of( acsp ) ).when( acspDataRepository ).findById( "TSA001" );

        Assertions.assertEquals( Map.of( "TSA001", acsp ), acspDataService.fetchAcspDetails( Stream.of( acspMember ) ) );
    }

}
