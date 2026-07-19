package com.banquito.platform.document.application.service;

import com.banquito.platform.document.api.dto.api.*;
import com.banquito.platform.document.domain.enums.EstadoTipoDocumentoEnum;
import com.banquito.platform.document.domain.model.*;
import com.banquito.platform.document.domain.repository.*;
import com.banquito.platform.document.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private DocumentoVersionRepository versionRepository;

    @Mock
    private DocumentoPayloadRepository payloadRepository;

    @Mock
    private DocumentoEventoRepository eventoRepository;

    @Mock
    private TipoDocumentoCatalogoRepository tipoRepository;

    @InjectMocks
    private DocumentService documentService;

    private TipoDocumentoCatalogo tipoDocumento;

    @BeforeEach
    void setUp() {
        tipoDocumento = new TipoDocumentoCatalogo();
        tipoDocumento.setCode("CEDULA");
        tipoDocumento.setName("Cédula de identidad");
        tipoDocumento.setDescription("Documento de identificación");
        tipoDocumento.setOwnerService("CUSTOMER");
        tipoDocumento.setStatus(EstadoTipoDocumentoEnum.ACTIVO);
    }

    // Tests para listarTipos
    @Test
    void testListarTipos_RetornaLista() {
        when(tipoRepository.findByStatusOrderByNameAsc(EstadoTipoDocumentoEnum.ACTIVO))
                .thenReturn(List.of(tipoDocumento));

        List<DocumentTypeResponse> result = documentService.listarTipos();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CEDULA", result.get(0).code());
    }

    // Tests para obtener
    @Test
    void testObtener_UuidNoExiste_LanzaExcepcion() {
        when(documentoRepository.findByUuidDocumento("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.obtener("no-existe"));

        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para descargar
    @Test
    void testDescargar_PayloadNoExiste_LanzaExcepcion() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(payloadRepository.findFirstByUuidDocumentoOrderByFechaCreacionDesc("doc-uuid-123"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.descargar("doc-uuid-123"));

        assertEquals("DOCUMENT_PAYLOAD_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para registrar
    @Test
    void testRegistrar_DatosValidos_RetornaDocumento() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(tipoRepository.findByCode("CEDULA")).thenReturn(Optional.of(tipoDocumento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.registrar(request);

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
        verify(versionRepository).save(any(DocumentoVersion.class));
        verify(eventoRepository).save(any(DocumentoEvento.class));
    }

    @Test
    void testRegistrar_TipoDocumentoInactivo_LanzaExcepcion() {
        tipoDocumento.setStatus(EstadoTipoDocumentoEnum.INACTIVO);
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(tipoRepository.findByCode("CEDULA")).thenReturn(Optional.of(tipoDocumento));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.registrar(request));

        assertEquals("DOCUMENT_TYPE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void testRegistrar_TipoDocumentoNoExiste_LanzaExcepcion() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "NO_EXISTE", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(tipoRepository.findByCode("NO_EXISTE")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.registrar(request));

        assertEquals("DOCUMENT_TYPE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void testRegistrar_SinPayload_RetornaDocumento() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", null, null, "user-123", "corr-123", Map.of()
        );
        when(tipoRepository.findByCode("CEDULA")).thenReturn(Optional.of(tipoDocumento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.registrar(request);

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
        verify(versionRepository).save(any(DocumentoVersion.class));
        verify(eventoRepository).save(any(DocumentoEvento.class));
        verify(payloadRepository, never()).save(any(DocumentoPayload.class));
    }

    // Tests para registrarIdempotente
    @Test
    void testRegistrarIdempotente_DocumentoExistente_RetornaExistente() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        documento.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento.setContextoNegocio("CUSTOMER");
        documento.setTipoDocumento("CEDULA");
        documento.setUuidReferenciaNegocio("customer-uuid-456");
        documento.setNombreArchivo("cedula.pdf");
        documento.setTipoMime("application/pdf");
        documento.setRutaAlmacenamiento("/path");
        documento.setHashSha256("hash");
        documento.setFechaCreacion(LocalDateTime.now());
        documento.setCreadoPor("user-123");
        documento.setCorrelationId("corr-123");
        documento.setMetadata(Map.of());
        when(documentoRepository.findFirstByContextoNegocioAndTipoDocumentoAndUuidReferenciaNegocio(
                "CUSTOMER", "CEDULA", "customer-uuid-456")).thenReturn(Optional.of(documento));

        DocumentMetadataResponse result = documentService.registrarIdempotente(request);

        assertNotNull(result);
        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void testRegistrarIdempotente_DocumentoNoExiste_RetornaNuevo() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", "customer-uuid-456", "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(documentoRepository.findFirstByContextoNegocioAndTipoDocumentoAndUuidReferenciaNegocio(
                "CUSTOMER", "CEDULA", "customer-uuid-456")).thenReturn(Optional.empty());
        when(tipoRepository.findByCode("CEDULA")).thenReturn(Optional.of(tipoDocumento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.registrarIdempotente(request);

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
    }

    @Test
    void testRegistrarIdempotente_SinReferenciaNegocio_RetornaNuevo() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                "CUSTOMER", "CEDULA", null, "cedula.pdf", "application/pdf",
                "/path", "hash", "base64", "text", "user-123", "corr-123", Map.of()
        );
        when(tipoRepository.findByCode("CEDULA")).thenReturn(Optional.of(tipoDocumento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.registrarIdempotente(request);

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
    }

    // Tests para obtener (caso exitoso)
    @Test
    void testObtener_UuidValido_RetornaDocumento() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        documento.setContextoNegocio("CUSTOMER");
        documento.setTipoDocumento("CEDULA");
        documento.setUuidReferenciaNegocio("customer-uuid-456");
        documento.setNombreArchivo("cedula.pdf");
        documento.setTipoMime("application/pdf");
        documento.setRutaAlmacenamiento("/path");
        documento.setHashSha256("hash");
        documento.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento.setFechaCreacion(LocalDateTime.now());
        documento.setCreadoPor("user-123");
        documento.setCorrelationId("corr-123");
        documento.setMetadata(Map.of());
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));

        DocumentMetadataResponse result = documentService.obtener("doc-uuid-123");

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
    }

    // Tests para asociarReferencia
    @Test
    void testAsociarReferencia_DatosValidos_RetornaDocumento() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        documento.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento.setContextoNegocio("CUSTOMER");
        documento.setTipoDocumento("CEDULA");
        documento.setUuidReferenciaNegocio("customer-uuid-456");
        documento.setNombreArchivo("cedula.pdf");
        documento.setTipoMime("application/pdf");
        documento.setRutaAlmacenamiento("/path");
        documento.setHashSha256("hash");
        documento.setFechaCreacion(LocalDateTime.now());
        documento.setCreadoPor("user-123");
        documento.setCorrelationId("corr-123");
        documento.setMetadata(Map.of());
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.asociarReferencia("doc-uuid-123", "customer-uuid-456", "corr-123");

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
        verify(eventoRepository).save(any(DocumentoEvento.class));
    }

    @Test
    void testAsociarReferencia_DocumentoNoExiste_LanzaExcepcion() {
        when(documentoRepository.findByUuidDocumento("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.asociarReferencia("no-existe", "customer-uuid-456", "corr-123"));

        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testAsociarReferencia_SinCorrelationId_RetornaDocumento() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        documento.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento.setContextoNegocio("CUSTOMER");
        documento.setTipoDocumento("CEDULA");
        documento.setUuidReferenciaNegocio("customer-uuid-456");
        documento.setNombreArchivo("cedula.pdf");
        documento.setTipoMime("application/pdf");
        documento.setRutaAlmacenamiento("/path");
        documento.setHashSha256("hash");
        documento.setFechaCreacion(LocalDateTime.now());
        documento.setCreadoPor("user-123");
        documento.setCorrelationId("corr-123");
        documento.setMetadata(Map.of());
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadataResponse result = documentService.asociarReferencia("doc-uuid-123", "customer-uuid-456", null);

        assertNotNull(result);
        verify(documentoRepository).save(any(Documento.class));
    }

    // Tests para buscar
    @Test
    void testBuscar_SinFiltros_RetornaLista() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        documento.setContextoNegocio("CUSTOMER");
        documento.setTipoDocumento("CEDULA");
        documento.setUuidReferenciaNegocio("customer-uuid-456");
        documento.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento.setFechaCreacion(LocalDateTime.now());
        when(documentoRepository.findAll()).thenReturn(List.of(documento));

        List<DocumentMetadataResponse> result = documentService.buscar(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testBuscar_ConFiltros_RetornaFiltrada() {
        Documento documento1 = new Documento();
        documento1.setUuidDocumento("doc-uuid-123");
        documento1.setContextoNegocio("CUSTOMER");
        documento1.setTipoDocumento("CEDULA");
        documento1.setUuidReferenciaNegocio("customer-uuid-456");
        documento1.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento1.setFechaCreacion(LocalDateTime.now());

        Documento documento2 = new Documento();
        documento2.setUuidDocumento("doc-uuid-456");
        documento2.setContextoNegocio("ACCOUNT");
        documento2.setTipoDocumento("CONTRATO");
        documento2.setUuidReferenciaNegocio("account-uuid-789");
        documento2.setEstado(com.banquito.platform.document.domain.enums.EstadoDocumentoEnum.CREATED);
        documento2.setFechaCreacion(LocalDateTime.now().minusDays(1));

        when(documentoRepository.findAll()).thenReturn(List.of(documento1, documento2));

        List<DocumentMetadataResponse> result = documentService.buscar("CUSTOMER", "CEDULA", "customer-uuid-456");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("doc-uuid-123", result.get(0).documentUuid());
    }

    @Test
    void testBuscar_SinResultados_RetornaVacio() {
        when(documentoRepository.findAll()).thenReturn(List.of());

        List<DocumentMetadataResponse> result = documentService.buscar("CUSTOMER", "CEDULA", "customer-uuid-456");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // Tests para descargar (caso exitoso)
    @Test
    void testDescargar_PayloadExistente_RetornaPayload() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        DocumentoPayload payload = new DocumentoPayload();
        payload.setUuidDocumento("doc-uuid-123");
        payload.setUuidVersion("version-uuid-123");
        payload.setContenidoBase64("base64");
        payload.setContenidoTexto("text");
        payload.setFechaCreacion(java.time.LocalDateTime.now());
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(payloadRepository.findFirstByUuidDocumentoOrderByFechaCreacionDesc("doc-uuid-123"))
                .thenReturn(Optional.of(payload));

        DocumentPayloadResponse result = documentService.descargar("doc-uuid-123");

        assertNotNull(result);
        assertEquals("doc-uuid-123", result.documentUuid());
        assertEquals("version-uuid-123", result.versionUuid());
    }

    // Tests para registrarVersion
    @Test
    void testRegistrarVersion_DatosValidos_RetornaVersion() {
        RegisterDocumentVersionRequest request = new RegisterDocumentVersionRequest(
                "cedula.pdf", "application/pdf", "/path", "hash", "base64", "text", "user-123"
        );
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(versionRepository.findByUuidDocumentoOrderByNumeroVersionDesc("doc-uuid-123")).thenReturn(List.of());
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentVersionResponse result = documentService.registrarVersion("doc-uuid-123", request);

        assertNotNull(result);
        verify(versionRepository).save(any(DocumentoVersion.class));
        verify(documentoRepository).save(any(Documento.class));
        verify(eventoRepository).save(any(DocumentoEvento.class));
    }

    @Test
    void testRegistrarVersion_ConVersionesAnteriores_RetornaVersion() {
        RegisterDocumentVersionRequest request = new RegisterDocumentVersionRequest(
                "cedula.pdf", "application/pdf", "/path", "hash", "base64", "text", "user-123"
        );
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        DocumentoVersion versionAnterior = new DocumentoVersion();
        versionAnterior.setUuidVersion("version-uuid-123");
        versionAnterior.setUuidDocumento("doc-uuid-123");
        versionAnterior.setNumeroVersion(1);
        versionAnterior.setEstado(com.banquito.platform.document.domain.enums.EstadoVersionDocumentoEnum.ACTIVA);
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(versionRepository.findByUuidDocumentoOrderByNumeroVersionDesc("doc-uuid-123")).thenReturn(List.of(versionAnterior));
        when(versionRepository.saveAll(any())).thenReturn(List.of(versionAnterior));
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentVersionResponse result = documentService.registrarVersion("doc-uuid-123", request);

        assertNotNull(result);
        verify(versionRepository).saveAll(any());
        verify(versionRepository).save(any(DocumentoVersion.class));
    }

    @Test
    void testRegistrarVersion_SinPayload_RetornaVersion() {
        RegisterDocumentVersionRequest request = new RegisterDocumentVersionRequest(
                "cedula.pdf", "application/pdf", "/path", "hash", null, null, "user-123"
        );
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(versionRepository.findByUuidDocumentoOrderByNumeroVersionDesc("doc-uuid-123")).thenReturn(List.of());
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentVersionResponse result = documentService.registrarVersion("doc-uuid-123", request);

        assertNotNull(result);
        verify(payloadRepository, never()).save(any(DocumentoPayload.class));
    }

    // Tests para listarVersiones
    @Test
    void testListarVersiones_DocumentoValido_RetornaLista() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        DocumentoVersion version = new DocumentoVersion();
        version.setUuidVersion("version-uuid-123");
        version.setUuidDocumento("doc-uuid-123");
        version.setNumeroVersion(1);
        version.setNombreArchivo("cedula.pdf");
        version.setTipoMime("application/pdf");
        version.setRutaAlmacenamiento("/path");
        version.setHashSha256("hash");
        version.setEstado(com.banquito.platform.document.domain.enums.EstadoVersionDocumentoEnum.ACTIVA);
        version.setFechaCreacion(java.time.LocalDateTime.now());
        version.setCreadoPor("user-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(versionRepository.findByUuidDocumentoOrderByNumeroVersionDesc("doc-uuid-123")).thenReturn(List.of(version));

        List<DocumentVersionResponse> result = documentService.listarVersiones("doc-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("version-uuid-123", result.get(0).versionUuid());
    }

    @Test
    void testListarVersiones_DocumentoNoExiste_LanzaExcepcion() {
        when(documentoRepository.findByUuidDocumento("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.listarVersiones("no-existe"));

        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para registrarEvento
    @Test
    void testRegistrarEvento_DatosValidos_RetornaEvento() {
        RegisterDocumentEventRequest request = new RegisterDocumentEventRequest(
                com.banquito.platform.document.domain.enums.TipoEventoDocumentoEnum.REGISTRADO, "Document created", "user-123", "corr-123"
        );
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(eventoRepository.save(any(DocumentoEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentEventResponse result = documentService.registrarEvento("doc-uuid-123", request);

        assertNotNull(result);
        verify(eventoRepository).save(any(DocumentoEvento.class));
    }

    @Test
    void testRegistrarEvento_DocumentoNoExiste_LanzaExcepcion() {
        RegisterDocumentEventRequest request = new RegisterDocumentEventRequest(
                com.banquito.platform.document.domain.enums.TipoEventoDocumentoEnum.REGISTRADO, "Document created", "user-123", "corr-123"
        );
        when(documentoRepository.findByUuidDocumento("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.registrarEvento("no-existe", request));

        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // Tests para listarEventos
    @Test
    void testListarEventos_DocumentoValido_RetornaLista() {
        Documento documento = new Documento();
        documento.setUuidDocumento("doc-uuid-123");
        DocumentoEvento evento = new DocumentoEvento();
        evento.setUuidEvento("event-uuid-123");
        evento.setUuidDocumento("doc-uuid-123");
        evento.setTipoEvento(com.banquito.platform.document.domain.enums.TipoEventoDocumentoEnum.REGISTRADO);
        evento.setDetalle("Document created");
        evento.setActorUuid("user-123");
        evento.setFechaCreacion(java.time.LocalDateTime.now());
        evento.setCorrelationId("corr-123");
        when(documentoRepository.findByUuidDocumento("doc-uuid-123")).thenReturn(Optional.of(documento));
        when(eventoRepository.findByUuidDocumentoOrderByFechaCreacionDesc("doc-uuid-123")).thenReturn(List.of(evento));

        List<DocumentEventResponse> result = documentService.listarEventos("doc-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("event-uuid-123", result.get(0).eventUuid());
    }

    @Test
    void testListarEventos_DocumentoNoExiste_LanzaExcepcion() {
        when(documentoRepository.findByUuidDocumento("no-existe")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> documentService.listarEventos("no-existe"));

        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
