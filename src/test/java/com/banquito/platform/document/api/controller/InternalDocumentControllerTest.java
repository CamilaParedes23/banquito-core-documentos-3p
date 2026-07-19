package com.banquito.platform.document.api.controller;

import com.banquito.platform.document.api.dto.api.DocumentMetadataResponse;
import com.banquito.platform.document.api.dto.api.RegisterDocumentRequest;
import com.banquito.platform.document.application.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalDocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private InternalDocumentController controller;

    private DocumentMetadataResponse documentMetadataResponse;

    @BeforeEach
    void setUp() {
        documentMetadataResponse = new DocumentMetadataResponse("doc-uuid-123", "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf", "/path", "hash", "ACTIVO", LocalDateTime.now(), "user-123", "corr-123", Map.of());
    }

    // Tests para register
    @Test
    void testRegister_DatosValidos_RetornaCreado() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(documentService.registrarIdempotente(request)).thenReturn(documentMetadataResponse);

        DocumentMetadataResponse result = controller.register(request);

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
        verify(documentService).registrarIdempotente(request);
    }
}
