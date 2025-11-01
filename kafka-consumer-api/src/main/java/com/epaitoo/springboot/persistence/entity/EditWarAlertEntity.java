package com.epaitoo.springboot.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


/**
 * JPA Entity mapping to edit_war_alerts table
 * Stores detected edit war incidents
 */
@Entity
@Table(
        name = "edit_war_alerts",
        indexes = {
                @Index(name = "idx_alerts_detected_at", columnList = "detected_at"),
                @Index(name = "idx_alerts_page_title", columnList = "page_title"),
                @Index(name = "idx_alerts_severity", columnList = "severity_level"),
                @Index(name = "idx_alerts_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditWarAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @NotBlank(message = "Page title is required")
    @Size(max = 500, message = "Page title must be less than 500 characters")
    @Column(name = "page_title", nullable = false, length = 500)
    private String pageTitle;

    @NotBlank(message = "Wiki is required")
    @Size(max = 100)
    @Column(name = "wiki", nullable = false, length = 100)
    private String wiki;

    @NotNull(message = "Severity level is required")
    @Column(name = "severity_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severityLevel;

    @NotNull
    @DecimalMin(value = "0.0", message = "Severity score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Severity score must be between 0.0 and 1.0")
    @Column(name = "severity_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal severityScore;

    @NotNull
    @Min(value = 5, message = "Total edits must be at least 5")
    @Column(name = "total_edits", nullable = false)
    private Integer totalEdits;

    @NotNull
    @Min(value = 0)
    @Column(name = "conflict_edits", nullable = false)
    private Integer conflictEdits;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "conflict_ratio", nullable = false, precision = 3, scale = 2)
    private BigDecimal conflictRatio;

    @NotNull
    @Min(value = 2)
    @Max(value = 3)
    @Column(name = "user_count", nullable = false)
    private Integer userCount;

    /**
     * Store as JSON array in TEXT column
     * Example: ["Alice", "Bob"]
     * We'll use @Convert to handle List <-> JSON conversion
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "involved_users", nullable = false, columnDefinition = "TEXT")
    private List<String> involvedUsers;

    @NotNull
    @Column(name = "first_edit_timestamp", nullable = false)
    private Long firstEditTimestamp;

    @NotNull
    @Column(name = "last_edit_timestamp", nullable = false)
    private Long lastEditTimestamp;

    @NotNull
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SeverityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        ACTIVE,
        RESOLVED,
        ESCALATING,
        COOLING_DOWN
    }

}
