package com.epaitoo.springboot.persistence.mapper;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.EditWarStatus;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class AlertMapperTest {

    private AlertMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AlertMapper();
    }

    @Test
    void testToEntity() {
        EditWarAlert alert = EditWarAlert.builder()
                .pageTitle("Test_Page")
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

        EditWarAlertEntity entity = mapper.toEntity(alert);

        assertNotNull(entity);
        assertEquals("Test_Page", entity.getPageTitle());
        assertEquals("en.wikipedia.org", entity.getWiki());
        assertEquals(new BigDecimal("0.75"), entity.getSeverityScore());
        assertEquals(5, entity.getTotalEdits());
        assertEquals(Arrays.asList("Alice", "Bob"), entity.getInvolvedUsers());
        assertEquals(EditWarAlertEntity.AlertStatus.ACTIVE, entity.getStatus());
    }

    @Test
    void testToDomain() {
        // Database entity
        EditWarAlertEntity entity = EditWarAlertEntity.builder()
                .pageTitle("Test_Page")
                .wiki("en.wikipedia.org")
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
                .severityLevel(EditWarAlertEntity.SeverityLevel.HIGH)
                .build();

        EditWarAlert alert = mapper.toDomain(entity);

        assertNotNull(alert);
        assertEquals("Test_Page", alert.getPageTitle());
        assertEquals("en.wikipedia.org", alert.getWiki());
        assertEquals(0.75, alert.getSeverityScore(), 0.001);
        assertEquals(5, alert.getTotalEdits());
        assertEquals(Arrays.asList("Alice", "Bob"), alert.getInvolvedUsers());
        assertEquals(EditWarStatus.ACTIVE, alert.getStatus());
    }

    @Test
    void testNullHandling() {
        // Test null input
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toDomain(null));
    }
}
