# FDW Analytics Engine

The **FDW Analytics Engine** is a Spring Boot application designed for advanced financial data processing and analytics. It seamlessly integrates with the **FinDataWarehouse** (PostgreSQL database & ETL pipeline) as an external dependency.

---

## Architecture & Prerequisites

The setup consists of two main components:
1. **FinDataWarehouse (Database / Service Layer):** Runs in a Docker container (PostgreSQL on port 5432). Included via **Git Submodule**.
2. **FDW Analytics Engine (Analytics Application):** Runs locally or as a service via HTTPS on port 8443.

### Requirements:
* **Java 17+** / Maven
* **Docker** & **Docker Compose**
* **Git**

---

## Quickstart: Clone & Setup

### 1. Clone Repository (with Submodules)
To fetch both this project and the required FinDataWarehouse repository in a single command, execute:

git clone --recurse-submodules https://github.com/chriscodecc/fdw-analytics-engine.git
cd fdw-analytics-engine

Note: If you already cloned the repository without --recurse-submodules, initialize the submodule manually:
git submodule update --init --recursive

---

## How to Run the Environment

### Step 1: Start the FinDataWarehouse Container
Navigate into the submodule folder and start the PostgreSQL database container:

cd FinDataWarehouse
docker-compose up -d
cd ..

This will spin up PostgreSQL 15, initialize the schema (init.sql), and expose port 5432 to your host system.

### Step 2: Run the Analytics Engine
Start the Spring Boot application locally via Maven:

./mvnw spring-boot:run

---

## Access, Ports & Credentials Quick Reference

### Endpoints & Web Access

* **FDW Analytics Engine:** https://localhost:8443 (HTTPS Enabled, Self-signed Cert)
* **Data Job Container:** http://localhost:8000 (Exposed Docker Container Port)

HTTPS Warning: Since the application uses a self-signed PKCS12 keystore (keystore.p12), your browser or API client (e.g., Postman) may display a certificate warning. Accept the self-signed certificate to proceed.

---

### Database Connection (FinDataWarehouse)

* **Host:** localhost
* **Port:** 5432
* **Database Name:** FinDataWarehouseDB
* **Username:** python_dbuser
* **Password:** th1s!SmyPy7onpW
* **Driver:** org.postgresql.Driver

---

### Application Security & Auth

* **SSL Key Alias:** analytics
* **SSL Key Password:** admin123
* **Custom API Key Header:** app.api.key=l`_Ggpsg[h8eGZZOPCK-;r(p1MQa+aCT

---

## Testing & Debugging

* **Spring Security Logging:** Debug logs for Spring Security and FilterChainProxy are enabled by default for local development (logging.level.org.springframework.security=DEBUG).
* **Testcontainers Support:** Configured for Docker-in-Docker / Host gateway communication via TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal.
