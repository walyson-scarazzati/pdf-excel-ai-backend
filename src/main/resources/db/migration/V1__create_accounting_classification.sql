CREATE TABLE accounting_accounts (
    code VARCHAR(16) PRIMARY KEY,
    full_code VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    account_group VARCHAR(80) NOT NULL,
    source VARCHAR(120) NOT NULL DEFAULT 'plano de contas.pdf',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accounting_history_codes (
    code VARCHAR(16) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accounting_classification_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    keywords TEXT NOT NULL DEFAULT '',
    direction VARCHAR(16) NOT NULL DEFAULT 'ANY',
    debit_account_code VARCHAR(16) NOT NULL REFERENCES accounting_accounts(code),
    credit_account_code VARCHAR(16) NOT NULL REFERENCES accounting_accounts(code),
    history_code VARCHAR(16) NOT NULL REFERENCES accounting_history_codes(code),
    priority INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT accounting_classification_rules_direction_check
        CHECK (direction IN ('ANY', 'CREDIT', 'DEBIT'))
);

CREATE INDEX idx_accounting_classification_rules_active_priority
    ON accounting_classification_rules(active, priority);

INSERT INTO accounting_accounts (code, full_code, description, account_group) VALUES
    ('19', '1101010001', 'CAIXA', 'DISPONIBILIDADES'),
    ('7560', '1101020001', 'BANCO DO BRASIL', 'BANCOS CONTA MOVIMENTO'),
    ('3336', '1101020002', 'BANCO SANTANDER', 'BANCOS CONTA MOVIMENTO'),
    ('7579', '1101030001', 'APLICAÇÕES - BRASIL', 'APLICACOES FINANCEIRAS'),
    ('3239', '2101080001', 'ADIANTAMENTO DE CLIENTES', 'ADIANTAMENTO DE CLIENTES'),
    ('3220', '1103020001', 'FORNECEDORES', 'ADIANTAMENTO A FORNECEDORES'),
    ('1821', '4302010048', 'SALÁRIOS', 'DESPESAS ADMINISTRATIVAS'),
    ('4340', '4302010128', 'HORAS EXTRAS', 'DESPESAS ADMINISTRATIVAS'),
    ('1660', '4302010022', 'HONORÁRIOS CONTÁBEIS', 'DESPESAS ADMINISTRATIVAS'),
    ('1724', '4302010028', 'INTERNET', 'DESPESAS ADMINISTRATIVAS'),
    ('1767', '4302010042', 'MANUTENÇÃO DE PROGRAMAS DE COMPUTADOR', 'DESPESAS ADMINISTRATIVAS'),
    ('1759', '4302010041', 'ÁGUA', 'DESPESAS ADMINISTRATIVAS'),
    ('1830', '4302010049', 'VALE ALIMENTACAO', 'DESPESAS ADMINISTRATIVAS'),
    ('1848', '4302010050', 'VALE TRANSPORTE', 'DESPESAS ADMINISTRATIVAS'),
    ('1880', '4303010003', 'DESPESAS BANCÁRIAS', 'DESPESAS FINANCEIRAS'),
    ('2941', '4302010101', 'ENERGIA ELÉTRICA', 'DESPESAS ADMINISTRATIVAS'),
    ('2950', '4302010102', 'TELEFONE', 'DESPESAS ADMINISTRATIVAS'),
    ('3298', '4302010116', 'SERVIÇOS DE TRANSPORTES', 'DESPESAS ADMINISTRATIVAS'),
    ('4316', '4302010125', 'SEGUROS DIVERSOS', 'DESPESAS ADMINISTRATIVAS'),
    ('6653', '4302010081', 'CARTÃO CORPORATIVO', 'DESPESAS ADMINISTRATIVAS'),
    ('6807', '4302020014', 'IPTU', 'DESPESAS TRIBUTARIAS'),
    ('1694', '4302020004', 'IMPOSTOS E TAXAS FEDERAIS', 'DESPESAS TRIBUTARIAS'),
    ('1686', '4302020002', 'IMPOSTOS E TAXAS ESTADUAIS', 'DESPESAS TRIBUTARIAS'),
    ('1708', '4302020003', 'IMPOSTOS E TAXAS MUNICIPAIS', 'DESPESAS TRIBUTARIAS'),
    ('6815', '4302020015', 'TAXAS DE FUNCIONAMENTO', 'DESPESAS TRIBUTARIAS'),
    ('9768', '4303010018', 'JUROS S/ EMPRESTIMOS E FINANCIAMENTOS', 'DESPESAS FINANCEIRAS');

INSERT INTO accounting_history_codes (code, description) VALUES
    ('31', 'Transferência enviada / folha / salários'),
    ('41', 'Recebimento de valores'),
    ('53', 'Tarifas bancárias'),
    ('54', 'Pagamentos diversos / fornecedores / serviços'),
    ('55', 'PIX recebido de cliente'),
    ('66', 'Saque / transferência'),
    ('67', 'Aplicação financeira'),
    ('68', 'Resgate financeiro'),
    ('78', 'Cartão'),
    ('133', 'Financiamento / empréstimo'),
    ('142', 'Impostos e taxas'),
    ('162', 'Seguros');

INSERT INTO accounting_classification_rules
    (name, keywords, direction, debit_account_code, credit_account_code, history_code, priority)
VALUES
    ('Resgate aplicação Banco do Brasil', 'resgate,resgat', 'ANY', '7560', '7579', '68', 10),
    ('Aplicação Banco do Brasil', 'rende facil,bb rende,aplicacao,aplicacoes', 'DEBIT', '7579', '7560', '67', 20),
    ('Tarifas bancárias', 'tarifa,tar.,cesta,pacote de servicos,despesas bancarias', 'DEBIT', '1880', '7560', '53', 30),
    ('Horas extras', 'hora extra,horas extras', 'DEBIT', '4340', '7560', '31', 40),
    ('Salários e folha', 'salario,salarios,folha,funcionario,ordenado', 'DEBIT', '1821', '7560', '31', 50),
    ('Saque em dinheiro', 'saque,dinheiro', 'DEBIT', '19', '7560', '66', 60),
    ('Água e saneamento', 'agua,saneamento,sabesp', 'DEBIT', '1759', '7560', '54', 70),
    ('Energia elétrica', 'energia,eletrica,cpfl,edp,enel', 'DEBIT', '2941', '7560', '54', 80),
    ('Telefone', 'telefone,telefonia,vivo,claro,tim', 'DEBIT', '2950', '7560', '54', 90),
    ('Internet', 'internet', 'DEBIT', '1724', '7560', '54', 100),
    ('Honorários contábeis', 'contabil,contabilidade,honorarios contabeis', 'DEBIT', '1660', '7560', '54', 110),
    ('Software e programas', 'software,programa de computador,programas de computador,sistema', 'DEBIT', '1767', '7560', '54', 120),
    ('Vale alimentação', 'beneficio,vr,vale refeicao,vale alimentacao,alimentacao', 'DEBIT', '1830', '7560', '54', 130),
    ('Seguros', 'seguro,seguros,apolice', 'DEBIT', '4316', '7560', '162', 140),
    ('Cartão corporativo', 'cartao,card', 'DEBIT', '6653', '7560', '78', 150),
    ('IPTU', 'iptu', 'DEBIT', '6807', '7560', '142', 160),
    ('Impostos federais', 'simples,darf,das,gps,pis,cofins,irpj,csll,irrf,inss,fgts', 'DEBIT', '1694', '7560', '142', 170),
    ('Impostos estaduais', 'icms,estadual', 'DEBIT', '1686', '7560', '142', 180),
    ('Impostos municipais', 'iss,issqn,municipal', 'DEBIT', '1708', '7560', '142', 190),
    ('Licenças e taxa de funcionamento', 'funcionamento,licenca,licenciamento,taxa', 'DEBIT', '6815', '7560', '142', 200),
    ('Outros impostos', 'imposto,tributo', 'DEBIT', '1694', '7560', '142', 210),
    ('Empréstimos e financiamentos', 'emprest,financ,parcela,contrato', 'DEBIT', '9768', '7560', '133', 220),
    ('PIX recebido', 'pix recebido,pix - recebido', 'CREDIT', '7560', '3239', '55', 230),
    ('Recebimentos de clientes', 'recebido,credito em conta,transferencia recebida,deposito', 'CREDIT', '7560', '3239', '41', 240),
    ('Serviços online', 'aplicativos,online', 'DEBIT', '3298', '7560', '54', 250),
    ('Pagamento padrão', '', 'DEBIT', '3220', '7560', '54', 1000),
    ('Recebimento padrão', '', 'CREDIT', '7560', '3239', '41', 1010);
