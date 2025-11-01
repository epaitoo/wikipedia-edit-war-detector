package com.epaitoo.springboot.persistence.mapper;

import com.epaitoo.springboot.entity.WikimediaEditEvent;
import com.epaitoo.springboot.persistence.entity.EditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {
    /**
     * Convert domain object to database entity
     *
     * @param event WikimediaEditEvent from Kafka
     * @return Database entity ready to be persisted
     */
    public EditEventEntity toEntity(WikimediaEditEvent event) {
        if (event == null) {
            return null;
        }

        return EditEventEntity.builder()
                // Page information
                .pageTitle(event.getPageTitle())
                .wiki(event.getWiki())
                .namespace(event.getNamespace())

                // User information
                .username(event.getUsername())
                .isBot(event.getIsBot())

                // Edit details
                .lengthOld(event.getLengthOld())
                .lengthNew(event.getLengthNew())
                .lengthChange(event.getLengthChange())

                // Metadata
                .eventType(event.getType())
                .timestamp(event.getTimestamp())

                .build();
    }

    /**
     * Convert database entity to domain object
     *
     * @param entity Database entity
     * @return WikimediaEditEvent
     */
    public WikimediaEditEvent toDomain(EditEventEntity entity) {
        if (entity == null) {
            return null;
        }

        WikimediaEditEvent event = new WikimediaEditEvent();

        // Page information
        event.setPageTitle(entity.getPageTitle());
        event.setWiki(entity.getWiki());
        event.setNamespace(entity.getNamespace());

        // User information
        event.setUsername(entity.getUsername());
        event.setIsBot(entity.getIsBot());

        // Edit details
        event.setLengthOld(entity.getLengthOld());
        event.setLengthNew(entity.getLengthNew());
        // lengthChange is computed in WikimediaEditEvent.getLengthChange()

        // Metadata
        event.setType(entity.getEventType());
        event.setTimestamp(entity.getTimestamp());

        return event;

    }
}
