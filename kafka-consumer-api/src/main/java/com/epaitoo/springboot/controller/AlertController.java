package com.epaitoo.springboot.controller;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    /**
     * GET /api/alerts?page=0&size=20&sort=detectedAt,desc
     *
     * Get all alerts with pagination and sorting
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: detectedAt)
     * @param sortDir Sort direction (default: desc)
     * @return Paginated alerts
     */
    @GetMapping("/alerts")
    public Mono<Page<EditWarAlert>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "detectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/alerts - page: {}, size: {}, sort: {},{}",
                page, size, sortBy, sortDir);

        return Mono.fromCallable(() -> {
            // Create pageable with sorting
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);

            // Get alerts from service
            Page<EditWarAlert> alerts = alertService.getAllAlerts(pageable);

            log.info("Returning {} alerts (page {} of {})",
                    alerts.getNumberOfElements(),
                    alerts.getNumber() + 1,
                    alerts.getTotalPages());

            return alerts;
        });
    }

    /**
     * GET /api/alerts/{id}
     *
     * Get specific alert by ID
     *
     * @param id Alert ID
     * @return Alert or 404 if not found
     */
    @GetMapping("/alerts/{id}")
    public Mono<ResponseEntity<EditWarAlert>> getAlertById(@PathVariable Long id) {
        log.info("GET /api/alerts/{}", id);

        return Mono.fromCallable(() -> alertService.getAlertById(id))
                .map(optional -> optional
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> {
                            log.warn("Alert not found: {}", id);
                            return ResponseEntity.notFound().build();
                        })
                );
    }

    /**
     * GET /api/alerts/search?q=trump
     *
     * Search alerts by page title (case-insensitive)
     *
     * @param query Search query
     * @return List of matching alerts
     */
    @GetMapping("/alerts/search")
    public Mono<ResponseEntity<List<EditWarAlert>>> searchAlerts(
            @RequestParam(required = true) String q
    ) {
        log.info("GET /api/alerts/search?q={}", q);

        if (q == null || q.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return Mono.fromCallable(() -> {
            List<EditWarAlert> results = alertService.searchByPageTitle(q);
            log.info("Search returned {} results for query: {}", results.size(), q);
            return ResponseEntity.ok(results);
        });
    }

    /**
     * GET /api/alerts/status/{status}
     *
     * Get alerts filtered by status
     *
     * @param status Alert status (ACTIVE, RESOLVED, etc.)
     * @return List of alerts with given status
     */
    @GetMapping("/alerts/status/{status}")
    public Mono<ResponseEntity<List<EditWarAlert>>> getAlertsByStatus(
            @PathVariable String status
    ) {
        log.info("GET /api/alerts/status/{}", status);

        return Mono.fromCallable(() -> {
            EditWarStatus warStatus = EditWarStatus.valueOf(status.toUpperCase());
            List<EditWarAlert> alerts = alertService.getAlertsByStatus(warStatus);
            return ResponseEntity.ok(alerts);
        }).onErrorResume(IllegalArgumentException.class, e -> {
            log.error("Invalid status: {}", status);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * GET /api/alerts/severity/{level}
     *
     * Get alerts filtered by severity level
     *
     * @param level Severity level (LOW, MEDIUM, HIGH, CRITICAL)
     * @return List of alerts with given severity
     */
    @GetMapping("/alerts/severity/{level}")
    public Mono<ResponseEntity<List<EditWarAlert>>> getAlertsBySeverity(
            @PathVariable String level
    ) {
        log.info("GET /api/alerts/severity/{}", level);

        return Mono.fromCallable(() -> {
            List<EditWarAlert> alerts = alertService.getAlertsBySeverity(level);
            return ResponseEntity.ok(alerts);
        }).onErrorResume(IllegalArgumentException.class, e -> {
            log.error("Invalid severity level: {}", level);
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * GET /api/alerts/wiki/{wiki}
     *
     * Get alerts for specific wiki
     *
     * @param wiki Wiki domain (e.g., en.wikipedia.org)
     * @return List of alerts for given wiki
     */
    @GetMapping("/alerts/wiki/{wiki}")
    public Mono<ResponseEntity<List<EditWarAlert>>> getAlertsByWiki(
            @PathVariable String wiki
    ) {
        log.info("GET /api/alerts/wiki/{}", wiki);

        return Mono.fromCallable(() -> {
            List<EditWarAlert> alerts = alertService.getAlertsByWiki(wiki);
            return ResponseEntity.ok(alerts);
        });
    }

    /**
     * GET /api/alerts/recent
     *
     * Get recent active alerts (ordered by detection time)
     *
     * @return List of recent active alerts
     */
    @GetMapping("/alerts/recent")
    public Mono<ResponseEntity<List<EditWarAlert>>> getRecentActiveAlerts() {
        log.info("GET /api/alerts/recent");

        return Mono.fromCallable(() -> {
            List<EditWarAlert> alerts = alertService.getRecentActiveAlerts();
            log.info("Returning {} recent alerts", alerts.size());
            return ResponseEntity.ok(alerts);
        });
    }

    /**
     * GET /api/stats
     *
     * Get alert statistics
     *
     * @return Statistics map
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStatistics() {
        log.info("GET /api/stats");

        return Mono.fromCallable(() -> {
            Map<String, Object> stats = alertService.getStatistics();
            return ResponseEntity.ok(stats);
        });
    }

    /**
     * GET /api/health
     *
     * Health check endpoint
     *
     * @return Health status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        log.debug("GET /api/health");

        return Mono.fromCallable(() -> {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "Edit War Detection API");
            health.put("totalAlerts", alertService.countAlerts());

            return ResponseEntity.ok(health);
        });
    }


}
