package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class LootPoolSubmissionDto {
    private List<AspectDto> aspects;

    public LootPoolSubmissionDto() {}

    public LootPoolSubmissionDto(List<AspectDto> aspects) {
        this.aspects = aspects;
    }

    public List<AspectDto> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDto> aspects) {
        this.aspects = aspects;
    }

    public static class AspectDto {
        private String name;
        private String rarity;
        private String requiredClass;

        public AspectDto() {}

        public AspectDto(String name, String rarity, String requiredClass) {
            this.name = name;
            this.rarity = rarity;
            this.requiredClass = requiredClass;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRarity() {
            return rarity;
        }

        public void setRarity(String rarity) {
            this.rarity = rarity;
        }

        public String getRequiredClass() {
            return requiredClass;
        }

        public void setRequiredClass(String requiredClass) {
            this.requiredClass = requiredClass;
        }
    }
}
