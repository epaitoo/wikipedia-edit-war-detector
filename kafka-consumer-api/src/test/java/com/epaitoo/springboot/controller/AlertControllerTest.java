package com.epaitoo.springboot.controller;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.service.AlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * TDD Tests for AlertController
 *
 * Testing REST endpoints with WebFlux (reactive)
 * Using WebTestClient for integration testing
 */
@WebFluxTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AlertService alertService;

    private EditWarAlert createMockAlert(String pageTitle) {
        return EditWarAlert.builder()
                .pageTitle(pageTitle)
                .wiki("en.wikipedia.org")
                .severityScore(0.75)
                .totalEdits(5)
                .conflictEdits(4)
                .conflictRatio(0.8)
                .userCount(2)
                .involvedUsers(Arrays.asList("Alice", "Bob"))
                .firstEditTimestamp(1000L)
                .lastEditTimestamp(2000L)
                .detectedAt(Instant.now())
                .status(EditWarStatus.ACTIVE)
                .build();
    }

    // ==================== GET ALL ALERTS ====================

    @Test
    @DisplayName("GET /api/alerts - Should return paginated alerts")
    void testGetAllAlerts() {
        // Given: Mock service returns alerts
        List<EditWarAlert> alerts = Arrays.asList(
                createMockAlert("Page1"),
                createMockAlert("Page2")
        );
        Page<EditWarAlert> page = new PageImpl<>(alerts, PageRequest.of(0, 20), 2);

        when(alertService.getAllAlerts(any(Pageable.class))).thenReturn(page);

        // When & Then: Call API and verify
        webTestClient.get()
                .uri("/api/alerts?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].pageTitle").isEqualTo("Page1")
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(20)
                .jsonPath("$.number").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/alerts - Should use default pagination when not specified")
    void testGetAllAlerts_DefaultPagination() {
        // Given: Mock empty result
        Page<EditWarAlert> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(alertService.getAllAlerts(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then: Call without params
        webTestClient.get()
                .uri("/api/alerts")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(0)
                .jsonPath("$.totalElements").isEqualTo(0);
    }

    // ==================== GET ALERT BY ID ====================

    @Test
    @DisplayName("GET /api/alerts/{id} - Should return alert when found")
    void testGetAlertById_Found() {
        // Given: Alert exists
        EditWarAlert alert = createMockAlert("Test_Page");
        when(alertService.getAlertById(1L)).thenReturn(Optional.of(alert));

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pageTitle").isEqualTo("Test_Page")
                .jsonPath("$.wiki").isEqualTo("en.wikipedia.org")
                .jsonPath("$.totalEdits").isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/alerts/{id} - Should return 404 when not found")
    void testGetAlertById_NotFound() {
        // Given: Alert doesn't exist
        when(alertService.getAlertById(999L)).thenReturn(Optional.empty());

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ==================== SEARCH ALERTS ====================

    @Test
    @DisplayName("GET /api/alerts/search - Should search by page title")
    void testSearchAlerts() {
        // Given: Search results
        List<EditWarAlert> results = Arrays.asList(
                createMockAlert("Donald_Trump"),
                createMockAlert("Trump_Tower")
        );
        when(alertService.searchByPageTitle("trump")).thenReturn(results);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/search?q=trump")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].pageTitle").isEqualTo("Donald_Trump");
    }

    @Test
    @DisplayName("GET /api/alerts/search - Should return 400 when query missing")
    void testSearchAlerts_MissingQuery() {
        // When & Then: Call without query param
        webTestClient.get()
                .uri("/api/alerts/search")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ==================== GET BY STATUS ====================

    @Test
    @DisplayName("GET /api/alerts/status/{status} - Should filter by status")
    void testGetAlertsByStatus() {
        // Given: Active alerts
        List<EditWarAlert> alerts = Arrays.asList(
                createMockAlert("Page1"),
                createMockAlert("Page2")
        );
        when(alertService.getAlertsByStatus(EditWarStatus.ACTIVE)).thenReturn(alerts);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/status/ACTIVE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    // ==================== GET BY SEVERITY ====================

    @Test
    @DisplayName("GET /api/alerts/severity/{level} - Should filter by severity")
    void testGetAlertsBySeverity() {
        // Given: High severity alerts
        List<EditWarAlert> alerts = Arrays.asList(createMockAlert("Critical_Page"));
        when(alertService.getAlertsBySeverity("HIGH")).thenReturn(alerts);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/severity/HIGH")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    // ==================== GET BY WIKI ====================

    @Test
    @DisplayName("GET /api/alerts/wiki/{wiki} - Should filter by wiki")
    void testGetAlertsByWiki() {
        // Given: Alerts for specific wiki
        List<EditWarAlert> alerts = Arrays.asList(createMockAlert("Page1"));
        when(alertService.getAlertsByWiki("en.wikipedia.org")).thenReturn(alerts);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/wiki/en.wikipedia.org")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    // ==================== GET RECENT ALERTS ====================

    @Test
    @DisplayName("GET /api/alerts/recent - Should return recent active alerts")
    void testGetRecentActiveAlerts() {
        // Given: Recent alerts
        List<EditWarAlert> alerts = Arrays.asList(
                createMockAlert("Recent1"),
                createMockAlert("Recent2")
        );
        when(alertService.getRecentActiveAlerts()).thenReturn(alerts);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/alerts/recent")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    // ==================== GET STATISTICS ====================

    @Test
    @DisplayName("GET /api/stats - Should return statistics")
    void testGetStatistics() {
        // Given: Mock statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", 100L);
        stats.put("activeAlerts", 75L);
        stats.put("resolvedAlerts", 25L);
        when(alertService.getStatistics()).thenReturn(stats);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAlerts").isEqualTo(100)
                .jsonPath("$.activeAlerts").isEqualTo(75)
                .jsonPath("$.resolvedAlerts").isEqualTo(25);
    }

    // ==================== HEALTH CHECK ====================

    @Test
    @DisplayName("GET /api/health - Should return health status")
    void testHealthCheck() {
        // Given: Service is up
        when(alertService.countAlerts()).thenReturn(42L);

        // When & Then: Call API
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.totalAlerts").isEqualTo(42);
    }
}