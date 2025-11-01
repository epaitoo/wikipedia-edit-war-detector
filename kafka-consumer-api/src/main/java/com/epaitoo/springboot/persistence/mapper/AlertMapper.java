package com.epaitoo.springboot.persistence.mapper;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AlertMapper {
    /**
     * Convert domain object to database entity
     *
     * @param alert Business/domain object
     * @return Database entity ready to be persisted
     */
    public EditWarAlertEntity toEntity(EditWarAlert alert) {
        if (alert == null) {
            return null;
        }

        return EditWarAlertEntity.builder()
                // Page information
                .pageTitle(alert.getPageTitle())
                .wiki(alert.getWiki())

                // Severity metrics
                .severityLevel(mapSeverityLevel(alert.getSeverityLevel()))
                .severityScore(doubleToBigDecimal(alert.getSeverityScore()))

                // Edit metrics
                .totalEdits(alert.getTotalEdits())
                .conflictEdits(alert.getConflictEdits())
                .conflictRatio(doubleToBigDecimal(alert.getConflictRatio()))

                // User information
                .userCount(alert.getUserCount())
                .involvedUsers(alert.getInvolvedUsers())

                // Timestamps
                .firstEditTimestamp(alert.getFirstEditTimestamp())
                .lastEditTimestamp(alert.getLastEditTimestamp())
                .detectedAt(alert.getDetectedAt())

                // Status
                .status(mapStatus(alert.getStatus()))

                .build();
    }

    /**
     * Convert database entity to domain object
     *
     * @param entity Database entity
     * @return Business/domain object
     */
    public EditWarAlert toDomain(EditWarAlertEntity entity) {
        if (entity == null) {
            return null;
        }

        EditWarAlert alert = EditWarAlert.builder()
                // Page information
                .pageTitle(entity.getPageTitle())
                .wiki(entity.getWiki())

                // Severity metrics
                .severityScore(bigDecimalToDouble(entity.getSeverityScore()))

                // Edit metrics
                .totalEdits(entity.getTotalEdits())
                .conflictEdits(entity.getConflictEdits())
                .conflictRatio(bigDecimalToDouble(entity.getConflictRatio()))

                // User information
                .userCount(entity.getUserCount())
                .involvedUsers(entity.getInvolvedUsers())

                // Timestamps
                .firstEditTimestamp(entity.getFirstEditTimestamp())
                .lastEditTimestamp(entity.getLastEditTimestamp())
                .detectedAt(entity.getDetectedAt())

                // Status
                .status(mapStatusToDomain(entity.getStatus()))

                .build();

        return alert;
    }

    /**
     * Convert Double to BigDecimal (for database)
     * Handles null and rounds to 2 decimal places
     */
    private BigDecimal doubleToBigDecimal(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert BigDecimal to Double (from database)
     * Handles null
     */
    private Double bigDecimalToDouble(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

    /**
     * Map domain severity level to entity enum
     */
    private EditWarAlertEntity.SeverityLevel mapSeverityLevel(String severityLevel) {
        if (severityLevel == null) {
            return EditWarAlertEntity.SeverityLevel.LOW;
        }

        return switch (severityLevel.toUpperCase()) {
            case "CRITICAL" -> EditWarAlertEntity.SeverityLevel.CRITICAL;
            case "HIGH" -> EditWarAlertEntity.SeverityLevel.HIGH;
            case "MEDIUM" -> EditWarAlertEntity.SeverityLevel.MEDIUM;
            default -> EditWarAlertEntity.SeverityLevel.LOW;
        };
    }

    /**
     * Map domain status to entity status
     */
    private EditWarAlertEntity.AlertStatus mapStatus(EditWarStatus status) {
        if (status == null) {
            return EditWarAlertEntity.AlertStatus.ACTIVE;
        }

        return switch (status) {
            case ACTIVE -> EditWarAlertEntity.AlertStatus.ACTIVE;
            case RESOLVED -> EditWarAlertEntity.AlertStatus.RESOLVED;
            case ESCALATING -> EditWarAlertEntity.AlertStatus.ESCALATING;
            case COOLING_DOWN -> EditWarAlertEntity.AlertStatus.COOLING_DOWN;
        };
    }

    /**
     * Map entity status back to domain status
     */
    private EditWarStatus mapStatusToDomain(EditWarAlertEntity.AlertStatus status) {
        if (status == null) {
            return EditWarStatus.ACTIVE;
        }

        return switch (status) {
            case ACTIVE -> EditWarStatus.ACTIVE;
            case RESOLVED -> EditWarStatus.RESOLVED;
            case ESCALATING -> EditWarStatus.ESCALATING;
            case COOLING_DOWN -> EditWarStatus.COOLING_DOWN;
        };
    }
}
