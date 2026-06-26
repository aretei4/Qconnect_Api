package com.api.distr.docs.config;

import java.time.LocalDateTime;

public class FeatureConfigDTO {

    private String        key;
    private boolean       enabled;
    private String        label;
    private String        description;
    private String        category;
    private LocalDateTime updatedAt;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getKey()              { return key; }
    public void   setKey(String v)      { this.key = v; }

    public boolean isEnabled()          { return enabled; }
    public void    setEnabled(boolean v){ this.enabled = v; }

    public String getLabel()            { return label; }
    public void   setLabel(String v)    { this.label = v; }

    public String getDescription()      { return description; }
    public void   setDescription(String v) { this.description = v; }

    public String getCategory()         { return category; }
    public void   setCategory(String v) { this.category = v; }

    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
