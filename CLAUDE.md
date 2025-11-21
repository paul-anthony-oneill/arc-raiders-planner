# ARC Raiders Loot Planner - Project Context

## Current Status
- **Phase**: MVP Development
- **Next Feature**: Implement suggestion modes in backend logic
- **Following Feature**: Add ARC enemy targeting capability
- **Long-Term Goals**: Add Quests, Crafting Recipe components and Workbench Upgrades as targets, target prioritization
- **Tech Stack**: Spring Boot 3.5.7 + React 19 + PostgreSQL 15

## Architecture Overview
```
Frontend (React/TS) → REST API → Spring Boot → PostgreSQL
                                      ↓
                              Metaforge API (External)
```

## Core Domain
- **Item**: Game items with metadata (rarity, value, stats)
- **GameMap**: Playable maps with loot zones
- **Area**: Specific loot zones with X/Y coordinates
- **LootType**: Categories (Industrial, Mechanical, ARC, etc.)
- **Relationships**: Area ↔ LootType (Many-to-Many)

## Key Algorithms
1. **Map Recommendation**: JPQL query joining GameMap→Area→LootType, counting matches
2. **Data Sync**: ETL pipeline from Metaforge API → DTOs → JPA entities

## Development Priorities
1. Complete suggestion modes implementation
2. Add comprehensive unit & E2E test coverage
3. Implement API response caching for Metaforge
4. Add OpenAPI/JavaDoc documentation
5. Optimize database queries (N+1 prevention)

## Quick Commands
```bash
# Backend
./mvnw spring-boot:run    # Starts app + PostgreSQL via Docker
./mvnw test               # Run tests
./mvnw clean package      # Build JAR

# Frontend
cd frontend
npm run dev               # Dev server (http://localhost:5173)
npm run build            # Production build
```

## File Structure
- `/src/main/java/com/pauloneill/arcraidersplanner/` - Backend code
- `/frontend/` - React application
- `/src/main/resources/db/migration/` - Flyway migrations
- `compose.yaml` - PostgreSQL container config

# Documentation Standards

## JavaDoc Requirements
```java
/**
 * Recommends optimal maps based on item loot type.
 * WHY: Players need efficient farming routes to minimize time spent
 * 
 * @param itemName Case-insensitive item name
 * @return Sorted list of maps by loot zone count
 * @throws ItemNotFoundException if item doesn't exist
 */
public List<MapRecommendationDto> recommendMapsByItemName(String itemName) {
```

## OpenAPI/Swagger
```java
@Operation(summary = "Search items by name")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Items found"),
    @ApiResponse(responseCode = "400", description = "Invalid search term")
})
@GetMapping("/items")
```

## Frontend Documentation
```tsx
/**
 * Displays loot planning interface with map recommendations.
 * WHY: Core UX for helping players optimize their raids
 */
export const Planner: React.FC = () => {
```

## Comment Philosophy
- Only use comments for non-obvious code logic
- Document WHY (intent), not WHAT (syntax)
- Business logic explanations in services
- Complex algorithm rationale
- API contract details

## TODO: Documentation Tasks
1. Add JavaDoc to all public methods
2. Configure Swagger UI at `/swagger-ui`
3. Document React component props with TSDoc
4. Create API usage examples
5. Add decision log for architecture choices

## README Sections to Maintain
- Quick Start guide
- Architecture decisions
- API endpoint reference
- Environment setup
- Troubleshooting common issues

