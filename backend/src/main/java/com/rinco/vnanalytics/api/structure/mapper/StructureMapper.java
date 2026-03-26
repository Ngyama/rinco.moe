package com.rinco.vnanalytics.api.structure.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructureMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final String QUALIFIED_CTE = """
            WITH top10_tags AS (
                SELECT t.bangumi_subject_id, t.tag_name,
                    ROW_NUMBER() OVER (PARTITION BY t.bangumi_subject_id ORDER BY t.tag_count DESC NULLS LAST, t.tag_name ASC) AS rn
                FROM vn_bangumi_subject_tag t
            ),
            qualified_subject AS (
                SELECT DISTINCT bangumi_subject_id FROM top10_tags
                WHERE rn <= 10 AND lower(tag_name) = 'galgame'
            )
            """;

    private static final String EXCLUSION_REGEX =
            "(HD版|HD\\s*Remaster|Premium\\s*Edition|Complete\\s*Edition|Latest\\s*Edition|DVD版|BD版|Blu-?ray版|"
                    + "全年齢版|新装版|廉価版|移植版|Portable版|Console版|International\\s*Edition|Definitive\\s*Edition|"
                    + "Special\\s*Edition|Deluxe\\s*Edition|Ultimate\\s*Edition|Perfect\\s*Edition|"
                    + "Director'?s\\s*Cut|Full\\s*Voice|フールボイス|フルボイス)";

    public StructureMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Object[]> queryHotGames(int year, int limit) {
        String sql = QUALIFIED_CTE + """
                SELECT
                    s.bangumi_subject_id,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(s.score_exact, s.score_api, s.score) AS score,
                    s.rating_total,
                    s.release_date::TEXT
                FROM vn_bangumi_subject s
                WHERE s.bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND EXTRACT(YEAR FROM s.release_date) = ?
                  AND s.release_date IS NOT NULL
                  AND COALESCE(s.rating_total, 0) > 0
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(s.title_jp, ''), s.title, '') !~* ?
                ORDER BY s.rating_total DESC NULLS LAST, s.bangumi_subject_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getLong("bangumi_subject_id"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score") == null ? null : rs.getBigDecimal("score").doubleValue(),
                rs.getObject("rating_total"),
                rs.getString("release_date")
        }, year, EXCLUSION_REGEX, limit);
    }

    public Object[] queryHotGameDetail(long bangumiSubjectId) {
        String sql = QUALIFIED_CTE + """
                SELECT
                    s.bangumi_subject_id,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(s.score_exact, s.score_api, s.score) AS score,
                    s.rating_total,
                    s.release_date::TEXT,
                    COALESCE(s.rating_1, 0) AS r1, COALESCE(s.rating_2, 0) AS r2, COALESCE(s.rating_3, 0) AS r3,
                    COALESCE(s.rating_4, 0) AS r4, COALESCE(s.rating_5, 0) AS r5, COALESCE(s.rating_6, 0) AS r6,
                    COALESCE(s.rating_7, 0) AS r7, COALESCE(s.rating_8, 0) AS r8, COALESCE(s.rating_9, 0) AS r9, COALESCE(s.rating_10, 0) AS r10
                FROM vn_bangumi_subject s
                WHERE s.bangumi_subject_id = ? AND s.bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                """;
        List<Object[]> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getLong("bangumi_subject_id"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score") == null ? null : rs.getBigDecimal("score").doubleValue(),
                rs.getObject("rating_total"),
                rs.getString("release_date"),
                rs.getInt("r1"), rs.getInt("r2"), rs.getInt("r3"), rs.getInt("r4"), rs.getInt("r5"),
                rs.getInt("r6"), rs.getInt("r7"), rs.getInt("r8"), rs.getInt("r9"), rs.getInt("r10")
        }, bangumiSubjectId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Object[]> queryControversy(int yearFrom, int yearTo, int minVotes) {
        String sql = QUALIFIED_CTE + """
                SELECT
                    s.bangumi_subject_id,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(s.score_exact, s.score_api, s.score) AS score,
                    s.rating_total,
                    COALESCE(s.rating_1, 0) AS r1, COALESCE(s.rating_2, 0) AS r2, COALESCE(s.rating_3, 0) AS r3,
                    COALESCE(s.rating_4, 0) AS r4, COALESCE(s.rating_5, 0) AS r5, COALESCE(s.rating_6, 0) AS r6,
                    COALESCE(s.rating_7, 0) AS r7, COALESCE(s.rating_8, 0) AS r8, COALESCE(s.rating_9, 0) AS r9, COALESCE(s.rating_10, 0) AS r10
                FROM vn_bangumi_subject s
                WHERE s.bangumi_subject_id IN (SELECT bangumi_subject_id FROM qualified_subject)
                  AND s.release_date IS NOT NULL
                  AND EXTRACT(YEAR FROM s.release_date) BETWEEN ? AND ?
                  AND COALESCE(s.rating_total, 0) >= ?
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(s.title_jp, ''), s.title, '') !~* ?
                  AND COALESCE(s.score_exact, s.score_api, s.score) IS NOT NULL
                ORDER BY s.rating_total DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getLong("bangumi_subject_id"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score").doubleValue(),
                rs.getInt("rating_total"),
                rs.getInt("r1"), rs.getInt("r2"), rs.getInt("r3"), rs.getInt("r4"), rs.getInt("r5"),
                rs.getInt("r6"), rs.getInt("r7"), rs.getInt("r8"), rs.getInt("r9"), rs.getInt("r10")
        }, yearFrom, yearTo, minVotes, EXCLUSION_REGEX);
    }

    public java.util.Set<Long> queryGalgameSubjectIds() {
        String sql = QUALIFIED_CTE + """
                SELECT bangumi_subject_id FROM qualified_subject ORDER BY bangumi_subject_id
                """;
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("bangumi_subject_id"));
        return new java.util.HashSet<>(ids);
    }
}
