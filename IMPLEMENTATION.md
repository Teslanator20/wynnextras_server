# WynnExtras Backend - Complete Implementation Guide

## 🎯 Overview

This guide will help you deploy the WynnExtras backend server with secure Mojang authentication. No API keys required - the system uses Minecraft's built-in sessionserver authentication.

**What this provides:**
- **Personal Aspect Tracking:** Players' aspect collections stored securely
- **Crowdsourced Loot Pools:** Community-verified raid loot pools (weekly reset)
- **Crowdsourced Gambits:** Community-verified daily gambits (daily reset)
- **Mojang Authentication:** Cryptographic proof of player ownership without exposing session IDs

---

## 📋 Prerequisites

Before starting, ensure you have:
- **Java 17 or later** installed
- **PostgreSQL** database (or you can use H2 for testing)
- **Maven** (included as `mvnw` wrapper)
- Basic command line knowledge
- Git (optional, for version control)

---

## 🚀 Quick Start (5 Steps)

### Step 1: Get the Code

```bash
# Clone or copy this repository
git clone <your-repo-url>
cd wynnextras_server_new
```

### Step 2: Configure Database

Create `src/main/resources/application.properties`:

```properties
# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/wynnextras
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Server Port
server.port=8080

# Timezone (for Wynncraft reset times)
spring.jpa.properties.hibernate.jdbc.time_zone=CET
```

**For testing with H2 (in-memory database):**
```properties
spring.datasource.url=jdbc:h2:mem:wynnextras
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
server.port=8080
```

### Step 3: Build the Application

```bash
# On Linux/Mac:
./mvnw clean package

# On Windows:
mvnw.cmd clean package
```

### Step 4: Run the Server

```bash
java -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar
```

You should see:
```
Started WynnextrasServerApplication in X seconds
```

### Step 5: Test It Works

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

**✅ Server is now running!**

---

## 🔧 Detailed Configuration

### Database Setup

#### PostgreSQL (Production)

1. **Create database:**
```sql
CREATE DATABASE wynnextras;
CREATE USER wynnextras_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE wynnextras TO wynnextras_user;
```

2. **Tables are auto-created** by JPA on first run. But if you need to create them manually:

```sql
-- Personal aspect tracking
CREATE TABLE personal_aspect (
    id BIGSERIAL PRIMARY KEY,
    player_uuid VARCHAR(32) NOT NULL,
    player_name VARCHAR(255) NOT NULL,
    aspect_name VARCHAR(255) NOT NULL,
    rarity VARCHAR(50) NOT NULL,
    amount INT NOT NULL,
    mod_version VARCHAR(50),
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(player_uuid, aspect_name)
);

-- Loot pool submissions (crowdsourcing)
CREATE TABLE raid_lootpool_submission (
    id BIGSERIAL PRIMARY KEY,
    raid_type VARCHAR(10) NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    aspects_json TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    week_identifier VARCHAR(10) NOT NULL
);

-- Approved loot pools
CREATE TABLE raid_lootpool_approved (
    id BIGSERIAL PRIMARY KEY,
    raid_type VARCHAR(10) NOT NULL,
    aspects_json TEXT NOT NULL,
    approved_at TIMESTAMP NOT NULL,
    week_identifier VARCHAR(10) NOT NULL,
    locked BOOLEAN NOT NULL,
    submission_count INT NOT NULL
);

-- Gambit submissions
CREATE TABLE gambit_submission (
    id BIGSERIAL PRIMARY KEY,
    gambits_json TEXT NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    day_identifier VARCHAR(10) NOT NULL
);

-- Approved gambits
CREATE TABLE gambit_approved (
    id BIGSERIAL PRIMARY KEY,
    gambits_json TEXT NOT NULL,
    approved_at TIMESTAMP NOT NULL,
    day_identifier VARCHAR(10) NOT NULL,
    locked BOOLEAN NOT NULL
);

-- Verified users (trusted contributors)
CREATE TABLE verified_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    added_at TIMESTAMP NOT NULL
);
```

### Add Verified Users

Verified users can instantly approve loot pools (bypasses the 3-submission requirement).

**Method 1: Direct SQL**
```sql
INSERT INTO verified_user (username, added_at)
VALUES ('MinecraftUsername1', NOW()),
       ('MinecraftUsername2', NOW());
```

**Method 2: REST Endpoint (TODO: implement admin endpoint)**
You can add an admin endpoint to manage verified users programmatically.

---

## 🔐 How Authentication Works

### The Mojang Authentication Flow

**Why this is secure:**
- ✅ No session IDs or passwords sent to backend
- ✅ Cryptographic proof via Mojang's verification
- ✅ Replay attack prevention (30-second expiry)
- ✅ Uses same method as Minecraft multiplayer servers

**The flow:**

```
CLIENT (Minecraft Mod)                    BACKEND (Spring Boot)              MOJANG SESSIONSERVER
       |                                         |                                    |
       | 1. Generate random serverId            |                                    |
       |    (shared secret)                     |                                    |
       |---------------------------------------->|                                    |
       | 2. Call Mojang joinServer              |                                    |
       |    with accessToken + serverId         |                                    |
       |----------------------------------------------------------------------->|
       |                                         |     3. Session validated           |
       |<-----------------------------------------------------------------------|
       |                                         |                                    |
       | 4. POST /user (or /lootpool, /gambit)  |                                    |
       |    Headers: Username, Server-ID        |                                    |
       |---------------------------------------->|                                    |
       |                                         |                                    |
       |                                         | 5. Call hasJoined(username, serverId)
       |                                         |----------------------------------->|
       |                                         |                                    |
       |                                         | 6. Returns verified UUID + username|
       |                                         |<-----------------------------------|
       |                                         |                                    |
       |                                         | 7. Use verified UUID              |
       |                                         |    (not what client claimed!)     |
       |                                         |                                    |
       | 8. Response (success/error)            |                                    |
       |<----------------------------------------|                                    |
```

**Key Security Features:**

1. **Session ID Never Exposed:** The Minecraft session ID stays in the client. Only the access token is used (sent to Mojang, not to us).

2. **Cryptographic Proof:** Only the real player with a valid Minecraft session can call `joinServer` successfully.

3. **Replay Prevention:** Each `serverId` can only be used once within 30 seconds. The backend tracks used server IDs.

4. **No Spoofing:** Even if a malicious client claims to be "PlayerX", the backend asks Mojang "did PlayerX actually authenticate?" and uses Mojang's verified UUID.

---

## 📡 API Endpoints

### 1. Personal Aspect Tracking

#### POST `/user` - Upload Personal Aspects

**Headers:**
```
Username: MinecraftUsername
Server-ID: generated_server_id
Content-Type: application/json
```

**Request Body:**
```json
{
  "playerName": "MinecraftUsername",
  "modVersion": "0.12.1",
  "aspects": [
    {
      "name": "Aspect of Fire",
      "rarity": "Mythic",
      "amount": 5
    },
    {
      "name": "Aspect of Water",
      "rarity": "Fabled",
      "amount": 12
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Aspects uploaded successfully"
}
```

**Response (401 Unauthorized):**
```json
{
  "status": "error",
  "message": "Authentication failed - invalid session"
}
```

#### GET `/user?playerUuid=xxx` - Get Player's Aspects

**No authentication required** - anyone can view anyone's aspects.

**Query Parameters:**
- `playerUuid` - Player's UUID (with or without dashes)

**Response:**
```json
{
  "playerUuid": "a1b2c3d4...",
  "playerName": "MinecraftUsername",
  "modVersion": "0.12.1",
  "updatedAt": 1738000000000,
  "aspects": [
    {
      "name": "Aspect of Fire",
      "rarity": "Mythic",
      "amount": 5
    }
  ]
}
```

---

### 2. Loot Pool Crowdsourcing

#### POST `/lootpool/{raidType}` - Submit Loot Pool

**Path Parameters:**
- `raidType` - One of: `NOTG`, `NOL`, `TCC`, `TNA`

**Headers:**
```
Username: MinecraftUsername
Server-ID: generated_server_id
Content-Type: application/json
```

**Request Body:**
```json
{
  "aspects": [
    {
      "name": "Aspect of Courage",
      "rarity": "Mythic",
      "requiredClass": "Warrior"
    },
    {
      "name": "Aspect of Wisdom",
      "rarity": "Fabled",
      "requiredClass": "Mage"
    }
  ]
}
```

**Response (Approved):**
```json
{
  "status": "approved",
  "message": "Loot pool approved for NOTG",
  "lootPool": {
    "aspects": [...]
  }
}
```

**Response (Pending):**
```json
{
  "status": "submitted",
  "message": "Loot pool submitted. Waiting for more confirmations."
}
```

**Approval Logic:**
- 1 verified user → ✅ Instant approval
- 3 matching submissions → ✅ Approved
- 10 matching submissions → ✅ Approved + 🔒 Locked (cannot change)

#### GET `/lootpool/{raidType}` - Get Approved Loot Pool

**Response:**
```json
{
  "aspects": [
    {
      "name": "Aspect of Courage",
      "rarity": "Mythic",
      "requiredClass": "Warrior"
    }
  ]
}
```

---

### 3. Gambit Crowdsourcing

#### POST `/gambit` - Submit Gambits

**Headers:**
```
Username: MinecraftUsername
Server-ID: generated_server_id
Content-Type: application/json
```

**Request Body:**
```json
{
  "gambits": [
    {
      "name": "Glutton's Gambit",
      "description": "Eat 100 foods to gain damage boost"
    },
    {
      "name": "Speed Demon",
      "description": "Run 10000 blocks for speed buff"
    }
  ]
}
```

**Response (Approved):**
```json
{
  "status": "approved",
  "message": "Gambits approved for today",
  "gambits": {
    "gambits": [...]
  }
}
```

**Approval Logic:**
- 2 matching submissions → ✅ Approved + 🔒 Locked

#### GET `/gambit` - Get Today's Gambits

**Response:**
```json
{
  "gambits": [
    {
      "name": "Glutton's Gambit",
      "description": "Eat 100 foods to gain damage boost"
    }
  ]
}
```

---

## ⏰ Reset Schedule

All times in **CET (Central European Time)** - Wynncraft's timezone.

### Loot Pools
- **Reset:** Every Friday at 19:00 CET
- **Duration:** 1 week (Friday to Friday)
- **Identifier:** `2026-W04` (ISO week number)

### Gambits
- **Reset:** Every day at 19:00 CET
- **Duration:** 24 hours
- **Identifier:** `2026-01-27` (ISO date)

### How Resets Work

When the clock passes 19:00 CET:
1. Old data becomes irrelevant (different identifier)
2. New submissions start fresh
3. Clients can submit to the new time period

**Example:**
- Jan 24 (Friday) 18:59 CET → Identifier: `2026-W04`
- Jan 24 (Friday) 19:00 CET → Identifier: `2026-W05` (new week starts)
- Jan 31 (Friday) 19:00 CET → Identifier: `2026-W06` (another week)

---

## 🧪 Testing the Server

### Test Personal Aspects (Requires Minecraft Client)

You'll need the WynnExtras mod running to generate valid authentication:

1. Start Minecraft with WynnExtras mod
2. Join Wynncraft
3. Run `/we aspects scan` or open a reward chest
4. Check server logs for: `Successfully authenticated player X (UUID: Y)`

### Test Crowdsourcing (Without Client)

You can't test authentication without the client, but you can test GET endpoints:

```bash
# Should return 404 (no approved loot pool yet)
curl http://localhost:8080/lootpool/NOTG

# Should return 404 (no approved gambits yet)
curl http://localhost:8080/gambit
```

### Add Test Data Manually (For Testing UI)

```sql
-- Add a test loot pool
INSERT INTO raid_lootpool_approved (
    raid_type, aspects_json, approved_at, week_identifier, locked, submission_count
) VALUES (
    'NOTG',
    '[{"name":"Aspect of Fire","rarity":"Mythic","requiredClass":"Warrior"}]',
    NOW(),
    '2026-W04',
    false,
    3
);

-- Add test gambits
INSERT INTO gambit_approved (
    gambits_json, approved_at, day_identifier, locked
) VALUES (
    '[{"name":"Test Gambit","description":"This is a test"}]',
    NOW(),
    '2026-01-27',
    true
);
```

Now GET requests will return data:
```bash
curl http://localhost:8080/lootpool/NOTG
# Returns the test loot pool

curl http://localhost:8080/gambit
# Returns the test gambits
```

---

## 🐛 Troubleshooting

### Server Won't Start

**Error: "Port 8080 is already in use"**
```bash
# Find process using port 8080
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in application.properties
server.port=8081
```

**Error: "Could not connect to database"**
- Check PostgreSQL is running: `pg_isready`
- Verify database credentials in `application.properties`
- Ensure database exists: `psql -l`

### Authentication Failing

**Error: "Authentication failed - invalid session"**

This means Mojang's sessionserver rejected the authentication. Causes:
1. Client not properly logged into Minecraft
2. Network issues between your server and Mojang
3. Invalid serverId (already used or expired)

**Solution:**
- Player should restart Minecraft
- Check server can reach `https://sessionserver.mojang.com`
- Check server logs for detailed error messages

**Error: "Authentication token already used"**

This is replay attack prevention working correctly. The client tried to reuse a serverId.

**Solution:**
- This is normal if a request is retried
- Client will generate a new serverId automatically
- No action needed

### Database Issues

**Error: "Table 'personal_aspect' doesn't exist"**
- Check `spring.jpa.hibernate.ddl-auto=update` in application.properties
- Or manually create tables using SQL above

**Error: "Too many connections"**
```properties
# Add to application.properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

### Performance Issues

**Slow response times:**
```properties
# Add database indexes
CREATE INDEX idx_personal_aspect_uuid ON personal_aspect(player_uuid);
CREATE INDEX idx_lootpool_submission_week ON raid_lootpool_submission(week_identifier);
CREATE INDEX idx_gambit_submission_day ON gambit_submission(day_identifier);
```

**Memory usage high:**
```bash
# Limit Java heap size
java -Xmx512m -Xms256m -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar
```

---

## 🔧 Production Deployment

### Using Docker (Recommended)

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/wynnextras_server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: wynnextras
      POSTGRES_USER: wynnextras_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wynnextras
      SPRING_DATASOURCE_USERNAME: wynnextras_user
      SPRING_DATASOURCE_PASSWORD: secure_password
    depends_on:
      - postgres

volumes:
  postgres_data:
```

Deploy:
```bash
docker-compose up -d
```

### Using Systemd (Linux)

Create `/etc/systemd/system/wynnextras.service`:
```ini
[Unit]
Description=WynnExtras Backend Server
After=network.target postgresql.service

[Service]
Type=simple
User=wynnextras
WorkingDirectory=/opt/wynnextras
ExecStart=/usr/bin/java -jar /opt/wynnextras/wynnextras_server.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable wynnextras
sudo systemctl start wynnextras
sudo systemctl status wynnextras
```

### Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name wynnextras.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 📊 Monitoring

### Health Checks

Spring Boot Actuator provides health endpoints:

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db
```

### Logs

```bash
# View logs (if using systemd)
sudo journalctl -u wynnextras -f

# View logs (if running in terminal)
# Logs are printed to stdout

# Configure logging in application.properties
logging.level.com.julianh06.wynnextras_server=INFO
logging.level.org.springframework=WARN
```

### Statistics Endpoint (TODO)

You can add a custom statistics endpoint to monitor crowdsourcing:

```java
@GetMapping("/stats")
public Map<String, Object> getStats() {
    Map<String, Object> stats = new HashMap<>();

    // Count personal aspects
    long totalAspects = personalAspectRepo.count();
    stats.put("totalPersonalAspects", totalAspects);

    // Count loot pool submissions this week
    String weekId = TimeUtils.getWeekIdentifier();
    long lootPoolSubs = lootPoolSubmissionRepo.countByWeekIdentifier(weekId);
    stats.put("lootPoolSubmissionsThisWeek", lootPoolSubs);

    // Count gambit submissions today
    String dayId = TimeUtils.getDayIdentifier();
    long gambitSubs = gambitSubmissionRepo.countByDayIdentifier(dayId);
    stats.put("gambitSubmissionsToday", gambitSubs);

    return stats;
}
```

---

## ✅ Deployment Checklist

### Initial Setup
- [ ] Java 17+ installed
- [ ] PostgreSQL installed and running
- [ ] Database created
- [ ] `application.properties` configured
- [ ] Code built successfully (`mvnw package`)

### Testing
- [ ] Server starts without errors
- [ ] Health endpoint responds: `curl http://localhost:8080/actuator/health`
- [ ] Database tables created (check with `\dt` in psql)
- [ ] GET endpoints return 404 (no data yet - expected)

### Security
- [ ] Change default database password
- [ ] Use HTTPS in production (nginx + Let's Encrypt)
- [ ] Firewall configured (only port 80/443 public)
- [ ] Database not exposed to internet

### Production
- [ ] Systemd service created (or Docker container running)
- [ ] Automatic restart on failure configured
- [ ] Logs being captured
- [ ] Backup strategy for database
- [ ] Domain name configured (if applicable)

### Client Integration
- [ ] Backend URL configured in client mod
- [ ] Test personal aspect upload from game
- [ ] Test loot pool crowdsourcing from game
- [ ] Test gambit detection from game
- [ ] Verified users added (if applicable)

---

## 🎉 You're Done!

Your WynnExtras backend is now fully deployed with secure Mojang authentication!

**Key Features:**
- ✅ Personal aspect tracking with cryptographic authentication
- ✅ Crowdsourced loot pools (3-submission consensus)
- ✅ Crowdsourced gambits (2-submission consensus)
- ✅ Automatic weekly/daily resets (CET timezone)
- ✅ Verified user system for trusted contributors
- ✅ Replay attack prevention
- ✅ No API keys or session IDs exposed

**Next Steps:**
1. Monitor server logs for authentication requests
2. Add verified users as needed
3. Set up monitoring/alerting (optional)
4. Configure backups for database

**Need Help?**
- Check server logs: Look for `MojangAuthService` messages
- Check database: `SELECT * FROM personal_aspect LIMIT 10;`
- Test authentication: Have a player use `/we aspects scan` in-game

---

**Written for:** WynnExtras Backend v1.0
**Last Updated:** January 2026
**Authentication:** Mojang Sessionserver (Industry Standard)
