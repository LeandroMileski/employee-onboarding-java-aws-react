# Employee Onboarding System - Implementation Guide (Java Backend)

## Phase 1: Prerequisites & Setup (30 minutes)

### 1.1 AWS Account Setup
```powershell
# Download and install AWS CLI for Windows
# Go to: https://awscli.amazonaws.com/AWSCLIV2.msi
# Run the installer

# Configure AWS credentials
aws configure
# Enter your AWS Access Key ID, Secret Access Key, Region (us-east-1), and output format (json)
```

### 1.2 Install Development Tools
```powershell
# Install Java JDK 11 or higher
# Download from: https://adoptium.net/temurin/releases/
# Or use Chocolatey:
choco install openjdk11

# Install Maven
# Download from: https://maven.apache.org/download.cgi
# Or use Chocolatey:
choco install maven

# Install Node.js (v16+)
# Download from: https://nodejs.org/
# Or use Chocolatey:
choco install nodejs

# Install SAM CLI
# Download from: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install-windows.html
# Or use pip:
pip install aws-sam-cli

# Install Git
# Download from: https://git-scm.com/download/win
# Or use Chocolatey:
choco install git
```

### 1.3 Create GitHub Repository
```powershell
# Create new repository on GitHub (employee-onboarding)
git clone https://github.com/yourusername/employee-onboarding.git
cd employee-onboarding
```

---

## Phase 2: Infrastructure Setup (45 minutes)

### 2.1 Create S3 Bucket
```powershell
# Create S3 bucket for file storage
$timestamp = (Get-Date).ToString("yyyyMMddHHmmss")
aws s3 mb s3://employee-onboarding-docs-$timestamp
aws s3 mb s3://employee-onboarding-frontend-$timestamp

# Enable versioning
aws s3api put-bucket-versioning `
    --bucket employee-onboarding-docs-$timestamp `
    --versioning-configuration Status=Enabled
```

### 2.2 Create DynamoDB Tables
```powershell
# Create Employees table
aws dynamodb create-table `
    --table-name Employees `
    --attribute-definitions `
        AttributeName=employee_id,AttributeType=S `
        AttributeName=sort_key,AttributeType=S `
    --key-schema `
        AttributeName=employee_id,KeyType=HASH `
        AttributeName=sort_key,KeyType=RANGE `
    --billing-mode PAY_PER_REQUEST

# Create Documents table
aws dynamodb create-table `
    --table-name Documents `
    --attribute-definitions `
        AttributeName=employee_id,AttributeType=S `
        AttributeName=document_id,AttributeType=S `
    --key-schema `
        AttributeName=employee_id,KeyType=HASH `
        AttributeName=document_id,KeyType=RANGE `
    --billing-mode PAY_PER_REQUEST
```

### 2.3 Create SNS Topic
```powershell
# Create SNS topic for notifications
aws sns create-topic --name employee-onboarding-notifications

# Subscribe your email for testing
aws sns subscribe `
    --topic-arn arn:aws:sns:us-east-1:ACCOUNT_ID:employee-onboarding-notifications `
    --protocol email `
    --notification-endpoint your-email@example.com
```

---

## Phase 3: Java Backend Development (120 minutes)

### 3.1 Create Project Structure
```powershell
mkdir -p backend/src/main/java/com/company/onboarding
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/company/onboarding
mkdir -p frontend/src/componentsq   
```

### 3.2 Create Maven POM File
```xml
<!-- backend/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.company</groupId>
    <artifactId>employee-onboarding</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- AWS Lambda Core -->
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.1</version>
        </dependency>
        
        <!-- AWS Lambda Events -->
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-events</artifactId>
            <version>3.11.0</version>
        </dependency>
        
        <!-- AWS SDK v2 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>dynamodb</artifactId>
            <version>2.20.26</version>
        </dependency>
        
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.20.26</version>
        </dependency>
        
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sns</artifactId>
            <version>2.20.26</version>
        </dependency>
        
        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
        
        <!-- JUnit for testing -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <configuration>
                    <createDependencyReducedPom>false</createDependencyReducedPom>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.3 Create Model Classes

#### Employee Model
```java
// backend/src/main/java/com/company/onboarding/model/Employee.java
package com.company.onboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class Employee {
    @JsonProperty("employee_id")
    private String employeeId;
    
    @JsonProperty("sort_key")
    private String sortKey;
    
    private String name;
    private String email;
    private String department;
    
    @JsonProperty("start_date")
    private String startDate;
    
    private String status;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    public Employee() {
        this.sortKey = "PROFILE";
        this.status = "pending";
        this.createdAt = LocalDateTime.now().toString();
        this.updatedAt = LocalDateTime.now().toString();
    }
    
    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getSortKey() { return sortKey; }
    public void setSortKey(String sortKey) { this.sortKey = sortKey; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
```

#### Document Model
```java
// backend/src/main/java/com/company/onboarding/model/Document.java
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
    
    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    
    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
```

### 3.4 Create Lambda Functions

#### Upload Handler Lambda
```java
// backend/src/main/java/com/company/onboarding/handler/UploadHandler.java
package com.company.onboarding.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.company.onboarding.model.Document;
import com.company.onboarding.service.DocumentService;
import com.company.onboarding.service.S3Service;
import com.company.onboarding.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentService documentService = new DocumentService();
    private final S3Service s3Service = new S3Service();
    private final NotificationService notificationService = new NotificationService();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            // Parse request body
            JsonNode requestBody = objectMapper.readTree(input.getBody());
            String employeeId = requestBody.get("employeeId").asText();
            String fileName = requestBody.get("fileName").asText();
            String fileType = requestBody.get("fileType").asText();
            String fileContent = requestBody.get("fileContent").asText();
            
            // Generate unique document ID
            String documentId = UUID.randomUUID().toString();
            String s3Key = String.format("documents/%s/%s/%s-%s", employeeId, fileType, documentId, fileName);
            
            // Decode base64 content
            byte[] decodedContent = Base64.getDecoder().decode(fileContent);
            
            // Upload to S3
            s3Service.uploadFile(System.getenv("S3_BUCKET"), s3Key, decodedContent, 
                               input.getHeaders().getOrDefault("content-type", "application/octet-stream"));
            
            // Create document record
            Document document = new Document();
            document.setEmployeeId(employeeId);
            document.setDocumentId(documentId);
            document.setDocumentType(fileType);
            document.setFileName(fileName);
            document.setS3Key(s3Key);
            document.setFileSize((long) decodedContent.length);
            document.setContentType(input.getHeaders().getOrDefault("content-type", "application/octet-stream"));
            
            // Save to DynamoDB
            documentService.saveDocument(document);
            
            // Send notification
            String message = String.format("Document uploaded: %s for employee %s", fileName, employeeId);
            notificationService.sendNotification(message, "Document Upload Confirmation");
            
            // Success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "File uploaded successfully");
            responseBody.put("documentId", documentId);
            responseBody.put("s3Key", s3Key);
            
            response.setStatusCode(200);
            response.setHeaders(getCorsHeaders());
            response.setBody(objectMapper.writeValueAsString(responseBody));
            
        } catch (Exception e) {
            context.getLogger().log("Upload error: " + e.getMessage());
            
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Upload failed");
            errorBody.put("details", e.getMessage());
            
            response.setStatusCode(500);
            response.setHeaders(getCorsHeaders());
            try {
                response.setBody(objectMapper.writeValueAsString(errorBody));
            } catch (Exception ex) {
                response.setBody("{\"error\":\"Internal server error\"}");
            }
        }
        
        return response;
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Methods", "POST");
        return headers;
    }
}
```

#### Employee Manager Lambda
```java
// backend/src/main/java/com/company/onboarding/handler/EmployeeHandler.java
package com.company.onboarding.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.company.onboarding.model.Employee;
import com.company.onboarding.service.EmployeeService;
import com.company.onboarding.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EmployeeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EmployeeService employeeService = new EmployeeService();
    private final NotificationService notificationService = new NotificationService();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            String httpMethod = input.getHttpMethod();
            
            switch (httpMethod) {
                case "POST":
                    return createEmployee(input, context);
                case "GET":
                    return getEmployee(input, context);
                case "PUT":
                    return updateEmployee(input, context);
                case "DELETE":
                    return deleteEmployee(input, context);
                default:
                    response.setStatusCode(405);
                    response.setBody("{\"error\":\"Method not allowed\"}");
                    return response;
            }
        } catch (Exception e) {
            context.getLogger().log("Employee management error: " + e.getMessage());
            response.setStatusCode(500);
            response.setHeaders(getCorsHeaders());
            response.setBody("{\"error\":\"Internal server error\"}");
            return response;
        }
    }
    
    private APIGatewayProxyResponseEvent createEmployee(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            Employee employee = objectMapper.readValue(input.getBody(), Employee.class);
            String employeeId = UUID.randomUUID().toString();
            employee.setEmployeeId(employeeId);
            
            // Save to DynamoDB
            employeeService.saveEmployee(employee);
            
            // Send welcome notification
            String message = String.format("Welcome %s! Your employee ID is %s", 
                                         employee.getName(), employeeId);
            notificationService.sendNotification(message, "Welcome to the Team");
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("employeeId", employeeId);
            responseBody.put("message", "Employee created successfully");
            
            response.setStatusCode(201);
            response.setHeaders(getCorsHeaders());
            response.setBody(objectMapper.writeValueAsString(responseBody));
            
        } catch (Exception e) {
            context.getLogger().log("Create employee error: " + e.getMessage());
            response.setStatusCode(500);
            response.setHeaders(getCorsHeaders());
            response.setBody("{\"error\":\"Failed to create employee\"}");
        }
        
        return response;
    }
    
    private APIGatewayProxyResponseEvent getEmployee(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            String employeeId = input.getPathParameters().get("employeeId");
            Employee employee = employeeService.getEmployee(employeeId);
            
            if (employee != null) {
                response.setStatusCode(200);
                response.setHeaders(getCorsHeaders());
                response.setBody(objectMapper.writeValueAsString(employee));
            } else {
                response.setStatusCode(404);
                response.setHeaders(getCorsHeaders());
                response.setBody("{\"error\":\"Employee not found\"}");
            }
            
        } catch (Exception e) {
            context.getLogger().log("Get employee error: " + e.getMessage());
            response.setStatusCode(500);
            response.setHeaders(getCorsHeaders());
            response.setBody("{\"error\":\"Failed to get employee\"}");
        }
        
        return response;
    }
    
    private APIGatewayProxyResponseEvent updateEmployee(APIGatewayProxyRequestEvent input, Context context) {
        // Implementation for updating employee
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(501);
        response.setHeaders(getCorsHeaders());
        response.setBody("{\"message\":\"Update not implemented yet\"}");
        return response;
    }
    
    private APIGatewayProxyResponseEvent deleteEmployee(APIGatewayProxyRequestEvent input, Context context) {
        // Implementation for deleting employee
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(501);
        response.setHeaders(getCorsHeaders());
        response.setBody("{\"message\":\"Delete not implemented yet\"}");
        return response;
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        return headers;
    }
}
```.publish({
        TopicArn: process.env.SNS_TOPIC,
        Message: `Welcome ${name}! Your employee ID is ${employeeId}`,
        Subject: 'Welcome to the Team'
    }).promise();
    
    return {
        statusCode: 201,
        headers: { 'Access-Control-Allow-Origin': '*' },
        body: JSON.stringify({
            employeeId,
            message: 'Employee created successfully'
        })
    };
}

async function getEmployee(employeeId) {
    const params = {
        TableName: process.env.EMPLOYEES_TABLE,
        Key: {
            employee_id: employeeId,
            sort_key: 'PROFILE'
        }
    };
    
    const result = await dynamodb.get(params).promise();
    
    return {
        statusCode: 200,
        headers: { 'Access-Control-Allow-Origin': '*' },
        body: JSON.stringify(result.Item)
    };
}
```

### 3.3 Create SAM Template
```yaml
# backend/template.yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: Employee Onboarding System

Parameters:
  S3BucketName:
    Type: String
    Default: employee-onboarding-docs
  SNSTopicArn:
    Type: String
    Description: SNS Topic ARN for notifications

Globals:
  Function:
    Timeout: 30
    Runtime: nodejs16.x
    Environment:
      Variables:
        S3_BUCKET: !Ref S3BucketName
        EMPLOYEES_TABLE: !Ref EmployeesTable
        DOCUMENTS_TABLE: !Ref DocumentsTable
        SNS_TOPIC: !Ref SNSTopicArn

Resources:
  # API Gateway
  OnboardingApi:
    Type: AWS::Serverless::Api
    Properties:
      StageName: prod
      Cors:
        AllowMethods: "'*'"
        AllowHeaders: "'*'"
        AllowOrigin: "'*'"

  # Lambda Functions
  UploadHandlerFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: src/lambdas/upload-handler.js
      Handler: upload-handler.handler
      Events:
        UploadApi:
          Type: Api
          Properties:
            RestApiId: !Ref OnboardingApi
            Path: /upload
            Method: post
      Policies:
        - S3WritePolicy:
            BucketName: !Ref S3BucketName
        - DynamoDBWritePolicy:
            TableName: !Ref DocumentsTable
        - SNSPublishMessagePolicy:
            TopicArn: !Ref SNSTopicArn

  EmployeeManagerFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: src/lambdas/employee-manager.js
      Handler: employee-manager.handler
      Events:
        CreateEmployee:
          Type: Api
          Properties:
            RestApiId: !Ref OnboardingApi
            Path: /employees
            Method: post
        GetEmployee:
          Type: Api
          Properties:
            RestApiId: !Ref OnboardingApi
            Path: /employees/{employeeId}
            Method: get
      Policies:
        - DynamoDBCrudPolicy:
            TableName: !Ref EmployeesTable
        - SNSPublishMessagePolicy:
            TopicArn: !Ref SNSTopicArn

  # DynamoDB Tables
  EmployeesTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: Employees
      AttributeDefinitions:
        - AttributeName: employee_id
          AttributeType: S
        - AttributeName: sort_key
          AttributeType: S
      KeySchema:
        - AttributeName: employee_id
          KeyType: HASH
        - AttributeName: sort_key
          KeyType: RANGE
      BillingMode: PAY_PER_REQUEST

  DocumentsTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: Documents
      AttributeDefinitions:
        - AttributeName: employee_id
          AttributeType: S
        - AttributeName: document_id
          AttributeType: S
      KeySchema:
        - AttributeName: employee_id
          KeyType: HASH
        - AttributeName: document_id
          KeyType: RANGE
      BillingMode: PAY_PER_REQUEST

Outputs:
  ApiEndpoint:
    Description: API Gateway endpoint URL
    Value: !Sub "https://${OnboardingApi}.execute-api.${AWS::Region}.amazonaws.com/prod/"
```

---

## Phase 4: Frontend Development (60 minutes)

### 4.1 Create React App
```bash
cd frontend
npx create-react-app . --template typescript
npm install axios react-router-dom @types/react-router-dom
```

### 4.2 Create File Upload Component
```jsx
// frontend/src/components/FileUpload.jsx
import React, { useState } from 'react';
import axios from 'axios';

const FileUpload = ({ employeeId, onUploadSuccess }) => {
    const [file, setFile] = useState(null);
    const [fileType, setFileType] = useState('');
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);

    const handleFileSelect = (event) => {
        const selectedFile = event.target.files[0];
        setFile(selectedFile);
    };

    const handleUpload = async () => {
        if (!file || !fileType) {
            alert('Please select a file and file type');
            return;
        }

        setUploading(true);
        setUploadProgress(0);

        try {
            const fileReader = new FileReader();
            fileReader.onload = async (e) => {
                const base64Content = e.target.result.split(',')[1];
                
                const uploadData = {
                    employeeId,
                    fileName: file.name,
                    fileType,
                    fileContent: base64Content
                };

                const response = await axios.post(
                    `${process.env.REACT_APP_API_URL}/upload`,
                    uploadData,
                    {
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        onUploadProgress: (progressEvent) => {
                            const progress = Math.round(
                                (progressEvent.loaded * 100) / progressEvent.total
                            );
                            setUploadProgress(progress);
                        }
                    }
                );

                if (response.data.documentId) {
                    onUploadSuccess(response.data);
                    setFile(null);
                    setFileType('');
                    setUploadProgress(0);
                }
            };

            fileReader.readAsDataURL(file);
        } catch (error) {
            console.error('Upload error:', error);
            alert('Upload failed. Please try again.');
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="file-upload-container">
            <h3>Upload Document</h3>
            
            <div className="form-group">
                <label>Document Type:</label>
                <select 
                    value={fileType} 
                    onChange={(e) => setFileType(e.target.value)}
                >
                    <option value="">Select document type</option>
                    <option value="identification">ID Document</option>
                    <option value="contract">Contract</option>
                    <option value="certificate">Certificate</option>
                    <option value="other">Other</option>
                </select>
            </div>

            <div className="form-group">
                <label>Select File:</label>
                <input 
                    type="file" 
                    onChange={handleFileSelect}
                    accept=".pdf,.jpg,.jpeg,.png"
                />
            </div>

            {file && (
                <div className="file-info">
                    <p>Selected: {file.name}</p>
                    <p>Size: {(file.size / 1024 / 1024).toFixed(2)} MB</p>
                </div>
            )}

            {uploading && (
                <div className="upload-progress">
                    <div className="progress-bar">
                        <div 
                            className="progress-fill"
                            style={{ width: `${uploadProgress}%` }}
                        ></div>
                    </div>
                    <p>{uploadProgress}% uploaded</p>
                </div>
            )}

            <button 
                onClick={handleUpload}
                disabled={!file || !fileType || uploading}
                className="upload-button"
            >
                {uploading ? 'Uploading...' : 'Upload Document'}
            </button>
        </div>
    );
};

export default FileUpload;
```

### 4.3 Create Main App Component
```jsx
// frontend/src/App.jsx
import React, { useState, useEffect } from 'react';
import FileUpload from './components/FileUpload';
import './App.css';

function App() {
    const [employee, setEmployee] = useState(null);
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // For demo purposes, create a test employee
        createTestEmployee();
    }, []);

    const createTestEmployee = async () => {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/employees`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name: 'Test Employee',
                    email: 'test@company.com',
                    department: 'Engineering',
                    startDate: new Date().toISOString().split('T')[0]
                })
            });

            const data = await response.json();
            setEmployee({ id: data.employeeId, name: 'Test Employee' });
        } catch (error) {
            console.error('Error creating test employee:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleUploadSuccess = (uploadData) => {
        setDocuments(prev => [...prev, uploadData]);
        alert('Document uploaded successfully!');
    };

    if (loading) {
        return <div className="loading">Setting up your account...</div>;
    }

    return (
        <div className="App">
            <header className="App-header">
                <h1>Employee Onboarding System</h1>
                {employee && (
                    <div className="employee-info">
                        <h2>Welcome, {employee.name}!</h2>
                        <p>Employee ID: {employee.id}</p>
                    </div>
                )}
            </header>

            <main className="App-main">
                {employee && (
                    <div className="onboarding-section">
                        <FileUpload 
                            employeeId={employee.id}
                            onUploadSuccess={handleUploadSuccess}
                        />
                        
                        <div className="documents-section">
                            <h3>Uploaded Documents</h3>
                            {documents.length > 0 ? (
                                <ul className="documents-list">
                                    {documents.map((doc, index) => (
                                        <li key={index} className="document-item">
                                            <span>Document ID: {doc.documentId}</span>
                                            <span>Status: Uploaded</span>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p>No documents uploaded yet.</p>
                            )}
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}

export default App;
```

---

## Phase 5: GitHub Actions Setup (30 minutes)

### 5.1 Create GitHub Secrets
In your GitHub repository, go to Settings > Secrets and add:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`

### 5.2 Create Deployment Workflows
```yaml
# .github/workflows/deploy-backend.yml
name: Deploy Backend
on:
  push:
    branches: [main]
    paths: ['backend/**']

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '16'
      
      - name: Setup SAM
        uses: aws-actions/setup-sam@v2
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.