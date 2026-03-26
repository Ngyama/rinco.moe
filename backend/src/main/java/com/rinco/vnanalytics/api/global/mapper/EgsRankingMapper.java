package com.rinco.vnanalytics.api.global.mapper;

import com.rinco.vnanalytics.api.global.model.GlobalSiteScoreItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class EgsRankingMapper {

    private final JdbcTemplate jdbcTemplate;

    public EgsRankingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int updateRankExcluded(long egsId, boolean excluded) {
        String sql = "UPDATE vn_egs_subject SET excluded_from_rank = ? WHERE egs_game_id = ?";
        return jdbcTemplate.update(sql, excluded, egsId);
    }

    public List<GlobalSiteScoreItem> queryTop(int minVotes, int limit, String exclusionRegex) {
        String sql = """
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY s.score_avg DESC NULLS LAST,
                                 s.rating_total DESC NULLS LAST,
                                 s.egs_game_id ASC
                    ) AS rank_value,
                    s.title_jp,
                    s.score_avg AS score_value,
                    m.id AS match_triple_id
                FROM vn_egs_subject s
                LEFT JOIN vn_match_triple m ON m.egs_game_id = s.egs_game_id
                WHERE COALESCE(s.rating_total, 0) > ?
                  AND s.score_avg IS NOT NULL
                  AND COALESCE(s.excluded_from_rank, FALSE) = FALSE
                  AND COALESCE(s.title_jp, '') !~* ?
                ORDER BY s.score_avg DESC NULLS LAST,
                         s.rating_total DESC NULLS LAST,
                         s.egs_game_id ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GlobalSiteScoreItem(
                rs.getInt("rank_value"),
                rs.getString("title_jp"),
                rs.getBigDecimal("score_value").doubleValue(),
                readMatchTripleId(rs)
        ), minVotes, exclusionRegex, limit);
    }

    public List<GlobalSiteScoreItem> queryCuratedPoolEgs(int limit) {
        String sql = """
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY COALESCE(m.egs_score, s.score_avg) DESC NULLS LAST,
                                 COALESCE(s.rating_total, 0) DESC NULLS LAST,
                                 m.egs_game_id ASC
                    ) AS rank_value,
                    s.title_jp,
                    COALESCE(m.egs_score, s.score_avg) AS score_value,
                    m.id AS match_triple_id
                FROM vn_match_triple m
                INNER JOIN vn_egs_subject s ON s.egs_game_id = m.egs_game_id
                WHERE m.egs_game_id IS NOT NULL
                  AND COALESCE(m.egs_score, s.score_avg) IS NOT NULL
                ORDER BY COALESCE(m.egs_score, s.score_avg) DESC NULLS LAST,
                         COALESCE(s.rating_total, 0) DESC NULLS LAST,
                         m.egs_game_id ASC
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
