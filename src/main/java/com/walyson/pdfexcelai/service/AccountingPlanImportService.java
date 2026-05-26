package com.walyson.pdfexcelai.service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccountingPlanImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPlanImportService.class);
    private static final String UPSERT_SQL = """
            INSERT INTO accounting_accounts (code, full_code, description, account_group, source)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (code) DO UPDATE SET
                full_code = EXCLUDED.full_code,
                description = EXCLUDED.description,
                account_group = EXCLUDED.account_group,
                source = EXCLUDED.source
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    @Value("${app.accounting.plan-import.enabled:true}")
    private boolean enabled;

    @Value("${app.accounting.plan-import.resource:classpath:plano de contas.xlsx}")
    private String resourceLocation;

    public AccountingPlanImportService(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importPlanIfAvailable() {
        if (!enabled) {
            LOGGER.info("Importacao do plano de contas desativada (app.accounting.plan-import.enabled=false)");
            return;
        }

        Resource resource = resourceLoader.getResource(resourceLocation);
        if (!resource.exists()) {
            LOGGER.info("Arquivo de plano de contas nao encontrado em {}. Seguindo com dados do Flyway.", resourceLocation);
            return;
        }

        int imported = 0;
        try (InputStream inputStream = resource.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row header = findHeaderRow(sheet, formatter);
            if (header == null) {
                LOGGER.warn("Nao foi possivel identificar cabecalho no arquivo {}", resourceLocation);
                return;
            }

            int firstDataRow = header.getRowNum() + 1;
            HeaderIndexes indexes = resolveIndexes(header, formatter);
            String source = resolveSourceName(resource);

            for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String code = digits(value(row, indexes.code(), formatter));
                String fullCode = digits(value(row, indexes.fullCode(), formatter));
                String description = value(row, indexes.description(), formatter);
                String accountGroup = value(row, indexes.accountGroup(), formatter);

                if (!StringUtils.hasText(code) || !StringUtils.hasText(description)) {
                    continue;
                }

                if (!StringUtils.hasText(fullCode)) {
                    fullCode = code;
                }
                if (!StringUtils.hasText(accountGroup)) {
                    accountGroup = "NAO INFORMADO";
                }

                jdbcTemplate.update(UPSERT_SQL, code, fullCode, description, accountGroup, source);
                imported++;
            }

            LOGGER.info("Plano de contas importado com sucesso: {} registro(s) atualizados de {}", imported, resourceLocation);
        } catch (Exception ex) {
            LOGGER.error("Falha ao importar plano de contas de {}", resourceLocation, ex);
        }
    }

    private Row findHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 25); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String rowText = normalize(value(row, 0, formatter) + " " + value(row, 1, formatter) + " " + value(row, 2, formatter));
            if (rowText.contains("codigo") || rowText.contains("descricao") || rowText.contains("conta")) {
                return row;
            }
        }
        return null;
    }

    private HeaderIndexes resolveIndexes(Row header, DataFormatter formatter) {
        Map<String, Integer> byToken = new HashMap<>();
        int fallback = 0;
        for (Cell cell : header) {
            String normalized = normalize(formatter.formatCellValue(cell));
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            byToken.putIfAbsent(normalized, cell.getColumnIndex());
            fallback = Math.max(fallback, cell.getColumnIndex());
        }

        int codeIndex = findColumn(byToken, "codigo reduzido", "codigo", "conta");
        int fullCodeIndex = findColumn(byToken, "codigo completo", "codigo estrutural", "conta contabil");
        int descriptionIndex = findColumn(byToken, "descricao", "descricao da conta", "nome");
        int groupIndex = findColumn(byToken, "grupo", "natureza", "classificacao", "subgrupo");

        if (codeIndex < 0) {
            codeIndex = 0;
        }
        if (descriptionIndex < 0) {
            descriptionIndex = Math.min(codeIndex + 1, fallback);
        }
        if (fullCodeIndex < 0) {
            fullCodeIndex = Math.min(descriptionIndex + 1, fallback);
        }
        if (groupIndex < 0) {
            groupIndex = Math.min(fullCodeIndex + 1, fallback);
        }

        return new HeaderIndexes(codeIndex, fullCodeIndex, descriptionIndex, groupIndex);
    }

    private int findColumn(Map<String, Integer> byToken, String... probes) {
        for (Map.Entry<String, Integer> entry : byToken.entrySet()) {
            String token = entry.getKey();
            for (String probe : probes) {
                if (token.contains(normalize(probe))) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }

    private String value(Row row, int columnIndex, DataFormatter formatter) {
        if (columnIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String resolveSourceName(Resource resource) {
        try {
            if (resource.getFilename() != null) {
                return resource.getFilename();
            }
        } catch (Exception ex) {
            LOGGER.debug("Nao foi possivel resolver nome do arquivo de origem", ex);
        }
        return resourceLocation;
    }

    private String normalize(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private record HeaderIndexes(int code, int fullCode, int description, int accountGroup) {
    }
}
