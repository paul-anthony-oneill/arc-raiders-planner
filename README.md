# ARC Raiders Loot Planner

A data-driven web application designed to optimize player efficiency in the extraction shooter, **ARC Raiders**. This tool recommends the **optimal map and location** based on the required item's loot type, demonstrating proficiency in modern **Java Backend Architecture** and **Full-Stack Development**.

---

## Technical Overview

This project is built using a modern, containerized, three-tier architecture, emphasizing industry-standard practices and tools.

### Backend (API/Logic)

| Technology | Version | Purpose & Feature Showcase |
| :--- | :--- | :--- |
| **Java** | 21 (LTS) | Used for application logic and concurrency. |
| **Spring Boot** | 3.x | Core framework for building robust, scalable REST APIs. |
| **Spring Data JPA** / **Hibernate** | Latest | ORM layer used for complex data modeling, demonstrated via **Many-to-Many relationships** (`Area` to `LootType`). |
| **PostgreSQL** | 15 (LTS) | Relational database (SQL) used for persistence, selected for data integrity and complex querying capabilities. |
| **Flyway** | Latest | **Database Migration Tool** used to version control the database schema and load static reference data (e.g., Maps, Loot Types) reliably. |
| **Project Lombok** | Latest | Used to reduce Java boilerplate code. |

### Frontend (User Interface)

| Technology | Version | Purpose & Feature Showcase |
| :--- | :--- | :--- |
| **React** | Latest | Library for building a fast, interactive single-page application (SPA). |
| **TypeScript** | Latest | Used for static type checking, improving code quality and maintainability (familiar to Java developers). |
| **Vite** | Latest | Modern, fast build tool for development and production bundling. |

### Infrastructure / DevOps

| Tool | Purpose & Feature Showcase |
| :--- | :--- |
| **Docker Compose** | Used to provision the local PostgreSQL database, ensuring environment parity and quick setup. |
| **Spring Boot Docker Compose Support** | Demonstrates DevOps awareness by automatically managing the PostgreSQL container lifecycle (start/stop) with the application. |

---

## Architectural Design

The application’s architecture is structured to highlight key engineering competencies.

### 1. Data Ingestion Pipeline (ETL)

This project features a custom data synchronization service, demonstrating integration with external, real-world services.

* **Extraction:** Data is sourced from the Metaforge ARC Raiders API.
* **Tool:** The modern **`RestClient`** is used to handle synchronous, paginated API calls, demonstrating knowledge of iterating through hundreds of records across multiple pages.
* **Transformation/Load:** The service maps complex, nested JSON objects (using **Records/DTOs**) into a clean, normalized relational model (JPA Entities).

### 2. Domain Model & Core Logic

The application’s intelligence lies in its normalized data model, which allows for powerful aggregation queries.

* **Core Entities:** `Item`, `GameMap`, `Area` (Loot Zone), and `LootType`.
* **Relationship Showcase:** An `Area` (Loot Zone) maintains a **Many-to-Many relationship** with multiple `LootType` categories (e.g., a single zone can spawn both 'Industrial' and 'Mechanical' loot).
* **Planner Algorithm (JPQL):** The core logic is executed via a single, highly optimized **JPQL query** that performs a `JOIN` across three tables and aggregates the results using a `COUNT` and `GROUP BY` clause. This returns a prioritized list of maps for any given item type.

---

## Key Endpoints & Functionality

The API provides the necessary endpoints to support the frontend application.

| Endpoint | Method | Functionality Showcase |
| :--- | :--- | :--- |
| `/api/items?search={term}` | `GET` | **Search Index:** Fetches items, supporting partial, case-insensitive search (`LIKE` query) for quick item selection. |
| `/api/items/recommendation?itemName={name}` | `GET` | **Core Planner Logic:** Takes an item name, determines its required `LootType`, and returns a ranked list of maps with the highest number of corresponding loot zones. |
| `/api/planner/loot-type?lootTypeName={name}` | `GET` | **Direct Planning:** Bypasses item search to plan directly from a category (e.g., finding the best map for 'Industrial' loot). |

---

## Quick Start (Developer Setup)

To run the application, you only need **Java 21** and **Docker Desktop** installed.

### Prerequisites

* Java Development Kit (JDK) 21
* Docker Desktop (Running)

### Setup & Run

1.  **Clone the repository:**
    ```bash
    git clone [YOUR_REPO_URL]
    cd arc-raiders-planner
    ```
2.  **Start the Backend & DB (Option A: Using Maven/IntelliJ):**
    Run the main `ArcRaidersPlannerApplication.java` file. Spring Boot will automatically detect and start the PostgreSQL container using the `compose.yaml` file.
3.  **Run Migrations & Sync:**
    On startup, **Flyway** will create the schema and **MetaforgeSyncService** will load all 500+ items from the external API.
4.  **Start the Frontend:**
    ```bash
    cd frontend
    npm install
    npm run dev
    ```
5.  **Access App:** Open your browser to the URL displayed by Vite (usually `http://localhost:5173`).

---

## Stretch Goals & Future Improvements

* **Authentication:** Implement Spring Security (OAuth2/JWT) to allow user customization (e.g., saving favorite routes).
* **Route Planning:** Implement a secondary `AreaConnection` entity and use the A* or Dijkstra algorithm in the `PlannerService` to calculate optimal routes between selected areas.
* **Visual Map:** Integrate the X/Y coordinates in the database with a React Leaflet or similar library to show a visual, interactive map of recommended zones.

---