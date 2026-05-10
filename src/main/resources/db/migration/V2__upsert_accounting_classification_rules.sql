SET search_path TO public;

CREATE UNIQUE INDEX IF NOT EXISTS idx_accounting_classification_rules_name
    ON accounting_classification_rules (name);

INSERT INTO accounting_classification_rules
    (name, keywords, direction, debit_account_code, credit_account_code, history_code, priority, active)
VALUES
    ('Resgate aplicação Banco do Brasil', 'resgate,resgat', 'ANY', '7560', '7579', '68', 10, true),
    ('Aplicação Banco do Brasil', 'rende facil,bb rende,aplicacao,aplicacoes', 'DEBIT', '7579', '7560', '67', 20, true),
    ('Tarifas bancárias', 'tarifa,tar.,cesta,pacote de servicos,despesas bancarias,debito servico cobranca,taria pix,tarifa pix', 'DEBIT', '1880', '7560', '53', 30, true),
    ('Horas extras', 'hora extra,horas extras', 'DEBIT', '4340', '7560', '31', 40, true),
    ('Salários e folha', 'salario,salarios,folha,funcionario,ordenado', 'DEBIT', '1821', '7560', '31', 50, true),
    ('Saque em dinheiro', 'saque,dinheiro,atm', 'DEBIT', '19', '7560', '66', 60, true),
    ('Água e saneamento', 'agua,saneamento,sabesp,semae', 'DEBIT', '1759', '7560', '54', 70, true),
    ('Energia elétrica', 'energia,eletrica,cpfl,edp,enel,conta luz', 'DEBIT', '2941', '7560', '54', 80, true),
    ('Telefone', 'telefone,telefonia,vivo,claro,tim', 'DEBIT', '2950', '7560', '54', 90, true),
    ('Internet', 'internet,web hosting,hosting,host', 'DEBIT', '1724', '7560', '54', 100, true),
    ('Honorários contábeis', 'contabil,contabilidade,honorarios contabeis,sigma assessoria contabil', 'DEBIT', '1660', '7560', '54', 110, true),
    ('Software e programas', 'software,programa de computador,programas de computador,sistema,sistemas', 'DEBIT', '1767', '7560', '54', 120, true),
    ('Vale alimentação', 'beneficio,vr,vale refeicao,vale alimentacao,alimentacao', 'DEBIT', '1830', '7560', '54', 130, true),
    ('Seguros', 'seguro,seguros,apolice,seguradora,tokio marine,suhai', 'DEBIT', '4316', '7560', '162', 140, true),
    ('Cartão corporativo', 'cartao,card', 'DEBIT', '6653', '7560', '78', 150, true),
    ('IPTU', 'iptu', 'DEBIT', '6807', '7560', '142', 160, true),
    ('Impostos federais', 'simples,darf,das,gps,pis,cofins,irpj,csll,irrf,inss,fgts,rfb', 'DEBIT', '1694', '7560', '142', 170, true),
    ('Impostos estaduais', 'icms,estadual,sefaz', 'DEBIT', '1686', '7560', '142', 180, true),
    ('Impostos municipais', 'iss,issqn,municipal,prefeitura', 'DEBIT', '1708', '7560', '142', 190, true),
    ('Licenças e taxa de funcionamento', 'funcionamento,licenca,licenciamento,taxa', 'DEBIT', '6815', '7560', '142', 200, true),
    ('Outros impostos', 'imposto,tributo', 'DEBIT', '1694', '7560', '142', 210, true),
    ('Empréstimos e financiamentos', 'emprest,financ,parcela,contrato,desenvolve sp', 'DEBIT', '9768', '7560', '133', 220, true),
    ('PIX recebido', 'pix recebido,pix - recebido,plx - recebido', 'CREDIT', '7560', '3239', '55', 230, true),
    ('Recebimentos de clientes', 'recebido,credito em conta,transferencia recebida,deposito,cobranca,recebimento fornecedor,ted-credito,ted credito', 'CREDIT', '7560', '3239', '41', 240, true),
    ('Serviços online', 'aplicativos,online', 'DEBIT', '3298', '7560', '54', 250, true),
    ('Pagamento padrão', '', 'DEBIT', '3220', '7560', '54', 1000, true),
    ('Recebimento padrão', '', 'CREDIT', '7560', '3239', '41', 1010, true)
ON CONFLICT (name) DO UPDATE SET
    keywords = EXCLUDED.keywords,
    direction = EXCLUDED.direction,
    debit_account_code = EXCLUDED.debit_account_code,
    credit_account_code = EXCLUDED.credit_account_code,
    history_code = EXCLUDED.history_code,
    priority = EXCLUDED.priority,
    active = EXCLUDED.active;
