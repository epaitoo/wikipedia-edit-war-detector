package com.epaitoo.springboot.persistence.repository;

import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity.SeverityLevel;
import com.epaitoo.springboot.persistence.entity.EditWarAlertEntity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EditWarAlertRepository extends JpaRepository<EditWarAlertEntity, Long> {
    List<EditWarAlertEntity> findByPageTitle(String pageTitle);
    List<EditWarAlertEntity> findByWiki(String wiki);
    List<EditWarAlertEntity> findBySeverityLevel(SeverityLevel severityLevel);
    List<EditWarAlertEntity> findByStatus(AlertStatus status);

    Optional<EditWarAlertEntity> findByPageTitleAndWiki(String pageTitle, String wiki);
    List<EditWarAlertEntity> findBySeverityLevelAndStatus(SeverityLevel severityLevel, AlertStatus status);
    List<EditWarAlertEntity> findByStatusOrderByDetectedAtDesc(AlertStatus status);
    List<EditWarAlertEntity> findByDetectedAtAfter(Instant afterTime);
    List<EditWarAlertEntity> findByDetectedAtBetween(Instant start, Instant end);
    List<EditWarAlertEntity> findByPageTitleContainingIgnoreCase(String keyword);

//    Page Queries
    Page<EditWarAlertEntity> findByStatus(AlertStatus status, Pageable pageable);
    Page<EditWarAlertEntity> findBySeverityLevel(SeverityLevel severityLevel, Pageable pageable);
    Page<EditWarAlertEntity> findByWiki(String wiki, Pageable pageable);

//    Custom Queries
    @Query(
            "SELECT a FROM EditWarAlertEntity a " +
            "WHERE a.wiki = :wiki " +
            "AND a.status = :status " +
            "AND a.detectedAt >= :since " +
            "ORDER BY a.detectedAt DESC"
    )
    List<EditWarAlertEntity> findRecentAlertsByWikiAndStatus(
            @Param("wiki") String wiki,
            @Param("status") AlertStatus status,
            @Param("since") Instant since
    );

    /**
     * Find top N the highest severity alerts
     */
    @Query("SELECT a FROM EditWarAlertEntity a " +
            "WHERE a.status = :status " +
            "ORDER BY a.severityScore DESC, a.detectedAt DESC")
    List<EditWarAlertEntity> findTopBySeverity(
            @Param("status") AlertStatus status,
            Pageable pageable
    );

    /**
     * Count alerts by severity level
     */
    @Query("SELECT COUNT(a) FROM EditWarAlertEntity a " +
            "WHERE a.severityLevel = :level " +
            "AND a.status = :status")
    Long countBySeverityLevelAndStatus(
            @Param("level") SeverityLevel severityLevel,
            @Param("status") AlertStatus status
    );

    /**
     * Get statistics: count by severity level
     * Returns: List of Object[] where [0] = severityLevel, [1] = count
     */
    @Query("SELECT a.severityLevel, COUNT(a) " +
            "FROM EditWarAlertEntity a " +
            "WHERE a.status = :status " +
            "GROUP BY a.severityLevel " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> getAlertCountsBySeverity(@Param("status") AlertStatus status);

    /**
     * Get most active pages (pages with most alerts)
     */
    @Query("SELECT a.pageTitle, COUNT(a) as alertCount " +
            "FROM EditWarAlertEntity a " +
            "WHERE a.wiki = :wiki " +
            "AND a.detectedAt >= :since " +
            "GROUP BY a.pageTitle " +
            "ORDER BY alertCount DESC")
    List<Object[]> getMostActivePages(
            @Param("wiki") String wiki,
            @Param("since") Instant since,
            Pageable pageable
    );

//    Native Sql Queries
    @Query(
            value = "SELECT * FROM edit_war_alerts " +
                    "WHERE wiki = :wiki " +
                    "AND severity_score >= :minScore " +
                    "AND status = :status " +
                    "ORDER BY detected_at DESC " +
                    "LIMIT :limit",
            nativeQuery = true
    )
    List<EditWarAlertEntity> findHighSeverityAlertsNative(
            @Param("wiki") String wiki,
            @Param("minScore") Double minScore,
            @Param("status") String status,
            @Param("limit") int limit
    );

//    Aggregate Queries
    long countByStatus(AlertStatus status);

    /**
     * Check if alert exists for page
     */
    boolean existsByPageTitleAndStatus(String pageTitle, AlertStatus status);

    /**
     * Delete old resolved alerts (cleanup)
     */
    long deleteByStatusAndDetectedAtBefore(AlertStatus status, Instant before);

}
