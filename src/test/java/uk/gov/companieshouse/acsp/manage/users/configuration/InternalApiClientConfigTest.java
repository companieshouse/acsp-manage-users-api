package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.companieshouse.api.InternalApiClient;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalApiClientConfigTest {

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getBasePath()).thenReturn("https://test-api-url");
    }

    @Test
    void internalApiClientSupplierIsCreatedCorrectly() {
        // Act
        InternalApiClient client = internalApiClientSupplier.get();

        // Assert
        assertNotNull(client);
        assertEquals("https://test-api-url", client.getBasePath());
        verify(internalApiClientSupplier, times(1)).get();
    }
}