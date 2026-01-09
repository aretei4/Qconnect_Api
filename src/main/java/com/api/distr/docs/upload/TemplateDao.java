package com.api.distr.docs.upload;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class TemplateDao {

    @Autowired
    private JdbcTemplate jdbc;

 
    public void saveOrUpdateTemplate(ExcelTemplate t) {

        // 1. Check count
        String countSql = "SELECT COUNT(1) FROM excel_template WHERE template_name = ?";
        Integer count = jdbc.queryForObject(
                countSql,
                Integer.class,
                t.getTemplateName()
        );

        // 2. Insert or Update
        if (count != null && count > 0) {
            // UPDATE
            String updateSql = """
                UPDATE excel_template
                SET template_type = ?,
                    mappings = ?::jsonb
                WHERE template_name = ?
            """;

            jdbc.update(
                    updateSql,
                    t.getTemplateType(),
                    new JSONObject(t.getMappings()).toString(),
                    t.getTemplateName()
            );

        } else {
            // INSERT
            String insertSql = """
                INSERT INTO excel_template (template_name, template_type, mappings)
                VALUES (?, ?, ?::jsonb)
            """;

            jdbc.update(
                    insertSql,
                    t.getTemplateName(),
                    t.getTemplateType(),
                    new JSONObject(t.getMappings()).toString()
            );
        }
    }


    
    public List<String> getAllTemplateNames() {
        String sql = "SELECT template_name FROM excel_template ORDER BY template_name";
        return jdbc.queryForList(sql, String.class);
    }
    
    public ExcelTemplate getTemplateByName(String name) {

        String sql = "SELECT * FROM excel_template WHERE template_name = ?";

        return jdbc.queryForObject(sql, new Object[]{name}, (rs, rowNum) -> {
            ExcelTemplate t = new ExcelTemplate();
            t.setId(rs.getInt("id"));
            t.setTemplateName(rs.getString("template_name"));
            t.setTemplateType(rs.getString("template_type"));

            String json = rs.getString("mappings");
            try {
				t.setMappings(new ObjectMapper().readValue(json, Map.class));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            return t;
        });
    }
}
