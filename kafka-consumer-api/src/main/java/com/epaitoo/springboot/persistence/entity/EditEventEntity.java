package com.epaitoo.springboot.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA Entity mapping to edit_events table
 * Stores individual edit events for analytics
 */
@Entity
@Table(
        name = "edit_events",
        indexes = {
                @Index(name = "idx_events_timestamp", columnList = "timestamp"),
                @Index(name = "idx_events_page_title", columnList = "page_title"),
                @Index(name = "idx_events_wiki", columnList = "wiki"),
                @Index(name = "idx_events_username", columnList = "username")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotBlank
    @Size(max = 500)
    @Column(name = "page_title", nullable = false, length = 500)
    private String pageTitle;

    @NotBlank
    @Size(max = 100)
    @Column(name = "wiki", nullable = false, length = 100)
    private String wiki;

    @NotNull
    @Column(name = "namespace", nullable = false)
    private Integer namespace;


    @NotBlank
    @Size(max = 255)
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @NotNull
    @Column(name = "is_bot", nullable = false)
    private Boolean isBot;


    @Column(name = "length_old")
    private Integer lengthOld;

    @Column(name = "length_new")
    private Integer lengthNew;

    @Column(name = "length_change")
    private Integer lengthChange;


    @NotBlank
    @Size(max = 50)
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Long timestamp;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
