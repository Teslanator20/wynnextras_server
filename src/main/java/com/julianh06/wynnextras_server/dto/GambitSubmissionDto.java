package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class GambitSubmissionDto {
    private List<GambitDto> gambits;

    public GambitSubmissionDto() {}

    public GambitSubmissionDto(List<GambitDto> gambits) {
        this.gambits = gambits;
    }

    public List<GambitDto> getGambits() {
        return gambits;
    }

    public void setGambits(List<GambitDto> gambits) {
        this.gambits = gambits;
    }

    public static class GambitDto {
        private String name;
        private String description;

        public GambitDto() {}

        public GambitDto(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
