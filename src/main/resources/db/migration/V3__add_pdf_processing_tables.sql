-- Tabela para armazenar metadados dos PDFs processados
CREATE TABLE IF NOT EXISTS pdf_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(50) NOT NULL,
    error_message TEXT
);

-- Tabela para armazenar dados extraídos dos PDFs
CREATE TABLE IF NOT EXISTS extracted_data (
    id BIGSERIAL PRIMARY KEY,
    pdf_id BIGINT NOT NULL REFERENCES pdf_metadata(id),
    field_name VARCHAR(255) NOT NULL,
    field_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela para logs detalhados do processamento
CREATE TABLE IF NOT EXISTS processing_logs (
    id BIGSERIAL PRIMARY KEY,
    pdf_id BIGINT NOT NULL REFERENCES pdf_metadata(id),
    log_message TEXT NOT NULL,
    logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);