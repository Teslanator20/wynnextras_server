# Adding Verified Users - Quick Guide

## What Are Verified Users?

Verified users are trusted contributors who can **instantly approve** loot pools with just 1 submission (instead of requiring 3 matching submissions from regular users).

---

## Method 1: Edit WynnextrasServerApplication.java (RECOMMENDED)

Add this to your main application class:

```java
package com.julianh06.wynnextras_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@RestController
public class WynnextrasServerApplication {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerifiedUserRepository verifiedUserRepo;  // ADD THIS

    public static void main(String[] args) {
        SpringApplication.run(WynnextrasServerApplication.class, args);
    }

    // ADD THIS METHOD
    @PostConstruct
    public void initVerifiedUsers() {
        // Add your trusted player usernames here
        // These are Minecraft UUIDs (without dashes)
        List<String> verifiedUsernames = Arrays.asList(
            "069a79f444e94726a5befca90e38aaf5",  // Example UUID 1
            "f6489b79-9f28-4a9f-9d9c-6a3e8c3f4b2a", // Example UUID 2
            "your_uuid_here"                      // Add more here
        );

        System.out.println("=== Adding Verified Users ===");

        for (String username : verifiedUsernames) {
            if (!verifiedUserRepo.existsByUsername(username)) {
                verifiedUserRepo.save(new VerifiedUser(username));
                System.out.println("✓ Added verified user: " + username);
            } else {
                System.out.println("  Already verified: " + username);
            }
        }

        System.out.println("=== Verified Users Setup Complete ===");
    }

    @GetMapping("/")
    public String index() {
        return "Welcome to WynnExtras Backend!";
    }

    // Your other methods...
}
```

### How to Get Your UUID

**Option 1: In-game with WynnExtras**
```
1. Join Wynncraft
2. Run: /we aspects scan
3. Check server logs for "Saved aspect submission from: <UUID>"
```

**Option 2: Online**
```
Visit: https://mcuuid.net/
Enter Minecraft username
Copy the UUID (with or without dashes, both work)
```

**Option 3: Via Wynncraft API**
```bash
curl -H "Authorization: Bearer YOUR_API_KEY" \
  https://api.wynncraft.com/v3/player/whoami
```

---

## Method 2: Direct SQL Insert

Connect to your PostgreSQL database and run:

```sql
INSERT INTO verified_user (username, added_at)
VALUES
  ('069a79f444e94726a5befca90e38aaf5', NOW()),
  ('f6489b79-9f28-4a9f-9d9c-6a3e8c3f4b2a', NOW()),
  ('your_uuid_here', NOW());
```

---

## Method 3: REST Endpoint (For Dynamic Management)

Add this to your `UserController.java`:

```java
@Autowired
private VerifiedUserRepository verifiedUserRepo;

@PostMapping("/admin/verified-user/{username}")
public ResponseEntity<?> addVerifiedUser(@PathVariable String username) {
    if (verifiedUserRepo.existsByUsername(username)) {
        return ResponseEntity.badRequest().body("User already verified");
    }

    verifiedUserRepo.save(new VerifiedUser(username));
    return ResponseEntity.ok("User verified: " + username);
}

@DeleteMapping("/admin/verified-user/{username}")
public ResponseEntity<?> removeVerifiedUser(@PathVariable String username) {
    Optional<VerifiedUser> user = verifiedUserRepo.findByUsername(username);
    if (user.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    verifiedUserRepo.delete(user.get());
    return ResponseEntity.ok("User removed: " + username);
}

@GetMapping("/admin/verified-users")
public ResponseEntity<?> listVerifiedUsers() {
    List<VerifiedUser> users = verifiedUserRepo.findAll();
    return ResponseEntity.ok(users);
}
```

Then use curl:
```bash
# Add user
curl -X POST http://localhost:8080/admin/verified-user/069a79f444e94726a5befca90e38aaf5

# Remove user
curl -X DELETE http://localhost:8080/admin/verified-user/069a79f444e94726a5befca90e38aaf5

# List all
curl http://localhost:8080/admin/verified-users
```

---

## Verification

After adding verified users, check the logs when starting your server:

```
=== Adding Verified Users ===
✓ Added verified user: 069a79f444e94726a5befca90e38aaf5
  Already verified: f6489b79-9f28-4a9f-9d9c-6a3e8c3f4b2a
=== Verified Users Setup Complete ===
```

---

## Testing Verified User Status

1. Have a verified user submit a loot pool
2. Check the response - should be "approved" immediately
3. Check server logs:
   ```
   Verified user <uuid> submitted loot pool for NOTG week 2026-W04, auto-approving
   ```

---

## Managing Verified Users

### Add More Users
Edit the list in `initVerifiedUsers()` and restart the server.

### Remove a User
Use Method 3 (REST endpoint) or direct SQL:
```sql
DELETE FROM verified_user WHERE username = 'uuid_to_remove';
```

### List All Verified Users
Direct SQL:
```sql
SELECT * FROM verified_user ORDER BY added_at DESC;
```

Or use the REST endpoint (if you added Method 3).

---

## Security Note

⚠️ **The `/admin/*` endpoints in Method 3 are NOT secured.**

For production, add authentication:
```java
// Add Spring Security dependency to pom.xml first
@PostMapping("/admin/verified-user/{username}")
@PreAuthorize("hasRole('ADMIN')")  // Requires authentication
public ResponseEntity<?> addVerifiedUser(@PathVariable String username) {
    // ...
}
```

Or simply don't expose these endpoints and manage verified users through:
- Server startup (`@PostConstruct`)
- Direct database access
- Server console commands

---

## Quick Reference

**Who should be verified?**
- Mod developers
- Trusted community members
- Players with proven accurate submissions
- Guild leaders you trust

**How many verified users should I have?**
- Start with 3-5 trusted players
- Add more as needed
- You can always remove users later

**What happens if a verified user submits wrong data?**
- Their submission instantly approves (no consensus check)
- Only add users you REALLY trust
- You can manually delete incorrect approved loot pools from the database if needed

---

## Example: Real World Setup

```java
@PostConstruct
public void initVerifiedUsers() {
    List<String> verifiedUsernames = Arrays.asList(
        "julianh06-uuid",           // Mod developer
        "trusted-tester-1-uuid",    // Beta tester
        "guild-leader-uuid"         // Active community member
    );

    // ... rest of method
}
```

This gives you 3 trusted sources who can keep loot pools up-to-date even if regular player submissions are slow.
