# WynnExtras Backend - System Architecture

## 🏗️ Overview

This document describes the technical architecture of the WynnExtras backend server.

## 📊 System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        MINECRAFT CLIENT                          │
│                       (WynnExtras Mod)                          │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MojangAuth.java                                         │  │
│  │  - Generate serverId                                     │  │
│  │  - Call Mojang joinServer                               │  │
│  │  - Prepare headers (Username, Server-ID)                │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────────┘
                        │ HTTP POST
                        │ Headers: Username, Server-ID
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT BACKEND                           │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Controllers Layer                                       │  │
│  │  - PersonalAspectController  (POST/GET /user)          │  │
│  │  - LootPoolController        (POST/GET /lootpool)      │  │
│  │  - GambitController          (POST/GET /gambit)        │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                                │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MojangAuthService                                       │  │
│  │  - verifyPlayer(username, serverId)                     │  │
│  │  - Call Mojang hasJoined API                           │  │
│  │  - Return verified UUID + username                      │  │
│  │  - Replay attack prevention                             │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                                │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Service Layer                                           │  │
│  │  - LootPoolService (consensus logic)                    │  │
│  │  - GambitService   (consensus logic)                    │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                                │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Repository Layer (Spring Data JPA)                     │  │
│  │  - PersonalAspectRepository                             │  │
│  │  - RaidLootPoolSubmissionRepository                     │  │
│  │  - RaidLootPoolApprovedRepository                       │  │
│  │  - GambitSubmissionRepository                           │  │
│  │  - GambitApprovedRepository                             │  │
│  │  - VerifiedUserRepository                               │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                                │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Entity Layer (JPA Entities)                            │  │
│  │  - PersonalAspect                                       │  │
│  │  - RaidLootPoolSubmission                               │  │
│  │  - RaidLootPoolApproved                                 │  │
│  │  - GambitSubmission                                     │  │
│  │  - GambitApproved                                       │  │
│  │  - VerifiedUser                                         │  │
│  └──────────────┬───────────────────────────────────────────┘  │
└──────────────────┼──────────────────────────────────────────────┘
                   │
                   ▼
         ┌──────────────────┐
         │   PostgreSQL     │
         │    Database      │
         └──────────────────┘

         ┌──────────────────┐
         │ Mojang Session   │
         │    Server API    │
         └──────────────────┘
```

## 🔐 Authentication Architecture

### Mojang Sessionserver Flow

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│  CLIENT  │                 │  BACKEND │                 │  MOJANG  │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │ 1. Generate serverId       │                            │
     │    (random secret)         │                            │
     │─────────────────────────►  │                            │
     │                            │                            │
     │ 2. joinServer(uuid,        │                            │
     │    accessToken, serverId)  │                            │
     │──────────────────────────────────────────────────────► │
     │                            │                            │
     │                            │    3. Session validated    │
     │ ◄──────────────────────────────────────────────────────│
     │                            │                            │
     │ 4. POST /user              │                            │
     │    Headers:                │                            │
     │    - Username: "Player"    │                            │
     │    - Server-ID: "abc123"   │                            │
     │─────────────────────────► │                            │
     │                            │                            │
     │                            │ 5. hasJoined(username,     │
     │                            │    serverId)               │
     │                            │─────────────────────────► │
     │                            │                            │
     │                            │ 6. Return verified UUID    │
     │                            │    {"id":"...", "name":".."}│
     │                            │ ◄─────────────────────────│
     │                            │                            │
     │                            │ 7. Process with           │
     │                            │    verified UUID           │
     │                            │                            │
     │ 8. Response (success)      │                            │
     │ ◄──────────────────────────│                            │
     │                            │                            │
```

### Security Properties

**What gets sent where:**
- ❌ Session ID: NEVER sent anywhere (stays in client)
- ❌ Access Token: Sent to Mojang ONLY (not to backend)
- ✅ Username: Sent to backend (untrusted, verified by Mojang)
- ✅ Server ID: Sent to both Mojang and backend (single-use token)
- ✅ Verified UUID: Returned by Mojang (trusted source of truth)

**Protection against:**
- ✅ UUID spoofing - Backend uses Mojang's verified UUID
- ✅ Replay attacks - Server IDs expire after 30 seconds
- ✅ Session hijacking - Access token never exposed to backend
- ✅ Impersonation - Only real player can call joinServer

## 📦 Component Details

### 1. Controllers (`@RestController`)

Handle HTTP requests and responses.

**PersonalAspectController:**
- `POST /user` - Upload personal aspects (authenticated)
- `GET /user?playerUuid=xxx` - Get player's aspects (public)

**LootPoolController:**
- `POST /lootpool/{raidType}` - Submit loot pool (authenticated)
- `GET /lootpool/{raidType}` - Get approved loot pool (public)

**GambitController:**
- `POST /gambit` - Submit gambits (authenticated)
- `GET /gambit` - Get approved gambits (public)

### 2. Services (`@Service`)

Business logic layer.

**MojangAuthService:**
```java
public AuthResult verifyPlayer(String username, String serverId) {
    // 1. Check replay attack (serverId already used?)
    // 2. Call Mojang: GET /session/minecraft/hasJoined?username=X&serverId=Y
    // 3. Parse response (200 = success, 204 = failure)
    // 4. Extract verified UUID from Mojang's response
    // 5. Mark serverId as used (prevent replay)
    // 6. Return AuthResult with verified UUID
}
```

**LootPoolService:**
```java
public LootPoolSubmissionDto submitLootPool(String raidType, List<AspectData> aspects, String username) {
    // 1. Get current week identifier (e.g., "2026-W04")
    // 2. Check if already approved AND locked
    // 3. Convert aspects to sorted JSON (canonical form)
    // 4. Check if user is verified (instant approval if yes)
    // 5. Save submission to database
    // 6. Find matching submissions (same aspectsJson)
    // 7. If 3+ matches OR verified user: approve
    // 8. If 10+ matches: approve + lock
    // 9. Return approval status
}
```

**GambitService:**
```java
public GambitSubmissionDto submitGambits(List<GambitData> gambits, String username) {
    // 1. Get current day identifier (e.g., "2026-01-27")
    // 2. Check if already approved AND locked
    // 3. Convert gambits to sorted JSON
    // 4. Save submission
    // 5. Find matching submissions
    // 6. If 2+ matches: approve + lock
    // 7. Return approval status
}
```

### 3. Repositories (`@Repository`)

Data access layer (Spring Data JPA).

```java
public interface PersonalAspectRepository extends JpaRepository<PersonalAspect, Long> {
    List<PersonalAspect> findByPlayerUuid(String playerUuid);
    Optional<PersonalAspect> findByPlayerUuidAndAspectName(String playerUuid, String aspectName);
}
```

### 4. Entities (`@Entity`)

Database models.

**PersonalAspect:**
```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"player_uuid", "aspect_name"}))
public class PersonalAspect {
    @Id @GeneratedValue
    private Long id;
    private String playerUuid;        // Normalized UUID (no dashes)
    private String playerName;        // Display name
    private String aspectName;        // "Aspect of Fire"
    private String rarity;            // "Mythic", "Fabled", etc.
    private int amount;               // How many player has
    private String modVersion;        // Client version
    private Instant updatedAt;        // Last upload time
}
```

**RaidLootPoolSubmission:**
```java
@Entity
public class RaidLootPoolSubmission {
    @Id @GeneratedValue
    private Long id;
    private String raidType;          // "NOTG", "NOL", "TCC", "TNA"
    private String submittedBy;       // Username
    private String aspectsJson;       // Sorted JSON array
    private Instant submittedAt;
    private String weekIdentifier;    // "2026-W04"
}
```

**RaidLootPoolApproved:**
```java
@Entity
public class RaidLootPoolApproved {
    @Id @GeneratedValue
    private Long id;
    private String raidType;
    private String aspectsJson;       // Approved loot pool
    private Instant approvedAt;
    private String weekIdentifier;
    private boolean locked;           // True if 10+ submissions
    private int submissionCount;      // How many matched
}
```

### 5. DTOs (Data Transfer Objects)

Request/response formats.

**PersonalAspectDto:**
```java
public class UploadRequest {
    private String playerName;
    private String modVersion;
    private List<AspectData> aspects;
}

public class AspectData {
    private String name;
    private String rarity;
    private int amount;
}

public class PlayerAspectsResponse {
    private String playerUuid;
    private String playerName;
    private String modVersion;
    private long updatedAt;
    private List<AspectData> aspects;
}
```

### 6. Utilities

**TimeUtils:**
```java
public class TimeUtils {
    private static final ZoneId CET = ZoneId.of("Europe/Paris");

    public static ZonedDateTime getCurrentCETTime() {
        return ZonedDateTime.now(CET);
    }

    public static String getWeekIdentifier() {
        // Returns "2026-W04" for the week starting last Friday 19:00
    }

    public static String getDayIdentifier() {
        // Returns "2026-01-27" for today (after 19:00)
    }
}
```

## 🔄 Request Flow Examples

### Example 1: Personal Aspect Upload

```
1. User opens reward chest in Minecraft
2. Client detects aspects: [Aspect of Fire (5), Aspect of Water (12)]
3. MojangAuth.getAuthData():
   - Generate serverId = "abc123xyz"
   - Call Mojang joinServer(uuid, accessToken, "abc123xyz")
   - Return AuthData(username="Player1", serverId="abc123xyz")
4. Build JSON payload:
   {
     "playerName": "Player1",
     "modVersion": "0.12.1",
     "aspects": [
       {"name": "Aspect of Fire", "rarity": "Mythic", "amount": 5},
       {"name": "Aspect of Water", "rarity": "Fabled", "amount": 12}
     ]
   }
5. POST http://wynnextras.com/user
   Headers:
     Username: Player1
     Server-ID: abc123xyz
   Body: <JSON from step 4>
6. PersonalAspectController receives request
7. Call mojangAuth.verifyPlayer("Player1", "abc123xyz")
8. MojangAuthService:
   - Check replay attack: serverId not used yet ✅
   - Call Mojang: GET https://sessionserver.mojang.com/session/minecraft/hasJoined?username=Player1&serverId=abc123xyz
   - Mojang returns: {"id":"a1b2c3d4...","name":"Player1"}
   - Extract UUID: "a1b2c3d4..." (normalized)
   - Mark serverId as used
   - Return AuthResult.success("a1b2c3d4...", "Player1")
9. Controller uses VERIFIED UUID (not what client claimed)
10. For each aspect:
    - Find existing by (verifiedUuid, aspectName)
    - If exists: UPDATE amount, rarity, updatedAt
    - If not: INSERT new row
11. Return 200 OK: {"status":"success","message":"Aspects uploaded successfully"}
```

### Example 2: Loot Pool Crowdsourcing

```
1. User opens NOTG preview chest
2. Client scans aspects in preview
3. Authenticate with Mojang (same as Example 1)
4. Build loot pool submission:
   {
     "aspects": [
       {"name":"Aspect of Courage","rarity":"Mythic","requiredClass":"Warrior"},
       {"name":"Aspect of Wisdom","rarity":"Fabled","requiredClass":"Mage"}
     ]
   }
5. POST http://wynnextras.com/lootpool/NOTG
   Headers: Username, Server-ID
   Body: <JSON from step 4>
6. LootPoolController receives request
7. Authenticate with Mojang (verifiedUsername = "Player1")
8. Call lootPoolService.submitLootPool("NOTG", aspects, "Player1")
9. LootPoolService:
   - Get week: "2026-W04"
   - Check if already approved+locked for NOTG/2026-W04: NO
   - Sort aspects by name: [{Courage},{Wisdom}]
   - Convert to JSON: '[{"name":"Aspect of Courage",...},{"name":"Aspect of Wisdom",...}]'
   - Check if "Player1" is verified user: NO
   - Save RaidLootPoolSubmission(NOTG, Player1, <JSON>, 2026-W04)
   - Find submissions with same JSON for NOTG/2026-W04: Count = 3 (including this one)
   - 3+ submissions! APPROVE
   - Save RaidLootPoolApproved(NOTG, <JSON>, 2026-W04, locked=false, count=3)
   - Return LootPoolSubmissionDto with aspects
10. Controller returns 200:
    {
      "status": "approved",
      "message": "Loot pool approved for NOTG",
      "lootPool": {aspects:[...]}
    }
11. Client shows: "§aLoot pool for §eNOTG §aapproved!"
```

## ⏰ Reset Logic

### Weekly Reset (Loot Pools)

```java
public static String getWeekIdentifier() {
    ZonedDateTime now = getCurrentCETTime();

    // Find last Friday 19:00 (or earlier)
    ZonedDateTime lastReset = now
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        .withHour(19).withMinute(0).withSecond(0).withNano(0);

    // If we're before Friday 19:00, go back another week
    if (now.isBefore(lastReset)) {
        lastReset = lastReset.minusWeeks(1);
    }

    // Get ISO week number
    int year = lastReset.getYear();
    int weekNumber = lastReset.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

    return String.format("%d-W%02d", year, weekNumber);
}
```

**Example timeline:**
- Friday Jan 24, 18:59 CET → "2026-W04" (still previous week)
- Friday Jan 24, 19:00 CET → "2026-W05" (NEW WEEK!)
- Friday Jan 24, 19:01 CET → "2026-W05"
- Thursday Jan 30, 23:59 CET → "2026-W05"
- Friday Jan 31, 19:00 CET → "2026-W06" (NEXT WEEK!)

### Daily Reset (Gambits)

```java
public static String getDayIdentifier() {
    ZonedDateTime now = getCurrentCETTime();

    // If before 19:00 today, use yesterday's date
    if (now.getHour() < 19) {
        now = now.minusDays(1);
    }

    return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
}
```

**Example timeline:**
- Monday Jan 27, 18:59 CET → "2026-01-26" (still yesterday)
- Monday Jan 27, 19:00 CET → "2026-01-27" (NEW DAY!)
- Tuesday Jan 28, 18:59 CET → "2026-01-27" (still yesterday)
- Tuesday Jan 28, 19:00 CET → "2026-01-28" (NEW DAY!)

## 🛡️ Security Measures

### 1. Authentication Security
- ✅ Mojang sessionserver verification (cryptographic proof)
- ✅ No session IDs or access tokens sent to backend
- ✅ Backend uses Mojang's verified UUID (not client claim)

### 2. Replay Attack Prevention
- ✅ Server IDs tracked in ConcurrentHashMap
- ✅ 30-second expiry window
- ✅ Automatic cleanup at 1000+ entries

### 3. Input Validation
- ✅ UUID format validation (32 hex chars)
- ✅ Raid type validation (NOTG/NOL/TCC/TNA only)
- ✅ Empty request body rejection
- ✅ SQL injection prevention (JPA prepared statements)

### 4. Database Security
- ✅ Unique constraints (prevent duplicate personal aspects)
- ✅ JPA transactions (@Transactional)
- ✅ Connection pooling (HikariCP)

### 5. Crowdsourcing Integrity
- ✅ Consensus requirement (3+ submissions)
- ✅ Verified user system (trusted contributors)
- ✅ Locking at high confidence (10+ submissions)
- ✅ JSON canonicalization (sorted before comparison)

## 📈 Performance Considerations

### Database Indexes (Recommended)

```sql
-- Speed up personal aspect lookups
CREATE INDEX idx_personal_aspect_uuid ON personal_aspect(player_uuid);
CREATE INDEX idx_personal_aspect_uuid_name ON personal_aspect(player_uuid, aspect_name);

-- Speed up loot pool queries
CREATE INDEX idx_lootpool_submission_week ON raid_lootpool_submission(week_identifier, raid_type);
CREATE INDEX idx_lootpool_submission_json ON raid_lootpool_submission(aspects_json); -- For matching
CREATE INDEX idx_lootpool_approved_week ON raid_lootpool_approved(week_identifier, raid_type);

-- Speed up gambit queries
CREATE INDEX idx_gambit_submission_day ON gambit_submission(day_identifier);
CREATE INDEX idx_gambit_submission_json ON gambit_submission(gambits_json); -- For matching
CREATE INDEX idx_gambit_approved_day ON gambit_approved(day_identifier);
```

### Caching Strategy (Future)

```java
// Cache approved loot pools (1 hour TTL)
@Cacheable(value = "lootpools", key = "#raidType + '-' + T(TimeUtils).getWeekIdentifier()")
public LootPoolSubmissionDto getApprovedLootPool(String raidType);

// Cache approved gambits (1 hour TTL)
@Cacheable(value = "gambits", key = "T(TimeUtils).getDayIdentifier()")
public GambitSubmissionDto getApprovedGambits();
```

### Connection Pooling

```properties
# HikariCP settings (in application.properties)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

## 🔍 Monitoring & Logging

### Key Log Messages

```
INFO  MojangAuthService - Successfully authenticated player PlayerName (UUID: abc123...)
WARN  MojangAuthService - Replay attack detected: serverId xyz already used
WARN  MojangAuthService - Authentication failed for username Player: Invalid session
ERROR MojangAuthService - Error verifying player with Mojang: <exception>

INFO  LootPoolService - Loot pool for NOTG approved (3 matching submissions)
INFO  LootPoolService - Loot pool for NOTG locked (10 matching submissions)
INFO  PersonalAspectController - Saved 15 aspects for verified player PlayerName (UUID: abc...)
```

### Health Monitoring

```bash
# Overall health
GET /actuator/health

# Database connectivity
GET /actuator/health/db

# Disk space
GET /actuator/health/diskSpace
```

## 🚀 Deployment Architecture

### Production Setup

```
                      ┌──────────────┐
                      │   Internet   │
                      └───────┬──────┘
                              │
                              ▼
                      ┌───────────────┐
                      │  Nginx (443)  │
                      │  SSL/TLS      │
                      └───────┬───────┘
                              │
                              ▼
                      ┌───────────────┐
                      │ Spring Boot   │
                      │   (8080)      │
                      └───────┬───────┘
                              │
                              ▼
                      ┌───────────────┐
                      │  PostgreSQL   │
                      │   (5432)      │
                      └───────────────┘
```

### Docker Deployment

```yaml
services:
  nginx:
    image: nginx:alpine
    ports: ["443:443", "80:80"]
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl

  backend:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wynnextras
    depends_on: [postgres]

  postgres:
    image: postgres:15
    volumes: [postgres_data:/var/lib/postgresql/data]
    environment:
      POSTGRES_DB: wynnextras
```

---

**Document Version:** 1.0
**Last Updated:** January 2026
**Architecture:** Microservice, RESTful API, Mojang Authentication
