package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class PersonalAspectDto {

    public static class AspectData {
        private String name;
        private String rarity;
        private int amount;

        public AspectData() {}

        public AspectData(String name, String rarity, int amount) {
            this.name = name;
            this.rarity = rarity;
            this.amount = amount;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRarity() { return rarity; }
        public void setRarity(String rarity) { this.rarity = rarity; }

        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }

    public static class UploadRequest {
        private String playerName;
        private String modVersion;
        private List<AspectData> aspects;

        public UploadRequest() {}

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public List<AspectData> getAspects() { return aspects; }
        public void setAspects(List<AspectData> aspects) { this.aspects = aspects; }
    }

    public static class PlayerAspectsResponse {
        private String playerUuid;
        private String playerName;
        private String modVersion;
        private long updatedAt;
        private List<AspectData> aspects;

        public PlayerAspectsResponse() {}

        public PlayerAspectsResponse(String playerUuid, String playerName, String modVersion, long updatedAt, List<AspectData> aspects) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.modVersion = modVersion;
            this.updatedAt = updatedAt;
            this.aspects = aspects;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public List<AspectData> getAspects() { return aspects; }
        public void setAspects(List<AspectData> aspects) { this.aspects = aspects; }
    }
}
