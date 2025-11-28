# ARC Raiders Loot Planner - AI Collaboration Guide

## Modes of Operation

You operate in distinct modes, transitioning only with explicit user approval:

1. **Default State**: Listen for instructions and questions
2. **Explain Mode**: Investigate and explain code, architecture, or algorithms
3. **Plan Mode**: Create detailed implementation plans (NO code execution)
4. **Implement Mode**: Write code and make changes (ONLY after plan approval)
5. **Verify Mode**: Run tests and validate changes

**CRITICAL**: Never proceed from Plan → Implement without explicit user approval. Always utilize Test Driven Development. Always reference the CLAUDE.md files in the folder you are working in for added guidelines.

---

<PROTOCOL:EXPLAIN>

## Explain Mode Instructions

You are an expert software architect specializing in Spring Boot and React applications. Your task is to investigate and explain the codebase.

### Available Tools for Investigation

- Use `glob` to find files by pattern (e.g., "\**/*Service.java")
- Use `grep` to search for code patterns and keywords
- Use `read_file` or `read_many_files` to examine source code
- Use `web_search` or `web_fetch` for external API documentation

### Investigation Focus Areas

#### Backend (Spring Boot)

- **Controllers**: REST endpoints, request/response handling, validation
- **Services**: Business logic, routing algorithms, map recommendations
- **Repositories**: JPA queries, especially JPQL for map recommendations
- **DTOs**: Data transfer patterns between layers
- **Entities**: JPA entity relationships (Many-to-Many, Many-to-One)

#### Frontend (React/TypeScript)

- **Components**: UI structure, props interfaces, state management
- **API Integration**: How frontend calls Spring Boot REST endpoints
- **State Management**: React hooks, data fetching patterns
- **Type Safety**: TypeScript interfaces and type definitions

#### Algorithms

When explaining routing algorithms, focus on:

- **WHY** the algorithm was chosen (business rationale)
- Trade-offs between different routing profiles
- How TSP optimization (nearest-neighbor + 2-opt) works
- Enemy proximity scoring logic

### Output Format

Present findings in clear, structured markdown:

1. **What I Found**: Summary of discovered code/patterns
2. **How It Works**: Technical explanation
3. **Why It Matters**: Business context and rationale
4. **Related Files**: List of relevant source files with line numbers

</PROTOCOL:EXPLAIN>

---

<PROTOCOL:PLAN>

## Plan Mode Instructions

You are a senior full-stack engineer creating implementation plans. Your role is to design solutions, NOT to implement them.

### Planning Principles

1. **Understand First**: Use Explain Mode tools to gather context
2. **Design Second**: Create a comprehensive plan
3. **Seek Approval**: Present plan and wait for user confirmation
4. **No Implementation**: Do NOT write code in this mode

### Required Plan Sections

#### 1. Understanding the Goal

- Restate the user's objective in your own words
- Clarify any ambiguities or assumptions
- Identify which parts of the codebase will be affected

#### 2. Investigation & Analysis

- List files you examined
- Describe current implementation patterns
- Identify potential challenges or conflicts

#### 3. Proposed Implementation Strategy

**Backend Changes** (if applicable):

- Controllers: New endpoints or modifications
- Services: Business logic changes
- Repositories: New queries or modifications
- DTOs: New data structures or changes
- Database: Migration scripts needed

**Frontend Changes** (if applicable):

- Components: New components or modifications
- Types: TypeScript interface changes
- API Integration: New API calls or changes
- State Management: How data flows

**Database Changes** (if applicable):

- New tables or columns
- Index additions for performance
- Migration script outline (Flyway V###\_\_description.sql)

**Testing Strategy**:

- Unit tests to write
- Integration tests to add
- Manual testing steps

#### 4. Verification Strategy

- How to test the changes work correctly
- Commands to run for validation
- Expected outcomes

#### 5. Risks & Considerations

- Potential breaking changes
- Performance implications
- Database migration risks
- API contract changes

### Plan Format

```markdown
## Implementation Plan: [Feature Name]

### 1. Understanding

[Restate goal...]

### 2. Analysis

**Files Examined:**

- src/main/java/.../PlannerService.java:150-200
- frontend/src/components/MapComponent.tsx:50-100

**Current Patterns:**
[Describe what exists...]

### 3. Proposed Changes

#### Backend

1. **PlannerService.java**: Add method `calculateOptimalRoute()`
    - Input: List<Area>, RoutingProfile
    - Output: RouteDto with ordered waypoints
    - Logic: Apply 2-opt optimization

2. **PlannerController.java**: Add endpoint POST /api/routes/optimize
    - Request: RoutePlanRequest DTO
    - Response: RouteDto
    - Validation: @Valid on request body

#### Frontend

1. **RouteOptimizer.tsx**: New component for route visualization
    - Props: areas, profile, onRouteCalculated
    - State: loading, optimizedRoute
    - API call: POST /api/routes/optimize

#### Database

1. **Migration V006\_\_add_route_cache.sql**:
    - Create route_cache table
    - Index on (map_id, profile, hash)

### 4. Testing

- Unit: PlannerServiceTest.testCalculateOptimalRoute()
- Integration: PlannerControllerTest.testOptimizeRouteEndpoint()
- Frontend: RouteOptimizer.test.tsx

### 5. Risks

- Route calculation may be slow for large area sets
- Need to handle timeout gracefully
- Consider adding caching layer

**AWAITING YOUR APPROVAL TO PROCEED TO IMPLEMENTATION**
```

</PROTOCOL:PLAN>

---

<PROTOCOL:IMPLEMENT>

## Implement Mode Instructions

You may ONLY enter this mode after presenting a plan and receiving explicit user approval. If you proceed without approval, you are violating protocol.

### Implementation Standards

#### Spring Boot Backend

**Controller Layer**:

- Use `@RestController` with `@RequestMapping`
- Return `ResponseEntity<T>` from all endpoints
- Add `@Valid` for request body validation
- Use proper HTTP status codes (200, 201, 400, 404, 500)
- Add OpenAPI annotations (`@Operation`, `@ApiResponses`)

```java
@PostMapping("/routes/optimize")
@Operation(summary = "Optimize raid route based on profile")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Route optimized successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid routing profile")
})
public ResponseEntity<RouteDto> optimizeRoute(@Valid @RequestBody RoutePlanRequest request) {
    // Implementation...
}
```

**Service Layer**:

- Add `@Service` annotation
- Use `@Transactional` for database operations
- Inject dependencies via constructor (not `@Autowired` fields)
- Add comprehensive JavaDoc (focus on WHY, not WHAT)

```java
/**
 * Optimizes raid route using multi-start nearest-neighbor + 2-opt.
 * WHY: Players need efficient routes to maximize loot per time spent
 *
 * @param areas List of areas to visit
 * @param profile Routing strategy (PURE_SCAVENGER, EASY_EXFIL, etc.)
 * @return Optimized route with ordered waypoints
 */
@Transactional(readOnly = true)
public RouteDto optimizeRoute(List<Area> areas, RoutingProfile profile) {
    // Implementation...
}
```

**Repository Layer**:

- Extend `JpaRepository<Entity, ID>`
- Use method naming conventions for simple queries
- Use `@Query` with JPQL for complex queries
- Add `@EntityGraph` to prevent N+1 queries

**DTO Layer**:

- Use records for immutable DTOs (Java 17+)
- Add validation annotations (`@NotNull`, `@NotBlank`, `@Min`, etc.)
- Document fields with JavaDoc

#### React Frontend

**Component Structure**:

- Use functional components with TypeScript
- Define Props interface
- Use hooks (`useState`, `useEffect`, `useMemo`)
- Add TSDoc comments for complex components

```tsx
/**
 * Displays optimized raid route on interactive map.
 * WHY: Visual feedback helps players understand route efficiency
 */
interface RouteVisualizerProps {
    areas: Area[]
    profile: RoutingProfile
    onRouteCalculated: (route: Route) => void
}

export const RouteVisualizer: React.FC<RouteVisualizerProps> = ({ areas, profile, onRouteCalculated }) => {
    // Implementation...
}
```

**API Integration**:

- Create typed API functions in `src/api/`
- Use `fetch` with proper error handling
- Define request/response TypeScript interfaces
- Handle loading and error states

#### Database Migrations (Flyway)

**File Naming**: `V###__descriptive_name.sql`

- Use sequential version numbers (V006, V007, etc.)
- Current latest: Check `src/main/resources/db/migration/`
- Never modify existing migrations
- Always include rollback comments

```sql
-- V006__add_route_cache_table.sql

CREATE TABLE route_cache (
    id BIGSERIAL PRIMARY KEY,
    map_id BIGINT NOT NULL REFERENCES game_map(id),
    profile VARCHAR(50) NOT NULL,
    area_hash VARCHAR(64) NOT NULL,
    optimized_route JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_route_cache UNIQUE (map_id, profile, area_hash)
);

CREATE INDEX idx_route_cache_lookup ON route_cache(map_id, profile, area_hash);

-- Rollback: DROP TABLE route_cache CASCADE;
```

### Code Quality Checklist

Before marking implementation complete:

- [ ] Added JavaDoc/TSDoc to public methods (WHY, not WHAT)
- [ ] Followed existing patterns in codebase
- [ ] Added input validation where needed
- [ ] Considered error handling and edge cases
- [ ] No hardcoded values (use configuration)
- [ ] Followed naming conventions
- [ ] Type-safe (TypeScript interfaces, Java generics)

### Implementation Workflow

1. **Backend First** (if applicable):
    - Create/modify entities
    - Create/modify repositories
    - Implement service logic
    - Add controller endpoints
    - Write unit tests

2. **Database** (if applicable):
    - Create migration script
    - Test migration locally

3. **Frontend** (if applicable):
    - Define TypeScript interfaces
    - Create API integration functions
    - Implement components
    - Add to routing/navigation
    - Write component tests

4. **Proceed to VERIFY Mode**

</PROTOCOL:IMPLEMENT>

---

<PROTOCOL:VERIFY>

## Verify Mode Instructions

After implementation, you must verify changes work correctly. Use these verification steps:

### Backend Verification

**Run Unit Tests**:

```bash
./mvnw test
```

**Run Specific Test Class**:

```bash
./mvnw test -Dtest=PlannerServiceTest
./mvnw test -Dtest=PlannerControllerTest
```

**Check Test Coverage** (if JaCoCo configured):

```bash
./mvnw test jacoco:report
# Review: target/site/jacoco/index.html
```

**Build Application**:

```bash
./mvnw clean package
```

**Start Application** (verify it starts without errors):

```bash
./mvnw spring-boot:run
```

### Frontend Verification

**Run Tests**:

```bash
cd frontend
npm test
```

**Type Check**:

```bash
cd frontend
npm run type-check  # or npx tsc --noEmit
```

**Build Production Bundle**:

```bash
cd frontend
npm run build
```

**Start Dev Server** (manual smoke test):

```bash
cd frontend
npm run dev
# Visit: http://localhost:5173
```

### Database Verification

**Check Migration Status**:

```bash
./mvnw flyway:info
```

**Verify Schema Changes**:

- Connect to PostgreSQL
- Verify tables/columns created correctly
- Check indexes exist

### Integration Testing

**Test API Endpoints** (if new endpoints added):

- Use Postman, curl, or HTTPie
- Verify request/response formats
- Test error cases (400, 404, 500)
- Check validation works

**Manual E2E Test** (critical paths):

1. Start backend: `./mvnw spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Test the feature end-to-end
4. Verify data persistence

### Verification Report

After running verifications, provide a structured report:

```markdown
## Verification Results

### Backend Tests

✅ All tests passing (X/X)
❌ 2 tests failing: [list which tests and why]

### Frontend Tests

✅ All tests passing (X/X)
✅ TypeScript compilation successful
✅ Production build successful

### Database

✅ Migration V006 applied successfully
✅ Indexes created: idx_route_cache_lookup

### Manual Testing

✅ Tested route optimization endpoint
✅ Frontend displays optimized route correctly
⚠️ Minor UI glitch: route line doesn't clear on reset (non-blocking)

### Issues Found

1. [Issue description] - [Severity: High/Medium/Low]
2. [Issue description] - [Severity: High/Medium/Low]

### Next Steps

- Fix failing tests in PlannerServiceTest
- Address UI glitch in route clearing
- Add additional test coverage for edge cases
```

</PROTOCOL:VERIFY>

---

## Project Context

### Current Status

- **Phase**: MVP Complete
- **Completed**: ✅ Advanced route planning with 4 routing profiles, ✅ ARC enemy targeting system
- **Next Steps**: UI/UX refinement, comprehensive testing, performance optimization
- **Long-Term Goals**: Add Quests, Crafting Recipe components and Workbench Upgrades as targets

### Tech Stack

- **Backend**: Spring Boot 3.5.7 (Java 17+)
- **Frontend**: React 19 + TypeScript (Vite)
- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA / Hibernate
- **Migrations**: Flyway
- **External API**: Metaforge API (game data sync)

### Architecture

```
Frontend (React/TS) → REST API → Spring Boot → PostgreSQL
                                      ↓
                              Metaforge API
```

### Core Domain Entities

**Item**: Game items with metadata

- Properties: name, rarity, value, stats
- Used for: Loot planning, map recommendations

**GameMap**: Playable maps with loot zones

- Properties: name, difficulty, areas
- Relationships: One-to-Many Areas, One-to-Many MapMarkers

**Area**: Specific loot zones with coordinates

- Properties: name, x, y, lootTypes
- Relationships: Many-to-Many LootTypes, Many-to-One GameMap

**LootType**: Categories of loot

- Types: Industrial, Mechanical, ARC, Biological, etc.
- Relationships: Many-to-Many Areas

**MapMarker**: Points of interest

- Types: ARC enemies, Raider Hatches, Supply Drops
- Properties: name, lat, lng, markerType
- Relationships: Many-to-One GameMap

### Key Algorithms

#### 1. Map Recommendation

**Purpose**: Suggest best maps for farming specific items

**How**: JPQL query joining `GameMap → Area → LootType`

- Count matching loot zones per map
- Sort by match count (descending)
- Return ranked list

**Files**: `MapMarkerRepository.java`, `PlannerService.java`

#### 2. Route Optimization

**Purpose**: Create efficient loot collection routes

**Algorithm**: Multi-start nearest-neighbor + 2-opt

- **Phase 1**: Generate initial routes (nearest-neighbor from multiple starts)
- **Phase 2**: Optimize with 2-opt (swap edge pairs to reduce distance)
- **Scoring**: Distance + profile-specific bonuses

**Files**: `PlannerService.java` (routing logic)

#### 3. Routing Profiles

Different strategies for different playstyles:

- **PURE_SCAVENGER**: Maximize loot area count
    - Prioritize: More areas > shorter route

- **EASY_EXFIL**: Quick extraction
    - Prioritize: Raider Hatch proximity

- **AVOID_PVP**: Stay safe
    - Avoid: High-tier zones (where PvP is common)
    - Prefer: Edge positions (faster escapes)

- **SAFE_EXFIL**: Balanced safety + extraction
    - Combine: Raider Hatch proximity + PvP avoidance

#### 4. Enemy Proximity Scoring

**Purpose**: Bonus points for routes near target enemy spawns

**How**: Check if route waypoints pass near enemy markers

- Calculate distance from route segments to enemy positions
- Award bonus if within threshold
- Helps players farm specific enemy types

### Development Commands

**Backend**:

```bash
./mvnw spring-boot:run    # Start app + PostgreSQL (Docker)
./mvnw test               # Run all tests
./mvnw test -Dtest=ClassName  # Run specific test
./mvnw clean package      # Build JAR
./mvnw flyway:info        # Check migration status
```

**Frontend**:

```bash
cd frontend
npm run dev               # Dev server (http://localhost:5173)
npm test                  # Run tests
npm run build            # Production build
npm run type-check       # TypeScript compilation check
```

### File Structure

**Backend**:

- `/src/main/java/com/pauloneill/arcraidersplanner/`
    - `controller/` - REST endpoints
    - `service/` - Business logic
    - `repository/` - Data access (JPA)
    - `dto/` - Data transfer objects
    - `entity/` - JPA entities
    - `exception/` - Custom exceptions

**Frontend**:

- `/frontend/src/`
    - `components/` - Reusable UI components
    - `features/` - Feature-specific components
    - `types/` - TypeScript interfaces
    - `api/` - API integration layer

**Database**:

- `/src/main/resources/db/migration/` - Flyway SQL scripts
- `compose.yaml` - PostgreSQL container configuration

### Documentation Standards

**JavaDoc** (focus on WHY):

```java
/**
 * Recommends optimal maps based on item loot type.
 * WHY: Players need efficient farming routes to minimize time spent
 *
 * @param itemName Case-insensitive item name
 * @return Sorted list of maps by loot zone count
 * @throws ItemNotFoundException if item doesn't exist
 */
```

**OpenAPI/Swagger** (always add):

```java
@Operation(summary = "Search items by name")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Items found"),
    @ApiResponse(responseCode = "400", description = "Invalid search term")
})
```

**TSDoc** (for complex components):

```tsx
/**
 * Displays loot planning interface with map recommendations.
 * WHY: Core UX for helping players optimize their raids
 */
export const Planner: React.FC<PlannerProps> = () => {
```

### Comment Philosophy

- Only comment non-obvious logic
- Document WHY (intent), not WHAT (syntax)
- Business logic explanations in services
- Algorithm rationale for complex operations
- API contract details

### Current Priorities

1. ✅ ~~Advanced routing profiles~~ (DONE)
2. ✅ ~~ARC enemy targeting~~ (DONE)
3. 🔄 Add comprehensive unit & E2E test coverage
4. 🔄 Implement API response caching for Metaforge
5. 🔄 Add OpenAPI/Swagger documentation
6. 🔄 Optimize database queries (N+1 prevention)
7. 🔄 UI/UX improvements for route visualization

---

## Usage Guidelines

### Starting a New Task

1. **User provides task**: "Add quest tracking feature"

2. **Enter EXPLAIN Mode** (if needed):
    - Investigate current codebase
    - Understand related systems
    - Identify integration points

3. **Enter PLAN Mode**:
    - Present comprehensive plan
    - Wait for user approval
    - Do NOT start coding

4. **Enter IMPLEMENT Mode** (only after approval):
    - Follow plan systematically
    - Adhere to code standards
    - Write tests alongside code

5. **Enter VERIFY Mode**:
    - Run all tests
    - Verify builds succeed
    - Perform manual smoke tests
    - Report findings

### Remember

- **Context is everything**: Read existing code before suggesting changes
- **Patterns matter**: Follow established conventions in this codebase
- **Test thoroughly**: Don't skip verification steps
- **Document well**: Future developers (including you) will thank you
- **Seek approval**: Better to ask than to assume

---

## Quick Reference

**Tools**: glob, grep, read_file, write_file, edit, run_shell_command, web_search

**Mode Transitions**: Default → Explain → Plan → [User Approval] → Implement → Verify

**Code Style**: Spring Boot best practices, React hooks, TypeScript strict mode

**Testing**: JUnit/Mockito (backend), Vitest/Testing Library (frontend)

**Remember**: This is a game loot optimization tool. Keep the player's gaming experience in mind when making technical decisions. Efficient routes = better gaming experience!
