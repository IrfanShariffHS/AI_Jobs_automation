# AI Job Automation Service

This is the automation microservice for the AI Job Automation Platform. It handles job application automation with quota management, job prioritization, and detailed tracking.

## Features

- **Quota Management**: Per-platform daily quota tracking
- **Job Prioritization**: Priority-based job queue processing
- **Application History**: Detailed tracking of all applications
- **Retry Logic**: Automatic retry with exponential backoff
- **Performance Metrics**: Analytics and reporting
- **Service-to-Service Communication**: Secure API integration with backend

## Architecture

- **Port**: 8081
- **Database**: MySQL (automation_db)
- **Backend Integration**: HTTP/REST with API key authentication

## API Endpoints

### Internal Automation Endpoints

- `POST /internal/automation/start` - Start automation process
- `POST /internal/automation/stop` - Stop automation process
- `GET /internal/automation/status/{automationId}` - Get automation status

## Environment Variables

- `SPRING_DATASOURCE_URL` - JDBC URL for automation database
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `BACKEND_API_URL` - Backend API URL
- `BACKEND_API_KEY` - API key for backend authentication
- `DB_ENCRYPTION_KEY` - Encryption key for credential decryption

## Database Schema

The service uses a separate database with the following tables:
- `automation_runs` - Automation execution history
- `job_queue` - Job queue management
- `quota_tracking` - Quota tracking per platform
- `application_history` - Detailed application tracking
- `retry_logs` - Retry attempt logs
- `performance_metrics` - Performance analytics

## Building

```bash
mvn clean package
```

## Running with Docker

```bash
docker build -t automation-service:latest .
docker run -p 8081:8081 automation-service:latest
```

## Integration with Backend

The automation service integrates with the backend through:
- **Read-Only Access**: Fetch user profiles, settings, credentials
- **Write-Only Access**: Create application records and job postings
- **Authentication**: API key via `X-Service-Key` header

## Development

The service is designed to work independently without affecting existing backend functionality. All changes are additive and follow the zero-impact principle.
