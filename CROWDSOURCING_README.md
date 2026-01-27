# WynnExtras Server - Crowdsourcing Implementation

## Overview
This implementation adds crowdsourced loot pools and gambits with automatic approval based on consensus.

## Features

### Loot Pools (Raid Aspect Pools)
- **Reset Schedule:** Friday 19:00 CET
- **Approval Requirements:**
  - 1 submission from verified user = instant approval
  - 3 matching submissions = approval
  - 10 matching submissions = approval + LOCKED for the week
- **Locked pools** cannot be changed until next reset

### Gambits (Daily Challenges)
- **Reset Schedule:** Daily 19:00 CET
- **Approval Requirements:**
  - 2 matching submissions = approval + LOCKED for the day
- **Locked gambits** cannot be changed until next reset

## API Endpoints

### Loot Pools
- **POST** `/lootpool/{raidType}` - Submit loot pool (NOTG, NOL, TCC, TNA)
  - Header: `Wynncraft-Api-Key`
  - Body: `{"aspects": [{"name": "...", "rarity": "...", "requiredClass": "..."}]}`
  - Returns: `{status: "approved"|"submitted", message: "...", lootPool?: {...}}`

- **GET** `/lootpool/{raidType}` - Get approved loot pool
  - Returns: `{"aspects": [...]}`
  - 404 if no approved pool exists

### Gambits
- **POST** `/gambit` - Submit gambits
  - Header: `Wynncraft-Api-Key`
  - Body: `{"gambits": [{"name": "...", "description": "..."}]}`
  - Returns: `{status: "approved"|"submitted", message: "...", gambits?: {...}}`

- **GET** `/gambit` - Get approved gambits
  - Returns: `{"gambits": [...]}`
  - 404 if no approved gambits exist

## Adding Verified Users

Verified users can instantly approve loot pools with a single submission.

### Option 1: Direct Database Insert (PostgreSQL)
```sql
INSERT INTO verified_user (username, added_at)
VALUES ('player_uuid_here', NOW());
```

### Option 2: Using Spring Boot Application
Add this method to `WynnextrasServerApplication.java`:

```java
@PostConstruct
public void addVerifiedUsers() {
    List<String> verifiedUsernames = Arrays.asList(
        "uuid1",
        "uuid2",
        "uuid3"
    );

    for (String username : verifiedUsernames) {
        if (!verifiedUserRepo.existsByUsername(username)) {
            verifiedUserRepo.save(new VerifiedUser(username));
            System.out.println("Added verified user: " + username);
        }
    }
}
```

### Option 3: REST Endpoint (Add to UserController.java)
```java
@PostMapping("/admin/verified-user/{username}")
public ResponseEntity<?> addVerifiedUser(@PathVariable String username) {
    if (verifiedUserRepo.existsByUsername(username)) {
        return ResponseEntity.badRequest().body("User already verified");
    }
    verifiedUserRepo.save(new VerifiedUser(username));
    return ResponseEntity.ok("User verified: " + username);
}
```

## Database Migration

When deploying, the following tables will be created automatically by JPA:

1. `raid_lootpool_submission` - All loot pool submissions
2. `raid_lootpool_approved` - Currently approved loot pools
3. `gambit_submission` - All gambit submissions
4. `gambit_approved` - Currently approved gambits
5. `verified_user` - List of trusted submitters

## How Matching Works

### Loot Pools
Aspects are sorted alphabetically by name before comparison. Two submissions match if their JSON strings are identical after sorting.

Example:
```json
[
  {"name": "Aspect of A", "rarity": "Mythic", "requiredClass": "Warrior"},
  {"name": "Aspect of B", "rarity": "Legendary", "requiredClass": null}
]
```

### Gambits
Gambits are sorted alphabetically by name before comparison.

Example:
```json
[
  {"name": "Gambit A", "description": "..."},
  {"name": "Gambit B", "description": "..."}
]
```

## Time Zone Handling

All reset times use CET (Central European Time):
- **Loot Pools:** Week defined as Friday 19:00 CET to next Friday 19:00 CET
- **Gambits:** Day defined as 19:00 CET to next day 19:00 CET

Week identifier format: `"2026-W04"` (ISO week number)
Day identifier format: `"2026-01-27"` (ISO date)

## Testing

### Test Loot Pool Submission
```bash
curl -X POST http://localhost:8080/lootpool/NOTG \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: YOUR_KEY" \
  -d '{
    "aspects": [
      {"name": "Aspect of Fire", "rarity": "Mythic", "requiredClass": "Warrior"}
    ]
  }'
```

### Test Gambit Submission
```bash
curl -X POST http://localhost:8080/gambit \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: YOUR_KEY" \
  -d '{
    "gambits": [
      {"name": "Glutton'\''s Gambit", "description": "Eat 100 foods"}
    ]
  }'
```

## Deployment Checklist

1. ✅ Add all new Java files to the project
2. ✅ Update `pom.xml` (should work with existing dependencies)
3. ✅ Configure database (PostgreSQL recommended)
4. ✅ Add verified users to database
5. ✅ Deploy and test endpoints
6. ✅ Update client mod to use new endpoints
