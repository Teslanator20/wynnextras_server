package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gambit_submission")
public class GambitSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String gambitsJson; // JSON array of 3-4 gambits (sorted)

    @Column(nullable = false)
    private String submittedBy; // username

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 10)
    private String dayIdentifier; // e.g., "2026-01-27"

    public GambitSubmission() {}

    public GambitSubmission(String gambitsJson, String submittedBy, String dayIdentifier) {
        this.gambitsJson = gambitsJson;
        this.submittedBy = submittedBy;
        this.dayIdentifier = dayIdentifier;
        this.submittedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGambitsJson() { return gambitsJson; }
    public void setGambitsJson(String gambitsJson) { this.gambitsJson = gambitsJson; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getDayIdentifier() { return dayIdentifier; }
    public void setDayIdentifier(String dayIdentifier) { this.dayIdentifier = dayIdentifier; }
}
