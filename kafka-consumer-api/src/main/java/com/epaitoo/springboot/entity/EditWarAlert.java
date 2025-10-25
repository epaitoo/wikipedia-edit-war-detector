package com.epaitoo.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditWarAlert {
    // Identification
    private String pageTitle;
    private String wiki;

    // Participants
    private List<String> involvedUsers;
    private int userCount;

    // Metrics
    private int totalEdits;
    private int conflictEdits;
    private double conflictRatio;  // 0.0 to 1.0

    // Severity (for ranking which wars are "hottest")
    private double severityScore;  // 0.0 to 1.0

    // Timing
    private Instant detectedAt;
    private Long firstEditTimestamp;
    private Long lastEditTimestamp;

    // Status
    private EditWarStatus status;

    /**
     * Calculate severity based on multiple factors
     */
    public void calculateSeverity() {
        // Factors that make a war more severe:
        // 1. Higher conflict ratio
        // 2. More edits in short time
        // 3. More users involved (but capped at 3)

        double conflictWeight = conflictRatio * 0.5;  // 50% weight
        double editFrequency = Math.min(totalEdits / 10.0, 1.0) * 0.3;  // 30% weight
        double userWeight = Math.min(userCount / 3.0, 1.0) * 0.2;  // 20% weight

        this.severityScore = conflictWeight + editFrequency + userWeight;
        this.severityScore = Math.min(this.severityScore, 1.0); // Cap at 1.0
    }

    /**
     * Get human-readable severity level
     */
    public String getSeverityLevel() {
        if (severityScore >= 0.8) return "CRITICAL";
        if (severityScore >= 0.6) return "HIGH";
        if (severityScore >= 0.4) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get edit frequency (edits per minute)
     */
    public double getEditFrequency() {
        if (firstEditTimestamp == null || lastEditTimestamp == null) {
            return 0.0;
        }

        long durationMillis = lastEditTimestamp - firstEditTimestamp;
        if (durationMillis == 0) return totalEdits;

        double durationMinutes = durationMillis / 60000.0;
        return totalEdits / durationMinutes;
    }

}
