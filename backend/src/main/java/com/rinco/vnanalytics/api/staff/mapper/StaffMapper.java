package com.rinco.vnanalytics.api.staff.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffMapper {

    private final JdbcTemplate jdbcTemplate;

    public StaffMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Object[]> queryAllPersons() {
        String sql = """
                SELECT bangumi_person_id, name, name_cn, name_jp, image_url, relation
                FROM vn_bangumi_person
                ORDER BY name ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getLong("bangumi_person_id"),
                rs.getString("name"),
                rs.getString("name_cn"),
                rs.getString("name_jp"),
                rs.getString("image_url"),
                rs.getString("relation")
        });
    }
}
