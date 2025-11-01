package com.epaitoo.springboot.service;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.mapper.AlertMapper;
import com.epaitoo.springboot.persistence.repository.EditWarAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for AlertService
 *
 * Testing:
 * - Get all alerts (with pagination)
 * - Get alert by ID
 * - Search alerts
 * - Get statistics
 * - Update alert status
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private EditWarAlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private AlertService alertService;

    private EditWarAlertEntity createMockEntity(Long id, String pageTitle, String severity) {
        return EditWarAlertEntity.builder()
                .Id(id)
                .pageTitle(pageTitle)
                .wiki("en.wikipedia.org")
                .severityLevel(EditWarAlertEntity.SeverityLevel.valueOf(severity))
                .severityScore(new BigDecimal("0.75"))
                .totalEdits(5)
                .conflictEdits(4)
                .conflictRatio(new BigDecimal("0.80"))
                .userCount(2)
                .involvedUsers(Arrays.asList("Alice", "Bob"))
                .firstEditTimestamp(1000L)
                .lastEditTimestamp(2000L)
                .detectedAt(Instant.now())
                .status(EditWarAlertEntity.AlertStatus.ACTIVE)
                .build();
    }

    private EditWarAlert createMockDomainAlert(Long id, String pageTitle) {
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

    // ==================== GET ALL ALERTS (PAGINATED) ====================

    @Test
    @DisplayName("Should get all alerts with pagination")
    void testGetAllAlerts_WithPagination() {
        // Given: Mock data
        List<EditWarAlertEntity> entities = Arrays.asList(
                createMockEntity(1L, "Page1", "HIGH"),
                createMockEntity(2L, "Page2", "MEDIUM")
        );
        Page<EditWarAlertEntity> entityPage = new PageImpl<>(entities);

        when(alertRepository.findAll(any(Pageable.class))).thenReturn(entityPage);
        when(alertMapper.toDomain(any(EditWarAlertEntity.class)))
                .thenAnswer(inv -> createMockDomainAlert(1L, "Page1"))
                .thenAnswer(inv -> createMockDomainAlert(2L, "Page2"));

        // When: Get alerts
        Pageable pageable = PageRequest.of(0, 20);
        Page<EditWarAlert> result = alertService.getAllAlerts(pageable);

        // Then: Verify
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(alertRepository).findAll(pageable);
        verify(alertMapper, times(2)).toDomain(any(EditWarAlertEntity.class));
    }

    // ==================== GET ALERT BY ID ====================

    @Test
    @DisplayName("Should get alert by ID when exists")
    void testGetAlertById_WhenExists() {
        // Given: Mock entity exists
        EditWarAlertEntity entity = createMockEntity(1L, "Test_Page", "HIGH");
        EditWarAlert domainAlert = createMockDomainAlert(1L, "Test_Page");

        when(alertRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(alertMapper.toDomain(entity)).thenReturn(domainAlert);

        // When: Get by ID
        Optional<EditWarAlert> result = alertService.getAlertById(1L);

        // Then: Verify
        assertTrue(result.isPresent());
        assertEquals("Test_Page", result.get().getPageTitle());
        verify(alertRepository).findById(1L);
        verify(alertMapper).toDomain(entity);
    }

    @Test
    @DisplayName("Should return empty when alert ID not found")
    void testGetAlertById_WhenNotExists() {
        // Given: ID doesn't exist
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // When: Get by ID
        Optional<EditWarAlert> result = alertService.getAlertById(999L);

        // Then: Verify
        assertFalse(result.isPresent());
        verify(alertRepository).findById(999L);
        verify(alertMapper, never()).toDomain(any());
    }

    // ==================== GET ALERTS BY STATUS ====================

    @Test
    @DisplayName("Should get alerts by status")
    void testGetAlertsByStatus() {
        // Given: Mock active alerts
        List<EditWarAlertEntity> entities = Arrays.asList(
                createMockEntity(1L, "Page1", "HIGH"),
                createMockEntity(2L, "Page2", "CRITICAL")
        );

        when(alertRepository.findByStatus(EditWarAlertEntity.AlertStatus.ACTIVE))
                .thenReturn(entities);
        when(alertMapper.toDomain(any(EditWarAlertEntity.class)))
                .thenAnswer(inv -> createMockDomainAlert(1L, "Page1"))
                .thenAnswer(inv -> createMockDomainAlert(2L, "Page2"));

        // When: Get by status
        List<EditWarAlert> result = alertService.getAlertsByStatus(EditWarStatus.ACTIVE);

        // Then: Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(alertRepository).findByStatus(EditWarAlertEntity.AlertStatus.ACTIVE);
    }

    // ==================== SEARCH ALERTS ====================

    @Test
    @DisplayName("Should search alerts by page title")
    void testSearchAlertsByPageTitle() {
        // Given: Mock search results
        List<EditWarAlertEntity> entities = Collections.singletonList(
                createMockEntity(1L, "Donald_Trump", "HIGH")
        );

        when(alertRepository.findByPageTitleContainingIgnoreCase("trump"))
                .thenReturn(entities);
        when(alertMapper.toDomain(any(EditWarAlertEntity.class)))
                .thenReturn(createMockDomainAlert(1L, "Donald_Trump"));

        // When: Search
        List<EditWarAlert> result = alertService.searchByPageTitle("trump");

        // Then: Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Donald_Trump", result.getFirst().getPageTitle());
        verify(alertRepository).findByPageTitleContainingIgnoreCase("trump");
    }

    // ==================== GET ALERTS BY SEVERITY ====================

    @Test
    @DisplayName("Should get alerts by severity level")
    void testGetAlertsBySeverity() {
        // Given: Mock high severity alerts
        List<EditWarAlertEntity> entities = Arrays.asList(
                createMockEntity(1L, "Page1", "HIGH"),
                createMockEntity(2L, "Page2", "HIGH")
        );

        when(alertRepository.findBySeverityLevel(EditWarAlertEntity.SeverityLevel.HIGH))
                .thenReturn(entities);
        when(alertMapper.toDomain(any(EditWarAlertEntity.class)))
                .thenAnswer(inv -> createMockDomainAlert(1L, "Page1"))
                .thenAnswer(inv -> createMockDomainAlert(2L, "Page2"));

        // When: Get by severity
        List<EditWarAlert> result = alertService.getAlertsBySeverity("HIGH");

        // Then: Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(alertRepository).findBySeverityLevel(EditWarAlertEntity.SeverityLevel.HIGH);
    }

    // ==================== GET STATISTICS ====================

    @Test
    @DisplayName("Should get alert statistics")
    void testGetStatistics() {
        // Given: Mock counts
        when(alertRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatus(EditWarAlertEntity.AlertStatus.ACTIVE))
                .thenReturn(75L);
        when(alertRepository.countByStatus(EditWarAlertEntity.AlertStatus.RESOLVED))
                .thenReturn(25L);

        // When: Get stats
        var stats = alertService.getStatistics();

        // Then: Verify
        assertNotNull(stats);
        assertEquals(100L, stats.get("totalAlerts"));
        assertEquals(75L, stats.get("activeAlerts"));
        assertEquals(25L, stats.get("resolvedAlerts"));

        verify(alertRepository).count();
        verify(alertRepository, times(2)).countByStatus(any());
    }

    // ==================== GET RECENT ALERTS ====================

    @Test
    @DisplayName("Should get recent active alerts")
    void testGetRecentActiveAlerts() {
        // Given: Mock recent alerts
        List<EditWarAlertEntity> entities = Arrays.asList(
                createMockEntity(1L, "Recent1", "HIGH"),
                createMockEntity(2L, "Recent2", "MEDIUM")
        );

        when(alertRepository.findByStatusOrderByDetectedAtDesc(EditWarAlertEntity.AlertStatus.ACTIVE))
                .thenReturn(entities);
        when(alertMapper.toDomain(any(EditWarAlertEntity.class)))
                .thenAnswer(inv -> createMockDomainAlert(1L, "Recent1"))
                .thenAnswer(inv -> createMockDomainAlert(2L, "Recent2"));

        // When: Get recent
        List<EditWarAlert> result = alertService.getRecentActiveAlerts();

        // Then: Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(alertRepository).findByStatusOrderByDetectedAtDesc(EditWarAlertEntity.AlertStatus.ACTIVE);
    }

    // ==================== COUNT ALERTS ====================

    @Test
    @DisplayName("Should count total alerts")
    void testCountAlerts() {
        // Given: Mock count
        when(alertRepository.count()).thenReturn(42L);

        // When: Count
        long count = alertService.countAlerts();

        // Then: Verify
        assertEquals(42L, count);
        verify(alertRepository).count();
    }
}