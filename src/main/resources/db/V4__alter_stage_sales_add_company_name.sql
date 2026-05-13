-- Add company_name to stage_sales_entery so every uploaded row knows its source company.
ALTER TABLE stage_sales_entery
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);
