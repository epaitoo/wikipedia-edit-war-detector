package com.epaitoo.springboot.persistence.repository;

import com.epaitoo.springboot.persistence.entity.EditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EditEventRepository extends JpaRepository<EditEventEntity, Long> {
    List<EditEventEntity> findByPageTitle(String pageTitle);
    List<EditEventEntity> findByUsername(String username);
    List<EditEventEntity> findByWiki(String wiki);
    List<EditEventEntity> findByIsBot(boolean isBot);
    List<EditEventEntity> findByNamespace(Integer namespace);
    Page<EditEventEntity> findByPageTitle(String pageTitle, Pageable pageable);
    List<EditEventEntity> findByTimestampAfter(Long timestamp);
    List<EditEventEntity> findByTimestampBetween(Long start, Long end);
    List<EditEventEntity> findTop100ByOrderByTimestampDesc();

//    Combined Queries
    List<EditEventEntity> findByIsBotAndNamespace(Boolean isBot, Integer namespace);
    List<EditEventEntity> findByPageTitleAndWiki(String pageTitle, String wiki);

//    Custom Queries
    /**
     * Get edit activity for a page
     */
    @Query("SELECT e FROM EditEventEntity e " +
            "WHERE e.pageTitle = :pageTitle " +
            "AND e.wiki = :wiki " +
            "AND e.timestamp >= :since " +
            "ORDER BY e.timestamp DESC")
    List<EditEventEntity> getPageActivity(
            @Param("pageTitle") String pageTitle,
            @Param("wiki") String wiki,
            @Param("since") Long since
    );

    /**
     * Get most active users
     */
    @Query("SELECT e.username, COUNT(e) as editCount " +
            "FROM EditEventEntity e " +
            "WHERE e.wiki = :wiki " +
            "AND e.isBot = false " +
            "AND e.timestamp >= :since " +
            "GROUP BY e.username " +
            "ORDER BY editCount DESC")
    List<Object[]> getMostActiveUsers(
            @Param("wiki") String wiki,
            @Param("since") Long since,
            Pageable pageable
    );

    /**
     * Get most edited pages
     */
    @Query("SELECT e.pageTitle, COUNT(e) as editCount " +
            "FROM EditEventEntity e " +
            "WHERE e.wiki = :wiki " +
            "AND e.namespace = 0 " +
            "AND e.timestamp >= :since " +
            "GROUP BY e.pageTitle " +
            "ORDER BY editCount DESC")
    List<Object[]> getMostEditedPages(
            @Param("wiki") String wiki,
            @Param("since") Long since,
            Pageable pageable
    );

    /**
     * Count edits by user on specific page
     */
    @Query("SELECT COUNT(e) FROM EditEventEntity e " +
            "WHERE e.pageTitle = :pageTitle " +
            "AND e.username = :username " +
            "AND e.timestamp >= :since")
    Long countUserEditsOnPage(
            @Param("pageTitle") String pageTitle,
            @Param("username") String username,
            @Param("since") Long since
    );

//    Aggregate Queries
    /**
     * Count events by page
     */
    long countByPageTitle(String pageTitle);

    /**
     * Count human edits
     */
    long countByIsBot(Boolean isBot);

    /**
     * Delete old events (cleanup - optional)
     */
    long deleteByTimestampBefore(Long timestamp);
}
