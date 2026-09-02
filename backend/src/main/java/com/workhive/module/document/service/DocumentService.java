package com.workhive.module.document.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.document.entity.Document;
import com.workhive.module.document.repository.DocumentRepository;
import com.workhive.security.TenantContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final WorkActivityService workActivityService;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository,
                           StorageService storageService,
                           WorkActivityService workActivityService,
                           AuditService auditService) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.workActivityService = workActivityService;
        this.auditService = auditService;
    }

    public Page<Document> getDocuments(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return documentRepository.findByTenantIdAndStatus(tenantId, "ACTIVE", pageable);
    }

    public Page<Document> getMyDocuments(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return documentRepository.findByTenantIdAndOwnerIdAndStatus(tenantId, userId, "ACTIVE", pageable);
    }

    public List<Document> getDocumentsForEntity(String entityType, UUID entityId) {
        UUID tenantId = TenantContext.requireTenantId();
        return documentRepository.findByTenantIdAndEntityTypeAndEntityIdAndStatus(tenantId, entityType, entityId, "ACTIVE");
    }

    public Document getDocument(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, String name, String description,
                                   String entityType, UUID entityId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }

        try {
            String objectKey = storageService.storeFile(tenantId, file);

            String docName = (name != null && !name.isBlank()) ? name.trim() : file.getOriginalFilename();

            Document doc = Document.builder()
                    .tenantId(tenantId)
                    .name(docName)
                    .originalName(file.getOriginalFilename())
                    .filePath(objectKey)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .ownerId(userId)
                    .entityType(entityType != null ? entityType : "ORGANIZATION")
                    .entityId(entityId)
                    .description(description)
                    .version(1)
                    .status("ACTIVE")
                    .build();

            doc = documentRepository.save(doc);

            workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "DOCUMENT_UPLOADED",
                    "Uploaded document: " + doc.getName(), description, null, null);
            auditService.log(tenantId, userId, "DOCUMENT_UPLOADED", "DOCUMENT", doc.getId(), null, null);

            return doc;
        } catch (Exception e) {
            throw new BadRequestException("Failed to upload document: " + e.getMessage());
        }
    }

    public Resource downloadDocument(UUID id) {
        Document doc = getDocument(id);
        try {
            InputStream is = storageService.getFileStream(doc.getFilePath());
            return new InputStreamResource(is);
        } catch (Exception e) {
            throw new ResourceNotFoundException("File not found on storage: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocument(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Document doc = getDocument(id);
        doc.setStatus("INACTIVE");
        documentRepository.save(doc);
        auditService.log(tenantId, userId, "DOCUMENT_DELETED", "DOCUMENT", doc.getId(), null, null);
    }
}
