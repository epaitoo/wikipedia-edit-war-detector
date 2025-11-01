package com.epaitoo.springboot.service;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.mapper.AlertMapper;
import com.epaitoo.springboot.persistence.repository.EditWarAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing Edit War Alerts
 *
 * Responsibilities:
 * - CRUD operations on alerts
 * - Business logic (search, filter, statistics)
 * - Transaction management
 * - Orchestration between repository and mapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {
    private final EditWarAlertRepository alertRepository;
    private final AlertMapper alertMapper;

    /**
     * Get all alerts with pagination
     *
     * @param pageable Pagination parameters
     * @return Page of alerts
     */
    public Page<EditWarAlert> getAllAlerts(Pageable pageable) {
        log.debug("Getting all alerts with pagination: {}", pageable);
        Page<EditWarAlertEntity> entityPage = alertRepository.findAll(pageable);

        return entityPage.map(alertMapper::toDomain);
    }

    /**
     * Get alert by ID
     *
     * @param id Alert ID
     * @return Optional containing alert if found
     */
    public Optional<EditWarAlert> getAlertById(Long id) {
        log.debug("Getting alert by ID: {}", id);

        return alertRepository.findById(id)
                .map(alertMapper::toDomain);
    }

    /**
     * Get alerts by status
     *
     * @param status Alert status (ACTIVE, RESOLVED, etc.)
     * @return List of alerts with given status
     */
    public List<EditWarAlert> getAlertsByStatus(EditWarStatus status) {
        log.debug("Getting alerts by status: {}", status);

        EditWarAlertEntity.AlertStatus entityStatus = mapStatus(status);
        List<EditWarAlertEntity> entities = alertRepository.findByStatus(entityStatus);

        return entities.stream()
                .map(alertMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by severity level
     *
     * @param severityLevel Severity level (LOW, MEDIUM, HIGH, CRITICAL)
     * @return List of alerts with given severity
     */
    public List<EditWarAlert> getAlertsBySeverity(String severityLevel) {
        log.debug("Getting alerts by severity: {}", severityLevel);

        EditWarAlertEntity.SeverityLevel level = EditWarAlertEntity.SeverityLevel.valueOf(
                severityLevel.toUpperCase()
        );

        List<EditWarAlertEntity> entities = alertRepository.findBySeverityLevel(level);

        return entities.stream()
                .map(alertMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Search alerts by page title (case-insensitive)
     *
     * @param keyword Search keyword
     * @return List of matching alerts
     */
    public List<EditWarAlert> searchByPageTitle(String keyword) {
        log.debug("Searching alerts by page title: {}", keyword);

        List<EditWarAlertEntity> entities = alertRepository
                .findByPageTitleContainingIgnoreCase(keyword);

        return entities.stream()
                .map(alertMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get recent active alerts (ordered by detection time)
     *
     * @return List of recent active alerts
     */
    public List<EditWarAlert> getRecentActiveAlerts() {
        log.debug("Getting recent active alerts");

        List<EditWarAlertEntity> entities = alertRepository
                .findByStatusOrderByDetectedAtDesc(EditWarAlertEntity.AlertStatus.ACTIVE);

        return entities.stream()
                .map(alertMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts by wiki (e.g., en.wikipedia.org)
     *
     * @param wiki Wiki domain
     * @return List of alerts for given wiki
     */
    public List<EditWarAlert> getAlertsByWiki(String wiki) {
        log.debug("Getting alerts by wiki: {}", wiki);

        List<EditWarAlertEntity> entities = alertRepository.findByWiki(wiki);

        return entities.stream()
                .map(alertMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get alert statistics
     *
     * @return Map containing various statistics
     */
    public Map<String, Object> getStatistics() {
        log.debug("Getting alert statistics");

        Map<String, Object> stats = new HashMap<>();

        // Total counts
        stats.put("totalAlerts", alertRepository.count());
        stats.put("activeAlerts", alertRepository.countByStatus(
                EditWarAlertEntity.AlertStatus.ACTIVE));
        stats.put("resolvedAlerts", alertRepository.countByStatus(
                EditWarAlertEntity.AlertStatus.RESOLVED));

        return stats;
    }

    /**
     * Count total alerts
     *
     * @return Total number of alerts
     */
    public long countAlerts() {
        return alertRepository.count();
    }

    /**
     * Count alerts by status
     *
     * @param status Alert status
     * @return Count of alerts with given status
     */
    public long countByStatus(EditWarStatus status) {
        EditWarAlertEntity.AlertStatus entityStatus = mapStatus(status);
        return alertRepository.countByStatus(entityStatus);
    }

    /**
     * Map domain status to entity status
     */
    private EditWarAlertEntity.AlertStatus mapStatus(EditWarStatus status) {
        return switch (status) {
            case ACTIVE -> EditWarAlertEntity.AlertStatus.ACTIVE;
            case RESOLVED -> EditWarAlertEntity.AlertStatus.RESOLVED;
            case ESCALATING -> EditWarAlertEntity.AlertStatus.ESCALATING;
            case COOLING_DOWN -> EditWarAlertEntity.AlertStatus.COOLING_DOWN;
        };
    }

}
