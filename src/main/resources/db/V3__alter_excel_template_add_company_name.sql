-- ============================================================
-- Add company_name column to excel_template
-- For sales/invoice type templates the company name is stored here.
-- ============================================================

ALTER TABLE excel_template
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);

-- Back-fill existing sales rows so company_name = template_name
UPDATE excel_template
SET    company_name = template_name
WHERE  template_type = 'sales'
  AND  company_name IS NULL;
