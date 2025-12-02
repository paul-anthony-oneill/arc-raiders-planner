# ARC Raiders Loot Planner

A web application designed to optimize player efficiency and ease the decision making process for which map and area to loot in the extraction shooter, **ARC Raiders**. This tool suggests an **optimal map and location** based on the required item and it's metadata, demonstrating proficiency in modern **Java Backend Architecture** and **Full-Stack Development**.

---

## Technical Overview

This project is built using a modern, containerized, three-tier architecture, emphasizing industry-standard practices and tools.

### Backend (API/Logic)

| Technology                          | Version  | Purpose & Feature Showcase                                                                                                                |
| :---------------------------------- | :------- | :---------------------------------------------------------------------------------------------------------------------------------------- |
| **Java**                            | 21 (LTS) | Used for application logic and concurrency.                                                                                               |
| **Spring Boot**                     | 3.5.7    | Core framework for building robust, scalable REST APIs.                                                                                   |
| **Spring Data JPA** / **Hibernate** | Latest   | ORM layer used for complex data modeling, demonstrated via **Many-to-Many relationships** (`Area` to `LootType`).                         |
| **PostgreSQL**                      | 15       | Relational database (SQL) used for persistence, selected for data integrity and complex querying capabilities.                            |
| **Flyway**                          | Latest   | **Database Migration Tool** used to version control the database schema and load static reference data (e.g., Maps, Loot Types) reliably. |
| **Project Lombok**                  | Latest   | Used to reduce Java boilerplate code.                                                                                                     |

### Frontend (User Interface)

| Technology     | Version | Purpose & Feature Showcase |
| :------------- | :------ | -------------------------- |
| **React**      | Latest  |
| **TypeScript** | Latest  |
| **Vite**       | Latest  |

### Infrastructure / DevOps

| Tool                                   | Purpose & Feature Showcase                                                                    |
| :------------------------------------- | :-------------------------------------------------------------------------------------------- |
| **Docker Compose**                     | Used to provision the local PostgreSQL database, ensuring environment parity and quick setup. |
| **Spring Boot Docker Compose Support** | Automatically manages the PostgreSQL container lifecycle (start/stop) with the application.   |

---

## Architectural Design

The application’s architecture is structured to highlight key engineering competencies.

### 1. Data Ingestion Pipeline (ETL)

This project features a custom data synchronization service, demonstrating integration with external, real-world services.

- **Extraction:** Data is sourced from the Metaforge ARC Raiders API (https://metaforge.app/arc-raiders).
- **Tool:** The modern **`RestClient`** is used to handle synchronous, paginated API calls, demonstrating knowledge of iterating through hundreds of records across multiple pages.
- **Transformation/Load:** The service maps complex, nested JSON objects (using **Records/DTOs**) into a clean, normalized relational model (JPA Entities).

### 2. Domain Model & Core Logic

- **Core Entities:** `Item`, `GameMap`, `Area` (Loot Zone), `LootType`, `ContainerType`, and `MarkerGroup`.
- **Relationship Showcase:** An `Area` (Loot Zone) maintains a **Many-to-Many relationship** with multiple `LootType` categories (e.g., a single zone can spawn both 'Industrial' and 'Mechanical' loot).
- **Planner Algorithm (JPQL):** The core logic is executed via a single, highly optimized **JPQL query** that performs a `JOIN` across three tables and aggregates the results using a `COUNT` and `GROUP BY` clause. This returns a prioritized list of maps for any given item type.
- **Container Grouping (DBSCAN):** Implements a **spatial clustering algorithm** that automatically groups nearby container spawns into consolidated zones, reducing route complexity and improving navigation clarity.

---

## Key Endpoints & Functionality

The API provides comprehensive endpoints for raid planning, item search, and enemy targeting.

| Endpoint                                    | Method | Functionality Showcase                                                                                                                                           |
| :------------------------------------------ | :----- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/items?search={term}`                  | `GET`  | **Search Index:** Fetches items with partial, case-insensitive search (`LIKE` query).                                                                            |
| `/api/items/recommendation?itemName={name}` | `GET`  | **Legacy Endpoint:** Basic map recommendation by single item.                                                                                                    |
| `/api/planner/generate-route`               | `POST` | **Advanced Route Planning:** Accepts items, enemies, containers, and routing profile. Returns optimized routes using **multi-start nearest-neighbor + 2-opt TSP algorithm**. |
| `/api/enemies?search={term}`                | `GET`  | **Enemy Search:** Searches ARC enemies by name (e.g., "Sentinel", "Guardian").                                                                                   |
| `/api/enemies/{id}`                         | `GET`  | **Enemy Details:** Fetches specific enemy spawn details by UUID.                                                                                                 |
| `/api/enemies/types`                        | `GET`  | **Enemy Categories:** Returns distinct enemy type classifications.                                                                                               |
| `/api/containers`                           | `GET`  | **Container Types:** Lists all container types (Red Lockers, Raider Caches, Weapon Crates, etc.).                                                                |
| `/api/containers?search={term}`             | `GET`  | **Container Search:** Search for specific container types by name.                                                                                                |

---

## Quick Start (Developer Setup)

To run the application, you only need **Java 21** and **Docker Desktop** installed.

### Prerequisites

- Java Development Kit (JDK) 21
- Docker Desktop (Running)

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

## Completed Features

- ✅ **Multiple Item Targets & Route Planning:** Multi-start nearest-neighbor + 2-opt optimization for efficient multi-zone routes
- ✅ **Routing Profiles:** Four specialized modes (PURE_SCAVENGER, EASY_EXFIL, AVOID_PVP, SAFE_EXFIL)
- ✅ **ARC Enemy Targeting:** Search and target specific enemy spawns with route integration
- ✅ **Enemy Proximity Scoring:** Routes automatically optimize for passing near target enemies
- ✅ **Container Targeting:** Target specific container types (Red Lockers, Raider Caches, Weapon Crates, etc.)
- ✅ **Marker Grouping & Zones:** Automatic clustering of nearby container spawns into consolidated zones using DBSCAN algorithm
- ✅ **Mixed Targeting:** Combine items, enemies, and containers in a single optimized route

## Future Improvements

- **Quest Integration:** Add quest objectives as route targets (e.g., "The Major's Footlocker" quest)
- **Crafting Components:** Integrate workbench upgrades and crafting recipe components as plannable targets
- **Target Prioritization:** Allow users to weight importance of different targets (items vs enemies vs containers vs quests)
- **Advanced Clustering Configuration:** Allow users to customize grouping parameters (distance threshold, minimum cluster size)

## Stretch Goals & Future Improvements

- **Authentication:** Implement Spring Security (OAuth2/JWT) to allow user customization (e.g., saving favorite routes).

---
