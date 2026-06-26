package com.api.distr.docs.company;

/**
 * Response DTO for company registry lookup.
 * Returned by GET /api/company/search?q=...
 */
public class CompanyDto {

    private Long    id;
    private String  name;
    private String  code;
    private String  baseUrl;
    private boolean active = true;

    public CompanyDto() {}

    public CompanyDto(Long id, String name, String code, String baseUrl) {
        this.id      = id;
        this.name    = name;
        this.code    = code;
        this.baseUrl = baseUrl;
    }

    public Long    getId()       { return id; }
    public String  getName()     { return name; }
    public String  getCode()     { return code; }
    public String  getBaseUrl()  { return baseUrl; }
    public boolean isActive()    { return active; }

    public void setId(Long id)           { this.id = id; }
    public void setName(String name)     { this.name = name; }
    public void setCode(String code)     { this.code = code; }
    public void setBaseUrl(String url)   { this.baseUrl = url; }
    public void setActive(boolean active){ this.active = active; }
}
