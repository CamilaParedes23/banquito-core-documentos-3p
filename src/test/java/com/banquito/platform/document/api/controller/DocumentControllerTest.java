package com.banquito.platform.document.api.controller;

import com.banquito.platform.document.api.dto.api.*;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController controller;

    private DocumentTypeResponse documentTypeResponse;
    private DocumentMetadataResponse documentMetadataResponse;
    private DocumentPayloadResponse documentPayloadResponse;
    private DocumentVersionResponse documentVersionResponse;
    private DocumentEventResponse documentEventResponse;

    @BeforeEach
    void setUp() {
        documentTypeResponse = new DocumentTypeResponse("CEDULA", "Cédula de identidad", "PDF", "CUSTOMER", "ACTIVO");
        documentMetadataResponse = new DocumentMetadataResponse("doc-uuid-123", "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf", "/path", "hash", "ACTIVO", LocalDateTime.now(), "user-123", "corr-123", Map.of());
        documentPayloadResponse = new DocumentPayloadResponse("doc-uuid-123", "version-uuid-123", "base64", "text", LocalDateTime.now());
        documentVersionResponse = new DocumentVersionResponse("version-uuid-123", "doc-uuid-123", 1, "cedula.pdf", "application/pdf", "/path", "hash", "ACTIVO", LocalDateTime.now(), "user-123");
        documentEventResponse = new DocumentEventResponse("event-uuid-123", "doc-uuid-123", "CREATED", "Document created", "user-123", LocalDateTime.now(), "corr-123");
    }

    // Tests para listTypes
    @Test
    void testListTypes_RetornaLista() {
        when(documentService.listarTipos()).thenReturn(List.of(documentTypeResponse));

        List<DocumentTypeResponse> result = controller.listTypes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CEDULA", result.get(0).code());
    }

    // Tests para register
    @Test
    void testRegister_DatosValidos_RetornaCreado() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(documentService.registrar(request)).thenReturn(documentMetadataResponse);

        DocumentMetadataResponse result = controller.register(request);

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
        verify(documentService).registrar(request);
    }

    // Tests para get
    @Test
    void testGet_UuidValido_RetornaDocumento() {
        when(documentService.obtener("doc-uuid-123")).thenReturn(documentMetadataResponse);

        DocumentMetadataResponse result = controller.get("doc-uuid-123");

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
    }

    // Tests para search
    @Test
    void testSearch_SinFiltros_RetornaLista() {
        when(documentService.buscar(null, null, null)).thenReturn(List.of(documentMetadataResponse));

        List<DocumentMetadataResponse> result = controller.search(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testSearch_ConFiltros_RetornaFiltrada() {
        when(documentService.buscar("CUSTOMER", "CEDULA", "customer-uuid-456")).thenReturn(List.of(documentMetadataResponse));

        List<DocumentMetadataResponse> result = controller.search("CUSTOMER", "CEDULA", "customer-uuid-456");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para download
    @Test
    void testDownload_UuidValido_RetornaPayload() {
        when(documentService.descargar("doc-uuid-123")).thenReturn(documentPayloadResponse);

        DocumentPayloadResponse result = controller.download("doc-uuid-123");

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
    }

    // Tests para registerVersion
    @Test
    void testRegisterVersion_DatosValidos_RetornaCreado() {
        RegisterDocumentVersionRequest request = new RegisterDocumentVersionRequest(
                "cedula.pdf", "application/pdf", "/path", "hash", "base64", "text", "user-123"
        );
        when(documentService.registrarVersion("doc-uuid-123", request)).thenReturn(documentVersionResponse);

        DocumentVersionResponse result = controller.registerVersion("doc-uuid-123", request);

        assertNotNull(result);
        assertEquals("version-uuid-123", result.versionUuid());
        verify(documentService).registrarVersion("doc-uuid-123", request);
    }

    // Tests para listVersions
    @Test
    void testListVersions_UuidValido_RetornaLista() {
        when(documentService.listarVersiones("doc-uuid-123")).thenReturn(List.of(documentVersionResponse));

        List<DocumentVersionResponse> result = controller.listVersions("doc-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Tests para registerEvent
    @Test
    void testRegisterEvent_DatosValidos_RetornaCreado() {
        RegisterDocumentEventRequest request = new RegisterDocumentEventRequest(
                com.banquito.platform.document.domain.enums.TipoEventoDocumentoEnum.REGISTRADO, "Document created", "user-123", "corr-123"
        );
        when(documentService.registrarEvento("doc-uuid-123", request)).thenReturn(documentEventResponse);

        DocumentEventResponse result = controller.registerEvent("doc-uuid-123", request);

        assertNotNull(result);
        assertEquals("event-uuid-123", result.eventUuid());
        verify(documentService).registrarEvento("doc-uuid-123", request);
    }

    // Tests para listEvents
    @Test
    void testListEvents_UuidValido_RetornaLista() {
        when(documentService.listarEventos("doc-uuid-123")).thenReturn(List.of(documentEventResponse));

        List<DocumentEventResponse> result = controller.listEvents("doc-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
