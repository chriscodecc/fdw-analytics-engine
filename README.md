# FDW Analytics Engine

The **FDW Analytics Engine** is a Spring Boot application designed for advanced financial data processing and analytics. It seamlessly integrates with the **FinDataWarehouse** 
(PostgreSQL database & ETL pipeline) as an external dependency.

---

## 🛠 Architecture & Prerequisites

The setup consists of two main components:
1. **FinDataWarehouse (Database / Service Layer):** Runs in a Docker container (PostgreSQL on port `5432`). Included via **Git Submodule**.
2. **FDW Analytics Engine (Analytics Application):** Runs locally or as a service via HTTPS on port `8443`.

### Requirements:
* **Java 17+** / Maven
* **Docker** & **Docker Compose**
* **Git**

---

## 📥 Quickstart: Clone & Setup

### 1. Clone Repository (with Submodules)
To fetch both this project and the required `FinDataWarehouse` repository in a single command, execute:

```bash
git clone --recurse-submodules [https://github.com/chriscodecc/fdw-analytics-engine.git](https://github.com/chriscodecc/fdw-analytics-engine.git)
cd fdw-analytics-engine
