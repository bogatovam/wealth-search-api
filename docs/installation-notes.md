# Installation Notes

## Prerequisites

Before installing the Wealth Search Engine, ensure you have the following installed:

- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 1.29 or higher)
- At least **8GB of free disk space** (for Docker images and Ollama model)
- At least **4GB of available RAM**

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd wealth-search-engine
```

### 2. Start the Application

Run the following command from the project root directory:

```bash
docker-compose up -d
```

This will start three services:
- **PostgreSQL** - Database server on port 5432
- **Ollama** - AI model server on port 11434
- **Application** - Spring Boot app on port 8080

### 3. Wait for Initial Setup to Complete

**IMPORTANT**: The first startup will take **5-10 minutes** to complete. This is normal and expected because:

1. Docker needs to pull base images (PostgreSQL, Ollama, Maven, JDK)
2. Maven downloads all project dependencies
3. The application is built from source (multi-module Maven project)
4. Ollama downloads the **gemma2:2b** model (~1.6GB)

You can monitor the progress by watching the logs:

```bash
docker-compose logs -f
```

### 4. Test

You can use postman collection to make requests

**IMPORTANT**: The first request to search will be long because of ollama init (~30s).

[Data Generation Collection.postman_collection.json](./Data%20Generation%20Collection.postman_collection.json)