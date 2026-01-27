# WynnExtras Backend Server

Secure backend API for WynnExtras Minecraft mod with Mojang sessionserver authentication.

## 🚀 Quick Start

```bash
# 1. Configure database (create application.properties)
# See IMPLEMENTATION.md for details

# 2. Build
./mvnw clean package

# 3. Run
java -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar

# 4. Test
curl http://localhost:8080/actuator/health
```

## 📚 Documentation

- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Complete step-by-step deployment guide (START HERE)
- **[CROWDSOURCING_README.md](CROWDSOURCING_README.md)** - Crowdsourcing system overview
- **[VERIFIED_USERS_SETUP.md](VERIFIED_USERS_SETUP.md)** - Managing trusted contributors

## 🔐 Security

This backend uses **Mojang sessionserver authentication** - the same method used by Minecraft multiplayer servers.

**Key Features:**
- ✅ No API keys required
- ✅ No session IDs exposed to backend
- ✅ Cryptographic proof of player ownership
- ✅ Replay attack prevention
- ✅ Industry-standard authentication flow

## 🎯 Features

### 1. Personal Aspect Tracking
- Players can upload their aspect collections
- Secure authentication via Mojang
- View any player's aspects publicly

### 2. Loot Pool Crowdsourcing
- Community-verified raid loot pools
- Consensus-based approval (3 submissions or 1 verified user)
- Weekly reset (Friday 19:00 CET)
- Auto-locking at 10 submissions

### 3. Gambit Crowdsourcing
- Community-verified daily gambits
- Consensus-based approval (2 submissions)
- Daily reset (19:00 CET)

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA**
- **PostgreSQL** (or H2 for testing)
- **Gson** (JSON parsing)
- **Mojang Sessionserver API** (authentication)

## 📡 API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/user` | POST | ✅ | Upload personal aspects |
| `/user?playerUuid=xxx` | GET | ❌ | Get player's aspects |
| `/lootpool/{raidType}` | POST | ✅ | Submit loot pool |
| `/lootpool/{raidType}` | GET | ❌ | Get approved loot pool |
| `/gambit` | POST | ✅ | Submit gambits |
| `/gambit` | GET | ❌ | Get approved gambits |

**Auth:** Mojang sessionserver authentication (Username + Server-ID headers)

## 🏗️ Project Structure

```
src/main/java/com/julianh06/wynnextras_server/
├── MojangAuthService.java          # Authentication service
│
├── PersonalAspect.java             # Personal aspect entity
├── PersonalAspectRepository.java   # JPA repository
├── PersonalAspectController.java   # REST endpoints
├── PersonalAspectDto.java          # Request/response DTOs
│
├── RaidLootPoolSubmission.java     # Loot pool submission entity
├── RaidLootPoolApproved.java       # Approved loot pool entity
├── RaidLootPoolSubmissionRepository.java
├── RaidLootPoolApprovedRepository.java
├── LootPoolService.java            # Business logic
├── LootPoolController.java         # REST endpoints
├── LootPoolSubmissionDto.java      # DTOs
│
├── GambitSubmission.java           # Gambit submission entity
├── GambitApproved.java             # Approved gambit entity
├── GambitSubmissionRepository.java
├── GambitApprovedRepository.java
├── GambitService.java              # Business logic
├── GambitController.java           # REST endpoints
├── GambitSubmissionDto.java        # DTOs
│
├── VerifiedUser.java               # Verified user entity
├── VerifiedUserRepository.java     # JPA repository
│
└── TimeUtils.java                  # CET timezone utilities
```

## 🗄️ Database Schema

### Tables Created Automatically
- `personal_aspect` - Player aspect collections
- `raid_lootpool_submission` - All loot pool submissions
- `raid_lootpool_approved` - Approved loot pools
- `gambit_submission` - All gambit submissions
- `gambit_approved` - Approved gambits
- `verified_user` - Trusted contributors

**JPA auto-creates tables on startup** (when `spring.jpa.hibernate.ddl-auto=update`)

## ⏰ Reset Schedule

All times in **CET (Central European Time)** - Wynncraft's timezone.

- **Loot Pools:** Weekly reset - Friday 19:00 CET
- **Gambits:** Daily reset - Every day 19:00 CET

## 🔒 Authentication Flow

```
1. Client generates random serverId
2. Client calls Mojang's joinServer(uuid, accessToken, serverId)
3. Client sends request to backend with Username + Server-ID headers
4. Backend calls Mojang's hasJoined(username, serverId)
5. Mojang returns verified UUID (proof of ownership)
6. Backend processes request using verified UUID
```

**This is the same authentication used by:**
- Every Minecraft multiplayer server
- Mojang's official authentication
- Industry standard for Minecraft

## 🧪 Testing

### With Minecraft Client
```bash
# Start the server
java -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar

# In Minecraft (with WynnExtras mod):
/we aspects scan

# Check server logs for:
# "Successfully authenticated player X (UUID: Y)"
```

### Without Client (GET endpoints only)
```bash
# Health check
curl http://localhost:8080/actuator/health

# Try fetching loot pool (will return 404 until submitted)
curl http://localhost:8080/lootpool/NOTG

# Try fetching gambits (will return 404 until submitted)
curl http://localhost:8080/gambit
```

## 🐳 Docker Deployment

```bash
# Build
./mvnw clean package
docker build -t wynnextras-backend .

# Run with Docker Compose
docker-compose up -d
```

See `IMPLEMENTATION.md` for full Docker setup.

## 🐛 Troubleshooting

**Port already in use:**
```properties
# Change port in application.properties
server.port=8081
```

**Database connection failed:**
```bash
# Check PostgreSQL is running
pg_isready

# Verify credentials in application.properties
```

**Authentication failing:**
- Player should restart Minecraft (refresh session)
- Check server can reach `https://sessionserver.mojang.com`
- Check logs for detailed error messages

**See [IMPLEMENTATION.md](IMPLEMENTATION.md) for detailed troubleshooting.**

## 📊 Monitoring

```bash
# View health status
curl http://localhost:8080/actuator/health

# View logs (systemd)
sudo journalctl -u wynnextras -f

# Database statistics
psql wynnextras -c "SELECT COUNT(*) FROM personal_aspect;"
```

## 🤝 Contributing

This is a private backend for the WynnExtras mod. If you're deploying your own instance:

1. Follow [IMPLEMENTATION.md](IMPLEMENTATION.md) for setup
2. Test thoroughly before production use
3. Keep dependencies updated (`./mvnw versions:display-dependency-updates`)
4. Report issues to WynnExtras developers

## 📝 Version History

### v1.0.0 (Current)
- ✅ Mojang sessionserver authentication
- ✅ Personal aspect tracking
- ✅ Loot pool crowdsourcing
- ✅ Gambit crowdsourcing
- ✅ Verified user system
- ✅ Automatic resets (CET timezone)
- ✅ Replay attack prevention

### v0.1.0 (Deprecated)
- ❌ Used Wynncraft API keys (insecure)
- ❌ Replaced with Mojang authentication

## 📄 License

Private - Part of WynnExtras ecosystem

## 🔗 Links

- **WynnExtras Mod:** [GitHub Repository]
- **Implementation Guide:** [IMPLEMENTATION.md](IMPLEMENTATION.md)
- **Wynncraft:** https://wynncraft.com
- **Mojang API:** https://wiki.vg/Mojang_API

---

**For deployment instructions, read [IMPLEMENTATION.md](IMPLEMENTATION.md)**
