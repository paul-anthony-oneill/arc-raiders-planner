# ARC Raiders Loot Planner

A web application designed to optimize player efficiency and ease the decision making process for which map and area to loot in the extraction shooter, **ARC Raiders**. This tool suggests an **optimal map and location** based on the required item and it's metadata, demonstrating proficiency in modern **Java Backend Architecture** and **Full-Stack Development**.

---

## Technical Overview

This project is built using a modern, containerized, three-tier architecture, emphasizing industry-standard practices and tools.

### Backend (API/Logic)

| Technology | Version | Purpose & Feature Showcase |
| :--- | :--- | :--- |
| **Java** | 21 (LTS) | Used for application logic and concurrency. |
| **Spring Boot** | 3.5.7 | Core framework for building robust, scalable REST APIs. |
| **Spring Data JPA** / **Hibernate** | Latest | ORM layer used for complex data modeling, demonstrated via **Many-to-Many relationships** (`Area` to `LootType`). |
| **PostgreSQL** | 15 | Relational database (SQL) used for persistence, selected for data integrity and complex querying capabilities. |
| **Flyway** | Latest | **Database Migration Tool** used to version control the database schema and load static reference data (e.g., Maps, Loot Types) reliably. |
| **Project Lombok** | Latest | Used to reduce Java boilerplate code. |

### Frontend (User Interface)

| Technology | Version | Purpose & Feature Showcase |
| :--- | :--- |
| **React** | Latest |
| **TypeScript** | Latest |
| **Vite** | Latest |

### Infrastructure / DevOps

| Tool | Purpose & Feature Showcase |
| :--- | :--- |
| **Docker Compose** | Used to provision the local PostgreSQL database, ensuring environment parity and quick setup. |
| **Spring Boot Docker Compose Support** | Automatically manages the PostgreSQL container lifecycle (start/stop) with the application. |

---

## Architectural Design

The application’s architecture is structured to highlight key engineering competencies.

### 1. Data Ingestion Pipeline (ETL)

This project features a custom data synchronization service, demonstrating integration with external, real-world services.

* **Extraction:** Data is sourced from the Metaforge ARC Raiders API (https://metaforge.app/arc-raiders).
* **Tool:** The modern **`RestClient`** is used to handle synchronous, paginated API calls, demonstrating knowledge of iterating through hundreds of records across multiple pages.
* **Transformation/Load:** The service maps complex, nested JSON objects (using **Records/DTOs**) into a clean, normalized relational model (JPA Entities).

### 2. Domain Model & Core Logic

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

---

## Quick Start (Developer Setup)

To run the application, you only need **Java 21** and **Docker Desktop** installed.

### Prerequisites

* Java Development Kit (JDK) 21
* Docker Desktop (Running)

### Setup & Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/paul-anthony-oneill/arc-raiders-planner.git
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

## Future Improvements

* **Visual Map:** Integrate the X/Y coordinates in the database with a React Leaflet or similar library to show a visual, interactive map of recommended zones.
* **Multiple Item Targets & Route Planning:** Implement a `routing` algorithm to suggest routes through multiple looting zones, allowing the user to set multiple Item targets
    * **Optimised Suggestions for Multiple Item Targets:** Rank maps/route suggestions where multiple item types are targeted based on proximity. E.g. if Electrical and ARC items are targeted, rank suggestions higher if a map has a zone with both Electrical and ARC loot types. If none exist on any map, then rank maps based on those with Electrical/ARC zones closest together.
* **Expand Beyond Items:** Use the Metaforge Data to integrate other targets for a raid, such as quest objectives or ARC targets. E.g. Complete `The Major's Footlocker` quest, find a `Rocketeer Driver` and a `Rusted Metal Gear` -> Suggest the map to complete that Quest and route to a known Rocketeer spawn and Industrial Loot Zone

## Stretch Goals & Future Improvements

* **Authentication:** Implement Spring Security (OAuth2/JWT) to allow user customization (e.g., saving favorite routes).

---
