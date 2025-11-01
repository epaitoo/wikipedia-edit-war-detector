package com.epaitoo.springboot.persistence.repository;

import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity.AlertStatus;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity.SeverityLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class EditWarAlertRepositoryTest {

    @Autowired
    private EditWarAlertRepository repository;

    @Test
    void testSaveAndFind() {
        // Create test alert
        EditWarAlertEntity alert = EditWarAlertEntity.builder()
                .pageTitle("Test_Article")
                .wiki("en.wikipedia.org")
                .severityLevel(SeverityLevel.HIGH)
                .severityScore(new BigDecimal("0.75"))
                .totalEdits(5)
                .conflictEdits(4)
                .conflictRatio(new BigDecimal("0.80"))
                .userCount(2)
                .involvedUsers(Arrays.asList("Alice", "Bob"))
                .firstEditTimestamp(System.currentTimeMillis() / 1000)
                .lastEditTimestamp(System.currentTimeMillis() / 1000)
                .detectedAt(Instant.now())
                .status(AlertStatus.ACTIVE)
                .build();

        // Save
        EditWarAlertEntity saved = repository.save(alert);
        assertNotNull(saved.getId());
        System.out.println("✅ Saved with ID: " + saved.getId());

        // Find
        EditWarAlertEntity found = repository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Test_Article", found.getPageTitle());
        assertEquals(SeverityLevel.HIGH, found.getSeverityLevel());
        assertEquals(new BigDecimal("0.75"), found.getSeverityScore());
        System.out.println("✅ Found: " + found.getPageTitle());
    }
}