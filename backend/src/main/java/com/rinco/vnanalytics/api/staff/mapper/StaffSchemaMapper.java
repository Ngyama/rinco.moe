package com.rinco.vnanalytics.api.staff.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Staff-related DDL. Score metrics for charts (e.g. dumbbell: min vs max Bangumi score among works)
 * should come from {@code vn_bangumi_subject}; this module only links persons to subjects.
 * <p>
 * Maintenance: when a sync job adds/updates subjects and staff rows, upsert into
 * {@code vn_bangumi_subject_person}. Aggregates can be computed in SQL (GROUP BY person) or
 * optionally materialized later if needed.
 */
@Component
public class StaffSchemaMapper {

    private final JdbcTemplate jdbcTemplate;

    public StaffSchemaMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Matches {@link StaffMapper} / import scripts; safe if scripts already created the table.
     */
    public void ensureBangumiPersonTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vn_bangumi_person (
                    bangumi_person_id BIGINT PRIMARY KEY,
                    name VARCHAR(512) NOT NULL DEFAULT '',
                    name_cn VARCHAR(512),
                    name_jp VARCHAR(512),
                    image_url VARCHAR(1024),
                    relation VARCHAR(255)
                )
                """);
    }

    /**
     * Person participation on a subject (e.g. 剧本 / 原画). Scores are not stored here —
     * join {@code vn_bangumi_subject} for {@code score_exact} / {@code score_api} / {@code score}.
     * PK includes {@code relation} so one person can have multiple roles on the same game.
     */
    public void ensureBangumiSubjectPersonTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vn_bangumi_subject_person (
                    bangumi_subject_id BIGINT NOT NULL,
                    bangumi_person_id BIGINT NOT NULL,
                    relation VARCHAR(128) NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (bangumi_subject_id, bangumi_person_id, relation)
                )
                """);
        // Legacy DBs may have UNIQUE(subject, person) only; Bangumi allows multiple roles per person.
        jdbcTemplate.execute("""
                ALTER TABLE vn_bangumi_subject_person DROP CONSTRAINT IF EXISTS uk_vn_bangumi_subject_person
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_bangumi_subject_person_person
                ON vn_bangumi_subject_person (bangumi_person_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_bangumi_subject_person_subject
                ON vn_bangumi_subject_person (bangumi_subject_id)
                """);
    }

    public void ensureAll() {
        ensureBangumiPersonTable();
        ensureBangumiSubjectPersonTable();
    }
}
