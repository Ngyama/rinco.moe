package com.rinco.vnanalytics.api.global.mapper;

import com.rinco.vnanalytics.api.global.model.BangumiGameItem;
import com.rinco.vnanalytics.api.global.model.GlobalSiteScoreItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Component
public class BangumiRankingMapper {

    private final JdbcTemplate jdbcTemplate;

    public BangumiRankingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureBangumiSubjectTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS vn_bangumi_subject (
                    id BIGSERIAL PRIMARY KEY,
                    bangumi_subject_id BIGINT NOT NULL UNIQUE,
                    title VARCHAR(255) NOT NULL,
                    score NUMERIC(4,2),
                    rank_value INT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        jdbcTemplate.execute(ddl);
        jdbcTemplate.execute("ALTER TABLE vn_bangumi_subject ADD COLUMN IF NOT EXISTS score_api NUMERIC(4,2)");
        jdbcTemplate.execute("ALTER TABLE vn_bangumi_subject ADD COLUMN IF NOT EXISTS score_exact NUMERIC(8,4)");
        jdbcTemplate.execute("ALTER TABLE vn_bangumi_subject ADD COLUMN IF NOT EXISTS rating_total INT");
        jdbcTemplate.execute("ALTER TABLE vn_bangumi_subject ADD COLUMN IF NOT EXISTS release_date DATE");
        jdbcTemplate.execute("ALTER TABLE vn_bangumi_subject ADD COLUMN IF NOT EXISTS excluded_from_rank BOOLEAN NOT NULL DEFAULT FALSE");
    }

    public int updateRankExcluded(long bangumiId, boolean excluded) {
        String sql = "UPDATE vn_bangumi_subject SET excluded_from_rank = ? WHERE bangumi_subject_id = ?";
        return jdbcTemplate.update(sql, excluded, bangumiId);
    }

    public Integer querySubjectCount(int minVotes, String exclusionRegex) {
        String countSql = """
                WITH top10_tags AS (
                    SELECT
                        t.bangumi_subject_id,
                        t.tag_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY t.bangumi_subject_id
                            ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC
                        ) AS rn
                    FROM vn_bangumi_subject_tag t
                ),
                qualified_subject AS (
                    SELECT DISTINCT bangumi_subject_id
                    FROM top10_tags
                    WHERE rn <= 10
                      AND lower(tag_name) = 'galgame'
                )
                SELECT COUNT(*)
                FROM vn_bangumi_subject s
                WHERE COALESCE(s.rating_total, 0) >= ?
                  AND s.bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(s.title_jp, ''), s.title, '') !~* ?
                """;
        return jdbcTemplate.queryForObject(countSql, Integer.class, minVotes, exclusionRegex);
    }

    public Integer querySubjectCountAll() {
        String countSql = "SELECT COUNT(*) FROM vn_bangumi_subject";
        return jdbcTemplate.queryForObject(countSql, Integer.class);
    }

    public List<BangumiGameItem> queryLocalList(int minVotes, int limit, int offset, String exclusionRegex) {
        String querySql = """
                WITH top10_tags AS (
                    SELECT
                        t.bangumi_subject_id,
                        t.tag_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY t.bangumi_subject_id
                            ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC
                        ) AS rn
                    FROM vn_bangumi_subject_tag t
                ),
                qualified_subject AS (
                    SELECT DISTINCT bangumi_subject_id
                    FROM top10_tags
                    WHERE rn <= 10
                      AND lower(tag_name) = 'galgame'
                )
                SELECT
                    bangumi_subject_id,
                    title,
                    COALESCE(score_exact, score_api, score) AS display_score,
                    rating_total,
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(score_exact, score_api, score) DESC NULLS LAST,
                                 rating_total DESC NULLS LAST,
                                 bangumi_subject_id ASC
                    ) AS leaderboard_rank
                FROM vn_bangumi_subject
                WHERE bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND COALESCE(rating_total, 0) >= ?
                  AND COALESCE(excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(title_jp, ''), title, '') !~* ?
                ORDER BY COALESCE(score_exact, score_api, score) DESC NULLS LAST,
                         rating_total DESC NULLS LAST,
                         bangumi_subject_id ASC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(querySql, (rs, rowNum) -> new BangumiGameItem(
                rs.getLong("bangumi_subject_id"),
                rs.getString("title"),
                rs.getString("title"),
                "",
                rs.getInt("leaderboard_rank"),
                rs.getBigDecimal("display_score") == null ? null : rs.getBigDecimal("display_score").doubleValue(),
                (Integer) rs.getObject("rating_total"),
                "https://bgm.tv/subject/" + rs.getLong("bangumi_subject_id")
        ), minVotes, exclusionRegex, limit, offset);
    }

    public List<GlobalSiteScoreItem> queryTop(int minVotes, int limit, String exclusionRegex) {
        String sql = """
                WITH top10_tags AS (
                    SELECT
                        t.bangumi_subject_id,
                        t.tag_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY t.bangumi_subject_id
                            ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC
                        ) AS rn
                    FROM vn_bangumi_subject_tag t
                ),
                qualified_subject AS (
                    SELECT DISTINCT bangumi_subject_id
                    FROM top10_tags
                    WHERE rn <= 10
                      AND lower(tag_name) = 'galgame'
                )
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                                 s.rating_total DESC NULLS LAST,
                                 s.bangumi_subject_id ASC
                    ) AS rank_value,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(s.score_exact, s.score_api, s.score) AS score_value,
                    m.id AS match_triple_id
                FROM vn_bangumi_subject s
                LEFT JOIN vn_match_triple m ON m.bangumi_subject_id = s.bangumi_subject_id
                WHERE COALESCE(s.rating_total, 0) > ?
                  AND s.bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND COALESCE(s.score_exact, s.score_api, s.score) IS NOT NULL
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(s.title_jp, ''), s.title, '') !~* ?
                ORDER BY COALESCE(s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                         s.rating_total DESC NULLS LAST,
                         s.bangumi_subject_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GlobalSiteScoreItem(
                rs.getInt("rank_value"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score_value").doubleValue(),
                readMatchTripleId(rs)
        ), minVotes, exclusionRegex, limit);
    }

    /**
     * Curated match pool: rows in vn_match_triple that have Bangumi, ordered by score (denormalized or subject).
     */
    public List<GlobalSiteScoreItem> queryCuratedPoolBangumi(int limit) {
        String sql = """
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(m.bangumi_score, s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                                 COALESCE(s.rating_total, 0) DESC NULLS LAST,
                                 m.bangumi_subject_id ASC
                    ) AS rank_value,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(m.bangumi_score, s.score_exact, s.score_api, s.score) AS score_value,
                    m.id AS match_triple_id
                FROM vn_match_triple m
                INNER JOIN vn_bangumi_subject s ON s.bangumi_subject_id = m.bangumi_subject_id
                WHERE m.bangumi_subject_id IS NOT NULL
                  AND COALESCE(m.bangumi_score, s.score_exact, s.score_api, s.score) IS NOT NULL
                ORDER BY COALESCE(m.bangumi_score, s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                         COALESCE(s.rating_total, 0) DESC NULLS LAST,
                         m.bangumi_subject_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GlobalSiteScoreItem(
                rs.getInt("rank_value"),
                rs.getString("title_jp"),
                readNullableDouble(rs, "score_value"),
                readMatchTripleId(rs)
        ), limit);
    }

    public void persistSubjects(List<BangumiGameItem> items) {
        if (items.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO vn_bangumi_subject (bangumi_subject_id, title, score, rank_value, release_date)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (bangumi_subject_id) DO UPDATE
                SET title = EXCLUDED.title,
                    score = EXCLUDED.score,
                    rank_value = EXCLUDED.rank_value,
                    release_date = COALESCE(EXCLUDED.release_date, vn_bangumi_subject.release_date)
                """;

        jdbcTemplate.batchUpdate(sql, items, items.size(), (ps, item) -> {
            ps.setLong(1, item.id());
            ps.setString(2, normalizeTitle(item));
            if (item.score() == null) {
                ps.setNull(3, java.sql.Types.NUMERIC);
            } else {
                ps.setDouble(3, item.score());
            }
            if (item.rank() == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, item.rank());
            }
            LocalDate releaseDate = parseReleaseDate(item.date());
            if (releaseDate == null) {
                ps.setNull(5, java.sql.Types.DATE);
            } else {
                ps.setObject(5, releaseDate);
            }
        });
    }

    public List<Object[]> queryTrendYearAverages(int minVotes, String exclusionRegex) {
        String sql = """
                WITH top10_tags AS (
                    SELECT
                        t.bangumi_subject_id,
                        t.tag_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY t.bangumi_subject_id
                            ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC
                        ) AS rn
                    FROM vn_bangumi_subject_tag t
                ),
                qualified_subject AS (
                    SELECT DISTINCT bangumi_subject_id
                    FROM top10_tags
                    WHERE rn <= 10
                      AND lower(tag_name) = 'galgame'
                ),
                filtered AS (
                    SELECT
                        EXTRACT(YEAR FROM release_date)::INT AS year_value,
                        COALESCE(score_exact, score_api, score) AS score_value
                    FROM vn_bangumi_subject
                    WHERE bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                      AND release_date IS NOT NULL
                      AND COALESCE(rating_total, 0) > ?
                      AND COALESCE(excluded_from_rank, FALSE) = FALSE
                      AND COALESCE(NULLIF(title_jp, ''), title, '') !~* ?
                      AND COALESCE(score_exact, score_api, score) IS NOT NULL
                )
                SELECT
                    year_value,
                    AVG(score_value) AS avg_score,
                    COUNT(*) AS item_count
                FROM filtered
                GROUP BY year_value
                ORDER BY year_value ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getInt("year_value"),
                rs.getBigDecimal("avg_score") == null ? null : rs.getBigDecimal("avg_score").doubleValue(),
                rs.getInt("item_count")
        }, minVotes, exclusionRegex);
    }

    public List<Object[]> queryTrendPoints(int minVotes, String exclusionRegex) {
        String sql = """
                WITH top10_tags AS (
                    SELECT
                        t.bangumi_subject_id,
                        t.tag_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY t.bangumi_subject_id
                            ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC
                        ) AS rn
                    FROM vn_bangumi_subject_tag t
                ),
                qualified_subject AS (
                    SELECT DISTINCT bangumi_subject_id
                    FROM top10_tags
                    WHERE rn <= 10
                      AND lower(tag_name) = 'galgame'
                )
                SELECT
                    EXTRACT(YEAR FROM release_date)::INT AS year_value,
                    release_date,
                    COALESCE(NULLIF(title_jp, ''), title) AS title_jp,
                    COALESCE(score_exact, score_api, score) AS score_value
                FROM vn_bangumi_subject
                WHERE bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND release_date IS NOT NULL
                  AND COALESCE(rating_total, 0) > ?
                  AND COALESCE(excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(title_jp, ''), title, '') !~* ?
                  AND COALESCE(score_exact, score_api, score) IS NOT NULL
                ORDER BY year_value ASC, score_value DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getInt("year_value"),
                rs.getObject("release_date") == null ? null : rs.getObject("release_date").toString(),
                rs.getString("title_jp"),
                rs.getBigDecimal("score_value") == null ? null : rs.getBigDecimal("score_value").doubleValue()
        }, minVotes, exclusionRegex);
    }

    private String normalizeTitle(BangumiGameItem item) {
        if (item.nameCn() != null && !item.nameCn().isBlank()) {
            return item.nameCn();
        }
        return item.name();
    }

    private static Long readMatchTripleId(ResultSet rs) throws SQLException {
        long v = rs.getLong("match_triple_id");
        return rs.wasNull() ? null : v;
    }

    private static Double readNullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private LocalDate parseReleaseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() != 10) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
