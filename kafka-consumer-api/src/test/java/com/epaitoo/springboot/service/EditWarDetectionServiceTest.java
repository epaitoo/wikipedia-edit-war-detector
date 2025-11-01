package com.epaitoo.springboot.service;

import com.epaitoo.springboot.entity.EditWarAlert;
import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.mapper.AlertMapper;
import com.epaitoo.springboot.persistence.repository.EditWarAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test for EditWarDetectionService
 *
 * Tests that detection service:
 * 1. Processes edits correctly
 * 2. Saves alerts to database when detected
 * 3. Uses mapper to convert domain → entity
 */
@ExtendWith(MockitoExtension.class)
class EditWarDetectionServiceTest {

    @Mock
    private EditWarAlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private EditWarDetectionService detectionService;

    private WikimediaEditEvent createTestEdit(String user, int oldLen, int newLen, long timestamp) {
        WikimediaEditEvent event = new WikimediaEditEvent();
        event.setPageTitle("Test_Article");
        event.setUsername(user);
        event.setLengthOld(oldLen);
        event.setLengthNew(newLen);
        event.setTimestamp(timestamp);
        event.setIsBot(false);
        event.setNamespace(0);  // Main namespace
        event.setType("edit");
        event.setWiki("en.wikipedia.org");
        return event;
    }

    @Test
    @DisplayName("Should save alert to database when edit war is detected")
    void testProcessEdit_SavesAlertToDatabase() {
        // Given: Simulate edit war scenario (5 reverting edits)
        long now = System.currentTimeMillis() / 1000;

        // Create reverting pattern that triggers detection
        detectionService.processEdit(createTestEdit("Alice", 5000, 5200, now));
        detectionService.processEdit(createTestEdit("Bob", 5200, 5000, now + 180));
        detectionService.processEdit(createTestEdit("Alice", 5000, 5200, now + 360));
        detectionService.processEdit(createTestEdit("Bob", 5200, 5000, now + 540));

        // Mock mapper behavior
        EditWarAlertEntity mockEntity = new EditWarAlertEntity();
        when(alertMapper.toEntity(any(EditWarAlert.class))).thenReturn(mockEntity);

        // Mock repository save
        when(alertRepository.save(any(EditWarAlertEntity.class))).thenReturn(mockEntity);

        // When: Process 5th edit (should trigger alert)
        Optional<EditWarAlert> result = detectionService.processEdit(
                createTestEdit("Alice", 5000, 5200, now + 720)
        );

        // Then: Alert should be detected
        assertTrue(result.isPresent(), "Alert should be detected");

        // Verify mapper was called
        verify(alertMapper).toEntity(any(EditWarAlert.class));

        // Verify repository save was called
        verify(alertRepository).save(any(EditWarAlertEntity.class));
    }

    @Test
    @DisplayName("Should NOT save to database when no edit war is detected")
    void testProcessEdit_DoesNotSaveWhenNoWarDetected() {
        // Given: Normal collaborative edits (not a war)
        long now = System.currentTimeMillis() / 1000;

        detectionService.processEdit(createTestEdit("Alice", 5000, 5100, now));
        detectionService.processEdit(createTestEdit("Bob", 5100, 5200, now + 180));
        Optional<EditWarAlert> result = detectionService.processEdit(
                createTestEdit("Charlie", 5200, 5300, now + 360)
        );

        // Then: No alert should be detected
        assertFalse(result.isPresent(), "No alert should be detected");

        // Verify repository save was NEVER called
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should pass correct data to mapper and repository")
    void testProcessEdit_PassesCorrectDataToMapperAndRepository() {
        // Given: Edit war scenario
        long now = System.currentTimeMillis() / 1000;

        detectionService.processEdit(createTestEdit("Alice", 5000, 5200, now));
        detectionService.processEdit(createTestEdit("Bob", 5200, 5000, now + 180));
        detectionService.processEdit(createTestEdit("Alice", 5000, 5200, now + 360));
        detectionService.processEdit(createTestEdit("Bob", 5200, 5000, now + 540));

        // Mock mapper and repository
        EditWarAlertEntity mockEntity = new EditWarAlertEntity();
        when(alertMapper.toEntity(any(EditWarAlert.class))).thenReturn(mockEntity);
        when(alertRepository.save(any(EditWarAlertEntity.class))).thenReturn(mockEntity);

        // When: Trigger alert
        detectionService.processEdit(createTestEdit("Alice", 5000, 5200, now + 720));

        // Then: Capture what was passed to mapper
        ArgumentCaptor<EditWarAlert> alertCaptor = ArgumentCaptor.forClass(EditWarAlert.class);
        verify(alertMapper).toEntity(alertCaptor.capture());

        EditWarAlert capturedAlert = alertCaptor.getValue();
        assertNotNull(capturedAlert);
        assertEquals("Test_Article", capturedAlert.getPageTitle());
        assertEquals("en.wikipedia.org", capturedAlert.getWiki());
        assertEquals(5, capturedAlert.getTotalEdits());
        assertTrue(capturedAlert.getUserCount() >= 2);

        // Verify repository received the entity
        verify(alertRepository).save(mockEntity);
    }
}