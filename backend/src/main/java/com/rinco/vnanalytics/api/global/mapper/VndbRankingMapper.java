package com.rinco.vnanalytics.api.global.mapper;

import com.rinco.vnanalytics.api.global.model.GlobalSiteScoreItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class VndbRankingMapper {

    private final JdbcTemplate jdbcTemplate;

    public VndbRankingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int updateRankExcluded(String vndbId, boolean excluded) {
        String sql = "UPDATE vn_vndb_subject SET excluded_from_rank = ? WHERE vndb_subject_id = ?";
        return jdbcTemplate.update(sql, excluded, vndbId);
    }

    public List<GlobalSiteScoreItem> queryTop(int minVotes, int limit, String exclusionRegex) {
        String sql = """
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                                 s.rating_total DESC NULLS LAST,
                                 s.vndb_subject_id ASC
                    ) AS rank_value,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(s.score_exact, s.score_api, s.score) AS score_value,
                    m.id AS match_triple_id
                FROM vn_vndb_subject s
                LEFT JOIN vn_match_triple m ON m.vndb_subject_id = s.vndb_subject_id
                WHERE COALESCE(s.rating_total, 0) > ?
                  AND COALESCE(s.score_exact, s.score_api, s.score) IS NOT NULL
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(NULLIF(s.title_jp, ''), s.title, '') !~* ?
                ORDER BY COALESCE(s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                         s.rating_total DESC NULLS LAST,
                         s.vndb_subject_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GlobalSiteScoreItem(
                rs.getInt("rank_value"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score_value").doubleValue(),
                readMatchTripleId(rs)
        ), minVotes, exclusionRegex, limit);
    }

    public List<GlobalSiteScoreItem> queryCuratedPoolVndb(int limit) {
        String sql = """
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(m.vndb_score, s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                                 COALESCE(s.rating_total, 0) DESC NULLS LAST,
                                 m.vndb_subject_id ASC
                    ) AS rank_value,
                    COALESCE(NULLIF(s.title_jp, ''), s.title) AS title_jp,
                    COALESCE(m.vndb_score, s.score_exact, s.score_api, s.score) AS score_value,
                    m.id AS match_triple_id
                FROM vn_match_triple m
                INNER JOIN vn_vndb_subject s ON s.vndb_subject_id = m.vndb_subject_id
                WHERE m.vndb_subject_id IS NOT NULL
                  AND COALESCE(m.vndb_score, s.score_exact, s.score_api, s.score) IS NOT NULL
                ORDER BY COALESCE(m.vndb_score, s.score_exact, s.score_api, s.score) DESC NULLS LAST,
                         COALESCE(s.rating_total, 0) DESC NULLS LAST,
                         m.vndb_subject_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GlobalSiteScoreItem(
                rs.getInt("rank_value"),
                rs.getString("title_jp"),
                readNullableDouble(rs, "score_value"),
                readMatchTripleId(rs)
        ), limit);
    }

    private static Long readMatchTripleId(ResultSet rs) throws SQLException {
        long v = rs.getLong("match_triple_id");
        return rs.wasNull() ? null : v;
    }

    private static Double readNullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }
}
