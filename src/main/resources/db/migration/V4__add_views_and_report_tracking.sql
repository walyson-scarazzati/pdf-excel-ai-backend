-- Visão consolidada para facilitar a geração de relatórios
CREATE OR REPLACE VIEW consolidated_report_view AS
SELECT 
    pm.id AS pdf_id,
    pm.file_name,
    pm.processed_at,
    ed.field_name,
    ed.field_value
FROM 
    pdf_metadata pm
JOIN 
    extracted_data ed ON pm.id = ed.pdf_id;

-- Tabela para registrar relatórios gerados
CREATE TABLE IF NOT EXISTS generated_reports (
    id BIGSERIAL PRIMARY KEY,
    report_name VARCHAR(255) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    filters_applied TEXT,
    file_path VARCHAR(255) NOT NULL
);