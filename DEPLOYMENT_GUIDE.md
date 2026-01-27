# WynnExtras Backend - Deployment Guide

**⚠️ NOTICE: This document is deprecated. Please use [IMPLEMENTATION.md](IMPLEMENTATION.md) instead.**

---

## 📚 Updated Documentation

The deployment process has been completely rewritten with Mojang authentication. Please refer to:

- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Complete step-by-step deployment guide (**START HERE**)
- **[README.md](README.md)** - Quick overview and quickstart
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Technical architecture and design
- **[VERIFIED_USERS_SETUP.md](VERIFIED_USERS_SETUP.md)** - Managing verified users

---

## 🔄 What Changed?

### Old System (Deprecated)
- ❌ Used Wynncraft API keys
- ❌ Insecure (keys could be stolen)
- ❌ Required manual setup by users

### New System (Current)
- ✅ Uses Mojang sessionserver authentication
- ✅ Secure (cryptographic proof)
- ✅ Zero setup required (automatic)

### Key Differences

| Aspect | Old (Wynncraft API) | New (Mojang Auth) |
|--------|---------------------|-------------------|
| **Authentication** | Wynncraft API key | Mojang sessionserver |
| **Headers** | `Wynncraft-Api-Key` | `Username`, `Server-ID` |
| **User Setup** | `/we apikey <key>` | None (automatic) |
| **Security** | API keys can be stolen | Cryptographic proof |
| **Session IDs** | Not involved | Not exposed (secure) |

---

## 🚀 Migration Guide

If you have an old backend deployment using Wynncraft API keys:

### Step 1: Update Backend Code

Replace old controllers/services with new ones from the repository.

**Files to update:**
- `PersonalAspectController.java` - Now uses Mojang auth
- `LootPoolController.java` - Now uses Mojang auth
- `GambitController.java` - Now uses Mojang auth

**Files to add:**
- `MojangAuthService.java` - New authentication service

**Files to remove:**
- `WynncraftService.java` - No longer needed (if you had it)

### Step 2: Update Client Mod

Update WynnExtras mod to version 0.12.1 or later (includes Mojang auth).

**Client changes:**
- `MojangAuth.java` - New authentication helper
- `WynncraftApiHandler.java` - Updated to use Mojang auth
- `MinecraftClientAccessor.java` - New mixin for session access

### Step 3: Database Migration

The database schema is mostly the same, but personal aspects now use:
- Verified UUIDs from Mojang (more secure)
- Player names from Mojang (source of truth)

**No migration needed** - new uploads will use verified data.

### Step 4: Test Everything

1. Build and deploy new backend
2. Deploy new client mod
3. Test personal aspect upload: `/we aspects scan`
4. Test loot pool crowdsourcing: Open preview chest
5. Test gambit detection: Open party finder

---

## 📖 Quick Links

- **Full Deployment Guide:** [IMPLEMENTATION.md](IMPLEMENTATION.md)
- **Architecture Overview:** [ARCHITECTURE.md](ARCHITECTURE.md)
- **API Reference:** See IMPLEMENTATION.md
- **Troubleshooting:** See IMPLEMENTATION.md

---

**This document is kept for historical reference only.**
**For current deployment instructions, read [IMPLEMENTATION.md](IMPLEMENTATION.md)**
