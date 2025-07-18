package com.company.onboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class Document {
    @JsonProperty("employee_id")
    private String employeeId;

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("s3_key")
    private String s3Key;

    @JsonProperty("upload_date")
    private String uploadDate;

    private String status;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("content_type")
    private String contentType;

    public Document() {
        this.uploadDate = LocalDateTime.now().toString();
        this.status = "uploaded";
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}