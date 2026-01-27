# WynnExtras Crowdsourcing - Full Deployment Guide

## 📦 What Was Added

### Backend (17 new files)
All files created in: `C:\Users\tim\wynnextras_server_new\src\main\java\com\julianh06\wynnextras_server\`

**Entities (5 files):**
- `RaidLootPoolSubmission.java` - Stores all loot pool submissions
- `RaidLootPoolApproved.java` - Current approved loot pools
- `GambitSubmission.java` - Stores all gambit submissions
- `GambitApproved.java` - Current approved gambits
- `VerifiedUser.java` - Trusted users list

**Repositories (5 files):**
- `RaidLootPoolSubmissionRepository.java`
- `RaidLootPoolApprovedRepository.java`
- `GambitSubmissionRepository.java`
- `GambitApprovedRepository.java`
- `VerifiedUserRepository.java`

**Services (2 files):**
- `LootPoolService.java` - Handles loot pool logic (approval, matching, locking)
- `GambitService.java` - Handles gambit logic (approval, matching)

**Controllers (2 files):**
- `LootPoolController.java` - REST endpoints for loot pools
- `GambitController.java` - REST endpoints for gambits

**DTOs (2 files):**
- `LootPoolSubmissionDto.java` - Request/response format for loot pools
- `GambitSubmissionDto.java` - Request/response format for gambits

**Utils (1 file):**
- `TimeUtils.java` - CET timezone handling for reset times

### Client Changes (2 files modified)
Location: `C:\Users\tim\Wynnextras_11\src\main\java\julianh06\wynnextras\`

**Modified Files:**
1. `features/aspects/aspect.java`
   - Changed preview chest upload to use crowdsourcing endpoint
   - Changed gambit detection to upload to crowdsourcing endpoint

2. `features/profileviewer/WynncraftApiHandler.java`
   - Added `uploadLootPool()` - Submit loot pool without personal progress
   - Added `uploadGambits()` - Submit gambits
   - Added `fetchCrowdsourcedLootPool()` - Get approved loot pool
   - Added `fetchCrowdsourcedGambits()` - Get approved gambits
   - Added `extractRequiredClass()` - Helper method

---

## 🚀 Backend Deployment Steps

### 1. Copy Files to Your Server Repository
```bash
# Copy all new files from:
C:\Users\tim\wynnextras_server_new\src\main\java\com\julianh06\wynnextras_server\

# To your actual server repository at:
<your_server_path>/src/main/java/com/julianh06/wynnextras_server/
```

### 2. Verify pom.xml Dependencies
Your existing `pom.xml` should already have everything needed:
- Spring Boot 3.5.7
- Spring Data JPA
- PostgreSQL driver
- H2 (for testing)

No changes needed unless you're missing Jackson (should be included with Spring Boot).

### 3. Database Setup

**Option A: PostgreSQL (Production)**
The tables will be created automatically by JPA when you start the application.

**Option B: Manual SQL (if needed)**
```sql
CREATE TABLE raid_lootpool_submission (
    id BIGSERIAL PRIMARY KEY,
    raid_type VARCHAR(10) NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    aspects_json TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    week_identifier VARCHAR(10) NOT NULL
);

CREATE TABLE raid_lootpool_approved (
    id BIGSERIAL PRIMARY KEY,
    raid_type VARCHAR(10) NOT NULL,
    aspects_json TEXT NOT NULL,
    approved_at TIMESTAMP NOT NULL,
    week_identifier VARCHAR(10) NOT NULL,
    locked BOOLEAN NOT NULL,
    submission_count INT NOT NULL
);

CREATE TABLE gambit_submission (
    id BIGSERIAL PRIMARY KEY,
    gambits_json TEXT NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    day_identifier VARCHAR(10) NOT NULL
);

CREATE TABLE gambit_approved (
    id BIGSERIAL PRIMARY KEY,
    gambits_json TEXT NOT NULL,
    approved_at TIMESTAMP NOT NULL,
    day_identifier VARCHAR(10) NOT NULL,
    locked BOOLEAN NOT NULL
);

CREATE TABLE verified_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    added_at TIMESTAMP NOT NULL
);
```

### 4. Add Verified Users

**Method 1: Direct SQL**
```sql
INSERT INTO verified_user (username, added_at)
VALUES ('your_minecraft_uuid_here', NOW());
```

**Method 2: Programmatically (Recommended)**
Edit `WynnextrasServerApplication.java` and add:

```java
@Autowired
private VerifiedUserRepository verifiedUserRepo;

@PostConstruct
public void addVerifiedUsers() {
    // Add your trusted usernames here (Minecraft UUIDs)
    List<String> verifiedUsernames = Arrays.asList(
        "uuid-of-trusted-player-1",
        "uuid-of-trusted-player-2",
        "uuid-of-trusted-player-3"
    );

    for (String username : verifiedUsernames) {
        if (!verifiedUserRepo.existsByUsername(username)) {
            verifiedUserRepo.save(new VerifiedUser(username));
            System.out.println("Added verified user: " + username);
        }
    }
}
```

### 5. Build and Deploy
```bash
cd <your_server_path>
./mvnw clean package
java -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar
```

### 6. Test Endpoints

**Test Loot Pool Submission:**
```bash
curl -X POST http://localhost:8080/lootpool/NOTG \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: YOUR_API_KEY" \
  -d '{
    "aspects": [
      {"name": "Aspect of Fire", "rarity": "Mythic", "requiredClass": "Warrior"}
    ]
  }'
```

**Test Gambit Submission:**
```bash
curl -X POST http://localhost:8080/gambit \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: YOUR_API_KEY" \
  -d '{
    "gambits": [
      {"name": "Glutton'\''s Gambit", "description": "Eat 100 foods"}
    ]
  }'
```

**Test Fetching:**
```bash
# Get approved loot pool
curl http://localhost:8080/lootpool/NOTG

# Get approved gambits
curl http://localhost:8080/gambit
```

---

## 🎮 Client Deployment Steps

### 1. Build the Mod
```bash
cd C:/Users/tim/Wynnextras_11
./gradlew.bat build
```

### 2. Deploy to Minecraft
```bash
rm -f "/c/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/mods/"wynnextras-*.jar
cp $(ls build/libs/wynnextras-*.jar | grep -v -E '(-all|-sources)\.jar$') "/c/Users/tim/AppData/Roaming/ModrinthApp/profiles/newest version/mods/"
```

### 3. Test In-Game

**Test Loot Pool Crowdsourcing:**
1. Join Wynncraft server
2. Open a raid preview chest (e.g., NOTG)
3. Check chat for upload status:
   - "Loot pool submitted. Waiting for more confirmations." (1-2 submissions)
   - "Loot pool for NOTG approved!" (3+ submissions or verified user)

**Test Gambit Crowdsourcing:**
1. Open Party Finder (`/pf`)
2. Check chat for:
   - Gambit detection message
   - "Gambits submitted. Waiting for confirmation." (1 submission)
   - "Gambits approved for today!" (2+ submissions)

---

## 🔧 How It Works

### Loot Pool Approval Logic
```
1 verified user submission → ✅ APPROVED
3 matching submissions → ✅ APPROVED
10 matching submissions → ✅ APPROVED + 🔒 LOCKED (cannot change)
```

### Gambit Approval Logic
```
2 matching submissions → ✅ APPROVED + 🔒 LOCKED (cannot change)
```

### Reset Schedule
- **Loot Pools:** Friday 19:00 CET → next Friday 19:00 CET
- **Gambits:** Daily 19:00 CET → next day 19:00 CET

### Matching Algorithm
Submissions are sorted alphabetically before comparison:
- **Loot Pools:** Sorted by aspect name
- **Gambits:** Sorted by gambit name

Two submissions match if their JSON strings are identical after sorting.

---

## 📝 API Reference

### POST /lootpool/{raidType}
Submit a loot pool for crowdsourcing.

**Headers:**
- `Wynncraft-Api-Key: <your-key>`
- `Content-Type: application/json`

**Body:**
```json
{
  "aspects": [
    {
      "name": "Aspect Name",
      "rarity": "Mythic|Fabled|Legendary",
      "requiredClass": "Warrior|Shaman|Mage|Archer|Assassin"
    }
  ]
}
```

**Response (approved):**
```json
{
  "status": "approved",
  "message": "Loot pool approved for NOTG",
  "lootPool": {
    "aspects": [...]
  }
}
```

**Response (pending):**
```json
{
  "status": "submitted",
  "message": "Loot pool submitted. Waiting for more confirmations."
}
```

### GET /lootpool/{raidType}
Get the approved loot pool for a raid.

**Response:**
```json
{
  "aspects": [
    {
      "name": "Aspect Name",
      "rarity": "Mythic",
      "requiredClass": "Warrior"
    }
  ]
}
```

### POST /gambit
Submit gambits for crowdsourcing.

**Headers:**
- `Wynncraft-Api-Key: <your-key>`
- `Content-Type: application/json`

**Body:**
```json
{
  "gambits": [
    {
      "name": "Gambit Name",
      "description": "What it does"
    }
  ]
}
```

**Response (approved):**
```json
{
  "status": "approved",
  "message": "Gambits approved for today",
  "gambits": {
    "gambits": [...]
  }
}
```

### GET /gambit
Get today's approved gambits.

**Response:**
```json
{
  "gambits": [
    {
      "name": "Gambit Name",
      "description": "What it does"
    }
  ]
}
```

---

## 🐛 Troubleshooting

### Backend Issues

**Error: "Table does not exist"**
- JPA should auto-create tables. Check `application.properties`:
  ```properties
  spring.jpa.hibernate.ddl-auto=update
  ```

**Error: "No bean named 'verifiedUserRepo'"**
- Make sure all repository interfaces have `@Repository` annotation
- Check that component scanning includes the package

**Error: "Invalid Wynncraft API key"**
- Verify `WynncraftService.fetchUuid()` is working
- Test API key manually: `curl -H "Authorization: Bearer YOUR_KEY" https://api.wynncraft.com/v3/player/whoami`

### Client Issues

**Error: "You need to set your api-key"**
- Run `/we apikey YOUR_KEY` in-game

**Upload not triggering:**
- Check throttling (60s cooldown per raid)
- Check if it's reset time (Friday 18:30-19:30 CET bypasses cooldown)
- Check logs for errors: `logs/latest.log`

**Compilation errors:**
- Make sure all imports are correct
- Run `./gradlew.bat clean build` to rebuild from scratch

---

## 📊 Monitoring

### Check Submission Status
Add this endpoint to `WynnextrasServerApplication.java`:

```java
@GetMapping("/crowdsource/stats")
public String getCrowdsourceStats() {
    StringBuilder sb = new StringBuilder();

    String weekId = TimeUtils.getWeekIdentifier();
    String dayId = TimeUtils.getDayIdentifier();

    sb.append("=== Crowdsource Statistics ===\n\n");
    sb.append("Current Week: ").append(weekId).append("\n");
    sb.append("Current Day: ").append(dayId).append("\n\n");

    // Loot pools
    sb.append("Loot Pool Submissions:\n");
    for (String raid : Arrays.asList("NOTG", "NOL", "TCC", "TNA")) {
        List<RaidLootPoolSubmission> subs = lootPoolSubmissionRepo.findByRaidTypeAndWeekIdentifier(raid, weekId);
        Optional<RaidLootPoolApproved> approved = lootPoolApprovedRepo.findByRaidTypeAndWeekIdentifier(raid, weekId);

        sb.append("  ").append(raid).append(": ");
        sb.append(subs.size()).append(" submissions");
        if (approved.isPresent()) {
            sb.append(" | APPROVED");
            if (approved.get().isLocked()) {
                sb.append(" 🔒 LOCKED");
            }
        }
        sb.append("\n");
    }

    // Gambits
    sb.append("\nGambit Submissions:\n");
    List<GambitSubmission> gambitSubs = gambitSubmissionRepo.findByDayIdentifier(dayId);
    Optional<GambitApproved> gambitApproved = gambitApprovedRepo.findByDayIdentifier(dayId);

    sb.append("  Today: ").append(gambitSubs.size()).append(" submissions");
    if (gambitApproved.isPresent()) {
        sb.append(" | APPROVED");
        if (gambitApproved.get().isLocked()) {
            sb.append(" 🔒 LOCKED");
        }
    }
    sb.append("\n");

    return sb.toString();
}
```

Access at: `http://localhost:8080/crowdsource/stats`

---

## ✅ Deployment Checklist

### Backend
- [ ] Copy all 17 Java files to server project
- [ ] Verify pom.xml has required dependencies
- [ ] Configure database connection
- [ ] Add verified users
- [ ] Build with Maven
- [ ] Deploy to server
- [ ] Test all 4 endpoints (POST/GET lootpool, POST/GET gambit)
- [ ] Verify timezone handling (should use CET)

### Client
- [ ] Verify aspect.java changes (loot pool upload)
- [ ] Verify WynncraftApiHandler.java additions
- [ ] Build mod with Gradle
- [ ] Deploy to Minecraft mods folder
- [ ] Test preview chest scanning + upload
- [ ] Test Party Finder gambit detection + upload
- [ ] Verify API key is set (`/we apikey`)

### Testing
- [ ] Submit 3 matching loot pools → should approve
- [ ] Submit 10 matching loot pools → should lock
- [ ] Submit as verified user → should instant approve
- [ ] Submit 2 matching gambits → should approve + lock
- [ ] Verify reset times work correctly (Friday 19:00 CET for loot pools)

---

## 🎉 Complete!

Your crowdsourcing system is now fully implemented. Players can now contribute to building accurate loot pool and gambit databases that benefit the entire community!

**Key Features:**
✅ Crowdsourced loot pools with consensus (3 players or 1 verified)
✅ Crowdsourced gambits with consensus (2 players)
✅ Auto-locking at high confidence (10 loot pool submissions)
✅ CET timezone handling for Wynncraft reset times
✅ Verified user system for trusted contributors
✅ Separate storage from personal aspect tracking
