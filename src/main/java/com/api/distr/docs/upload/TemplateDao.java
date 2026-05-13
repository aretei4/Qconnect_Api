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

        // Auto-generate a stable template_name from company + type when company is provided
        if (t.getCompanyName() != null && !t.getCompanyName().isBlank()) {
            t.setTemplateName(t.getCompanyName().trim() + "_" + t.getTemplateType().trim());
        }

        String mappingsJson = new JSONObject(t.getMappings()).toString();

        String countSql = "SELECT COUNT(1) FROM excel_template WHERE template_name = ?";
        Integer count = jdbc.queryForObject(countSql, Integer.class, t.getTemplateName());

        if (count != null && count > 0) {
            String updateSql = """
                UPDATE excel_template
                SET template_type = ?,
                    mappings      = ?::jsonb,
                    company_name  = ?
                WHERE template_name = ?
            """;
            jdbc.update(updateSql,
                    t.getTemplateType(), mappingsJson, t.getCompanyName(), t.getTemplateName());
        } else {
            String insertSql = """
                INSERT INTO excel_template (template_name, template_type, mappings, company_name)
                VALUES (?, ?, ?::jsonb, ?)
            """;
            jdbc.update(insertSql,
                    t.getTemplateName(), t.getTemplateType(), mappingsJson, t.getCompanyName());
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
            t.setCompanyName(rs.getString("company_name"));

            String json = rs.getString("mappings");
            try {
                t.setMappings(new ObjectMapper().readValue(json, Map.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return t;
        });
    }

    /** Returns all sales/invoice templates as {templateName, companyName} objects. */
    public List<ExcelTemplate> getSalesCompanies() {
        String sql = """
            SELECT template_name, company_name
            FROM   excel_template
            WHERE  template_type = 'sales'
            ORDER  BY company_name
            """;
        return jdbc.query(sql, (rs, rowNum) -> {
            ExcelTemplate t = new ExcelTemplate();
            t.setTemplateName(rs.getString("template_name"));
            t.setCompanyName(rs.getString("company_name"));
            return t;
        });
    }

    /** All distinct company names (non-null) across all templates. */
    public List<String> getDistinctCompanies() {
        String sql = """
            SELECT DISTINCT company_name
            FROM   excel_template
            WHERE  company_name IS NOT NULL AND company_name <> ''
            ORDER  BY company_name
            """;
        return jdbc.queryForList(sql, String.class);
    }

    /** All templates that belong to a specific company (includes full mappings). */
    public List<ExcelTemplate> getTemplatesByCompany(String companyName) {
        String sql = "SELECT * FROM excel_template WHERE company_name = ? ORDER BY template_type";
        return jdbc.query(sql, new Object[]{companyName}, (rs, rowNum) -> {
            ExcelTemplate t = new ExcelTemplate();
            t.setId(rs.getInt("id"));
            t.setTemplateName(rs.getString("template_name"));
            t.setTemplateType(rs.getString("template_type"));
            t.setCompanyName(rs.getString("company_name"));
            String json = rs.getString("mappings");
            try {
                t.setMappings(new ObjectMapper().readValue(json, Map.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return t;
        });
    }
}
