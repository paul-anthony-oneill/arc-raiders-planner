# 🎯 ARC Raiders Loot Planner

> **Full-stack route optimization tool for ARC Raiders extraction shooter**
> Intelligent raid planning with advanced algorithms, spatial clustering, and real-time data integration

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

---

## 📸 Screenshots

> **Note:** Add screenshots here showing the tactical planner interface, route visualization, and key features.

<details>
<summary>🖼️ Screenshot Placeholders (Click to expand)</summary>

- **Main Tactical Planner Interface** - 3-panel layout with item selection, details, and map
- **Route Calculation** - Optimized multi-waypoint routes with different profiles
- **Container Grouping Visualization** - DBSCAN clustering of nearby container spawns
- **Zone Highlighting** - Visual feedback showing item spawn locations
- **Keyboard Shortcuts Modal** - Accessibility features and power-user shortcuts

</details>

---

## 🎯 What It Does

This web application **optimizes player efficiency** in ARC Raiders (extraction shooter) by calculating intelligent loot routes across game maps. Players can:

- 🔍 **Search for items, enemies, and containers** they need to find
- 🗺️ **Generate optimized routes** using multiple routing strategies (scavenging, safe extraction, PvP avoidance, etc.)
- 🎯 **Mix objectives** - combine loot, enemy hunts, and container raids in one route
- 📊 **Visualize spawn zones** with automatic spatial clustering of nearby targets

---

## 💡 Why I Built This (Skills Demonstrated)

This project showcases enterprise-level full-stack development skills applicable to real-world software engineering:

| **Skill Area** | **Implementation** |
|----------------|-------------------|
| **Algorithm Design** | Multi-start nearest-neighbor + 2-opt TSP optimization for route planning |
| **Spatial Computing** | DBSCAN clustering algorithm for grouping nearby container spawns |
| **Backend Architecture** | Clean layered architecture (Controller → Service → Repository) with Spring Boot |
| **Data Modeling** | Complex JPA relationships (Many-to-Many), optimized JPQL queries to prevent N+1 |
| **ETL Pipeline** | Synchronization service fetching 500+ items from external REST API with pagination |
| **Database Design** | PostgreSQL with Flyway migrations, normalized schema with referential integrity |
| **Frontend Development** | React 19 + TypeScript with TanStack Router, lazy loading, and responsive design |
| **Testing** | 77 comprehensive JUnit tests covering services, repositories, and controllers |
| **UX Polish** | Keyboard shortcuts, tooltips, loading states, accessibility mode, mobile responsive |

---

## 🏆 Key Technical Achievements

### 1. **Intelligent Route Optimization**
- **Algorithm:** Multi-start nearest-neighbor with 2-opt optimization (TSP solver)
- **Routing Profiles:** 4 distinct strategies (PURE_SCAVENGER, EASY_EXFIL, AVOID_PVP, SAFE_EXFIL)
- **Scoring System:** Multi-criteria evaluation considering loot zones, extraction points, danger zones, and enemy proximity

### 2. **Spatial Clustering (DBSCAN)**
- **Purpose:** Automatically groups nearby container spawns (within 100m) into consolidated zones
- **Impact:** Reduces route complexity, improves navigation clarity
- **Implementation:** Custom `GeometryService` with configurable epsilon and minPoints parameters

### 3. **Complex Data Integration**
- **External API:** Metaforge ARC Raiders API (500+ items, maps, loot zones, enemies, containers)
- **ETL Pipeline:** `MetaforgeSyncService` handles paginated API calls, JSON transformation, and database persistence
- **Data Model:** 9 JPA entities with Many-to-Many relationships and spatial coordinates

### 4. **Advanced Query Optimization**
- **JPQL Queries:** Single query joining 3 tables with aggregation (MapAreaRepository)
- **N+1 Prevention:** Strategic use of `JOIN FETCH` and `@EntityGraph`
- **Custom Repositories:** Type-safe query methods with Spring Data JPA

---

## 🛠️ Technology Stack

### **Backend**
| Technology | Purpose |
|------------|---------|
| **Java 21** | Modern language features (Records, Pattern Matching, Virtual Threads) |
| **Spring Boot 3.5.7** | Enterprise framework for REST APIs, dependency injection, and autoconfiguration |
| **Spring Data JPA / Hibernate** | ORM with complex relationships (Many-to-Many Area ↔ LootType) |
| **PostgreSQL 15** | Relational database with spatial data (lat/lng coordinates) |
| **Flyway** | Database version control and migration management |
| **RestClient** | Modern HTTP client for external API integration |
| **Lombok** | Boilerplate reduction (@RequiredArgsConstructor, @Data, etc.) |

### **Frontend**
| Technology | Purpose |
|------------|---------|
| **React 19** | Component-based UI with latest features (Suspense, lazy loading) |
| **TypeScript 5** | Type-safe frontend development |
| **TanStack Router** | Type-safe routing with file-based structure |
| **Vite** | Lightning-fast build tool and dev server |
| **Tailwind CSS** | Utility-first styling with custom retro/CRT theme |

### **DevOps & Infrastructure**
| Technology | Purpose |
|------------|---------|
| **Docker Compose** | PostgreSQL containerization for local development |
| **Maven** | Dependency management and build automation |
| **JUnit 5 + Mockito** | Comprehensive backend testing (77 tests) |

---

## 📚 API Endpoints

### **Item Management**
- `GET /api/items?search={term}` - Search items by name (case-insensitive, partial match)
- `GET /api/items/{id}` - Get detailed item info with crafting context (recipes, usage, drops)

### **Route Planning**
- `POST /api/planner/generate-route` - Generate optimized routes
  - **Body:** `PlannerRequestDto` (items, enemies, containers, routing profile, Raider Key status)
  - **Returns:** Ranked list of `PlannerResponse` with waypoints, scores, and extraction points

### **Enemy Targeting**
- `GET /api/enemies?search={term}` - Search ARC enemies (e.g., "Sentinel", "Guardian")
- `GET /api/enemies/{id}` - Get enemy spawn details by UUID
- `GET /api/enemies/types` - List distinct enemy classifications

### **Container Targeting**
- `GET /api/containers` - List all container types
- `GET /api/containers?search={term}` - Search containers (e.g., "Red Locker", "Raider Cache")

### **Zone Highlighting**
- `GET /api/areas/by-map-and-item?mapName={map}&itemName={item}` - Get zones containing item

---

## 🚀 Quick Start

### **Prerequisites**
- **Java 21** (LTS) - [Download](https://adoptium.net/)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop/)
- **Node.js 18+** (for frontend) - [Download](https://nodejs.org/)

### **Setup**

```bash
# 1. Clone the repository
git clone https://github.com/paul-anthony-oneill/arc-raiders-planner.git
cd arc-raiders-planner

# 2. Start Backend (Spring Boot auto-starts PostgreSQL via Docker Compose)
./mvnw spring-boot:run

# 3. Start Frontend (in new terminal)
cd frontend
npm install
npm run dev

# 4. Open browser
# Backend: http://localhost:8080
# Frontend: http://localhost:5173
```

### **First Run**
- ✅ Flyway automatically creates database schema
- ✅ `MetaforgeSyncService` syncs 500+ items from external API
- ✅ Static data (maps, loot types) loaded from migration scripts

---

## ✨ Features

### **Core Functionality**
- ✅ Multi-item route planning with optimized waypoint ordering
- ✅ Four routing profiles (Scavenger, Easy Exfil, Avoid PvP, Safe Exfil)
- ✅ ARC enemy targeting integrated into route optimization
- ✅ Container targeting with automatic spatial clustering
- ✅ Mixed objective routing (combine items + enemies + containers)
- ✅ Zone highlighting showing where items spawn

### **UX Polish**
- ✅ Unified 3-panel tactical planner interface
- ✅ Keyboard shortcuts with help modal (Press `?`)
- ✅ Accessibility mode (Ctrl+A removes CRT effects)
- ✅ Mobile responsive design with tab navigation
- ✅ Loading skeletons and smooth transitions
- ✅ Tooltips on all interactive elements
- ✅ Route export (JSON), sharing (URL), and printing

### **Technical Features**
- ✅ Lazy loading for route visualization (performance optimization)
- ✅ Session-based state management (priority/ongoing targets)
- ✅ Advanced filtering (rarity, type, loot zone)
- ✅ 77 passing backend tests with comprehensive coverage

---

## 📊 Architecture

```
┌─────────────────────────────────────────────────────────┐
│              React Frontend (Port 5173)                 │
│   • TanStack Router • TypeScript • Tailwind CSS        │
│   • 3-Panel Layout: Objectives → Details → Map         │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/REST
                     ↓
┌─────────────────────────────────────────────────────────┐
│         Spring Boot Backend (Port 8080)                 │
│  ┌──────────────┬──────────────┬─────────────────────┐ │
│  │ Controllers  │  Services    │  Repositories       │ │
│  │ (REST API)   │  (Business)  │  (Data Access)      │ │
│  └──────────────┴──────────────┴─────────────────────┘ │
│   • PlannerService: TSP solver + DBSCAN clustering      │
│   • MetaforgeSyncService: External API ETL              │
│   • GeometryService: Spatial calculations               │
└────────────────────┬────────────────────────────────────┘
                     │ JDBC
                     ↓
┌─────────────────────────────────────────────────────────┐
│         PostgreSQL Database (Docker Container)          │
│   • 9 tables with JPA relationships                     │
│   • Flyway migrations for version control               │
│   • Spatial data (lat/lng coordinates)                  │
└─────────────────────────────────────────────────────────┘
                     ↑
                     │ HTTP
┌─────────────────────────────────────────────────────────┐
│           Metaforge ARC Raiders API                     │
│   • 500+ items • Maps • Loot zones • Enemies           │
└─────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing

```bash
# Run all backend tests (77 tests)
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Run frontend build (TypeScript check)
cd frontend && npm run build
```

**Test Coverage:**
- ✅ Service layer (business logic, algorithms, clustering)
- ✅ Repository layer (custom queries, JPQL)
- ✅ Controller layer (REST endpoints, validation)
- ✅ Integration tests (full Spring context)

---

## 📝 CV-Ready Bullet Points

Copy these for your CV/resume:

- **Built full-stack extraction shooter route planner** using Spring Boot, React, TypeScript, and PostgreSQL
- **Implemented TSP solver** with multi-start nearest-neighbor + 2-opt optimization for multi-waypoint route calculation
- **Developed DBSCAN spatial clustering algorithm** to automatically group nearby container spawns into consolidated zones
- **Designed ETL pipeline** syncing 500+ items from external REST API with pagination and JSON transformation
- **Optimized database queries** using JPQL with JOIN FETCH to eliminate N+1 problems in Many-to-Many relationships
- **Achieved 77 passing backend tests** covering services, repositories, and controllers with JUnit 5 and Mockito
- **Implemented 4 routing profiles** (scavenging, extraction, PvP avoidance) with multi-criteria scoring system
- **Built responsive React UI** with lazy loading, keyboard shortcuts, accessibility features, and mobile support

---

## 🔮 Future Enhancements

- **Quest Integration** - Add quest objectives as route targets
- **Crafting Tree Visualization** - Show item dependencies for workbench upgrades
- **Target Prioritization** - Weight importance of different objectives
- **Live Deployment** - Deploy to cloud platform (Railway/Render + Vercel)
- **Authentication** - Spring Security with OAuth2/JWT for saved routes
- **Route Sharing** - Community route database with ratings

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Paul O'Neill**

- GitHub: [@paul-anthony-oneill](https://github.com/paul-anthony-oneill)
- Portfolio: [Add your portfolio link]

---

## 🙏 Acknowledgments

- Game data provided by [Metaforge ARC Raiders Wiki](https://metaforge.app/arc-raiders)
- Built as a portfolio project to demonstrate full-stack development skills
- Inspired by extraction shooter route planning tools (Tarkov, Hunt: Showdown)

---

⭐ **If you find this project useful or impressive, please consider giving it a star!**
