# ViewQL - CodeQL Security Scanner Web Interface

ViewQL is a web-based interface for running CodeQL security scans on Java applications.

## Prerequisites

- Java 17 JDK
- Maven 3.6+
- CodeQL CLI (version 2.11.0 or higher)
- Git

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Et43/viewql.git
   cd viewql
   ```

2. **Configure application.properties**
   
   Create or modify `src/main/resources/application.properties`:
   ```properties
   # Application settings
   spring.application.name=viewql
   server.port=8080
   
   # File upload limits
   spring.servlet.multipart.max-file-size=100MB
   spring.servlet.multipart.max-request-size=100MB
   
   # Error handling
   server.error.include-message=always
   server.error.include-stacktrace=never
   server.error.whitelabel.enabled=false
   
   # CodeQL paths - Update these to match your environment
   codeql.cli.path=/path/to/codeql/codeql
   codeql.extractors.path=/path/to/codeql/extractors/java
   codeql.database.storage-location=/path/to/storage/databases
   ```

## Running the Application

### Option 1: Direct Run (Development)
```bash
mvn spring-boot:run
```

### Option 2: WAR Deployment (Production)

1. Build the WAR file:
   ```bash
   mvn clean package
   ```

2. Deploy the generated WAR file (`target/viewql.war`) to Tomcat:
   - Copy the WAR file to `$TOMCAT_HOME/webapps/`
   - Restart Tomcat

## Verification

1. Access the application:
   - Direct run: http://localhost:8080
   - Tomcat deployment: http://localhost:8080/viewql

2. Navigate to the diagnostics page and run a simple diagnostics scan to ensure everything is working.

## Common Issues

### CodeQL CLI Not Found
Ensure the `codeql.cli.path` in `application.properties` points to the correct CodeQL executable location.

### Java Extractor Missing
Verify that `codeql.extractors.path` points to the Java extractor directory within your CodeQL installation.

### Storage Permission Issues
Make sure the application has write permissions to the directory specified in `codeql.database.storage-location`.

## Environment-Specific Notes

### Windows
- Use forward slashes (/) or escaped backslashes (\\) in paths
- Example path: `C:/Users/username/codeql/codeql.exe`

### Linux/macOS
- Use forward slashes (/) in paths
- Example path: `/opt/codeql/codeql`

