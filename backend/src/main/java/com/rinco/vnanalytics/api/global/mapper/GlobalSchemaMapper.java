package com.rinco.vnanalytics.api.global.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GlobalSchemaMapper {

    private final JdbcTemplate jdbcTemplate;

    public GlobalSchemaMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureRankingExcludeColumns() {
        jdbcTemplate.execute("ALTER TABLE IF EXISTS vn_bangumi_subject ADD COLUMN IF NOT EXISTS excluded_from_rank BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE IF EXISTS vn_vndb_subject ADD COLUMN IF NOT EXISTS excluded_from_rank BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE IF EXISTS vn_egs_subject ADD COLUMN IF NOT EXISTS excluded_from_rank BOOLEAN NOT NULL DEFAULT FALSE");
    }

    /** Persisted weighted combined score; only triple-complete rows with all scores get a value. */
    public void ensureMatchTripleCombinedWeightedColumn() {
        jdbcTemplate.execute(
                "ALTER TABLE vn_match_triple ADD COLUMN IF NOT EXISTS combined_weighted_score DOUBLE PRECISION");
    }
}
