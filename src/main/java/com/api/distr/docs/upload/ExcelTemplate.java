package com.api.distr.docs.upload;

import java.util.Map;

public class ExcelTemplate {
    private Integer id;
    private String  templateName;
    private String  templateType;
    private String  companyName;            // only meaningful for type = "sales"
    private Map<String, String> mappings;   // JSONB

    public Integer getId()                            { return id; }
    public void    setId(Integer id)                  { this.id = id; }

    public String  getTemplateName()                  { return templateName; }
    public void    setTemplateName(String templateName){ this.templateName = templateName; }

    public String  getTemplateType()                  { return templateType; }
    public void    setTemplateType(String templateType){ this.templateType = templateType; }

    public String  getCompanyName()                   { return companyName; }
    public void    setCompanyName(String companyName) { this.companyName = companyName; }

    public Map<String, String> getMappings()          { return mappings; }
    public void    setMappings(Map<String, String> mappings){ this.mappings = mappings; }
}

