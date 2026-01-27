# Verified Users System

## Overview

Verified users have special privileges:
- **Loot Pool Auto-Approval**: When a verified user submits a loot pool, it's instantly approved without requiring 3 submissions from different users
- **Trusted Contributors**: Verified users are trusted community members who consistently provide accurate data

## Managing Verified Users

### File Location

Edit the file: `src/main/resources/verified_users.txt`

### File Format

```text
# Lines starting with # are comments
# Add one Minecraft username per line
# Usernames are case-insensitive

PlayerName1
PlayerName2
AnotherPlayer
```

**Rules**:
- One username per line
- Case-insensitive (stored as lowercase)
- Lines starting with `#` are ignored (comments)
- Empty lines are ignored
- Whitespace is trimmed

### Adding a Verified User

1. Open `src/main/resources/verified_users.txt`
2. Add the Minecraft username on a new line
3. Save the file
4. Either:
   - **Option A**: Restart the server (loads automatically on startup)
   - **Option B**: Trigger reload via API: `POST http://wynnextras.com/admin/reload-verified-users`

### Removing a Verified User

1. Open `src/main/resources/verified_users.txt`
2. Delete or comment out the username (add `#` at the start of the line)
3. Save the file
4. Reload (restart server or call reload endpoint)

### Reload Without Restart

```bash
# Reload verified users from file
curl -X POST http://wynnextras.com/admin/reload-verified-users

# Check verified user count
curl http://wynnextras.com/admin/verified-users/count
```

**Response**:
```json
{
  "status": "success",
  "message": "Verified users reloaded successfully",
  "verifiedUserCount": 5
}
```

## How It Works

### On Server Startup

1. `VerifiedUserLoader` runs automatically
2. Reads `verified_users.txt` from classpath
3. Syncs usernames to database:
   - Adds new users from file
   - Removes users no longer in file
   - Keeps existing users
4. Logs summary: `Verified users loaded: X total (Y added, Z existing, W removed)`

### Database Sync

- File is the **source of truth**
- Database is synced to match file
- Users removed from file are removed from database
- Users added to file are added to database

### Loot Pool Submission

When a verified user submits a loot pool:
1. Server checks `isVerifiedUser(username)`
2. If verified → instant approval, no waiting for 3 confirmations
3. Logged as: `Verified user {username} submitted loot pool for {raidType} week {weekId}, auto-approving`

## Security Note

Currently, the reload endpoint has **no authentication**. This means anyone can trigger a reload.

If you want to secure it:
1. Add authentication to `AdminController`
2. Require admin API key or credentials
3. Use Spring Security or custom auth

Example with API key:
```java
@PostMapping("/reload-verified-users")
public ResponseEntity<?> reloadVerifiedUsers(
        @RequestHeader(value = "Admin-Key") String adminKey) {
    if (!adminKey.equals(System.getenv("ADMIN_KEY"))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid admin key");
    }
    // ... reload logic
}
```

## Example

### verified_users.txt
```text
# WynnExtras Core Team
Teslanator20
JulianH06

# Trusted Contributors
ExamplePlayer123
TrustedUser456
```

### Result
- 4 verified users loaded
- All 4 can auto-approve loot pools
- Changes take effect on next reload or restart

## Logs

Check logs for verified user activity:

```
INFO: Loading verified users from verified_users.txt
INFO: Added new verified user: teslanator20
INFO: Added new verified user: julianh06
INFO: Verified users loaded: 2 total (2 added, 0 existing, 0 removed)
```

When verified user submits:
```
INFO: Verified user Teslanator20 submitted loot pool for NOTG week 2026-W04, auto-approving
```

## Troubleshooting

### No users loaded

**Check**:
1. File exists: `src/main/resources/verified_users.txt`
2. File is not empty (has at least one non-comment line)
3. Server logs for errors: `Error loading verified users from file`

### User not being verified

**Check**:
1. Username is in file and not commented out
2. Username matches exactly (case-insensitive)
3. File was reloaded after adding username
4. Check database: Should have entry in `verified_user` table

### Reload endpoint not working

**Check**:
1. Server is running
2. Endpoint URL is correct: `POST /admin/reload-verified-users`
3. Check response for error details
4. Check server logs
