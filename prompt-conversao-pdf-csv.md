# Prompt para Conversão de Extrato Bancário PDF para CSV

## Objetivo
Converter extratos bancários em PDF para formato CSV padronizado.

## Formato de Saída CSV

O arquivo CSV deve seguir exatamente esta estrutura:

```csv
DATA;VALOR;DÉBITO;CRÉDITO;CÓDIGO DO HISTÓRICO;COMPLEMENTO
```

### Especificações das Colunas:

1. **DATA**: Data da transação no formato `DD/MM/AAAA` (ex: 01/09/2025)
2. **VALOR**: Valor monetário com prefixo `R$` e formatação brasileira (ex: R$ 1.200,00)
3. **DÉBITO**: Código numérico da conta de débito (ex: 7560, 3220, 434, 19, 1759, 1880, etc)
4. **CRÉDITO**: Código numérico da conta de crédito (ex: 3239, 7560, 7579, etc)
5. **CÓDIGO DO HISTÓRICO**: Código numérico do tipo de transação (ex: 55, 67, 68, 31, 54, 53, 66, etc)
6. **COMPLEMENTO**: Descrição textual da transação (ex: nome do beneficiário, tipo de pagamento, etc)

### Separador
- Use **ponto e vírgula (;)** como separador de colunas

### Formato de Valores
- Moeda: `R$ X.XXX,XX` (ponto para milhares, vírgula para decimais)
- Sempre incluir o prefixo `R$` e espaço após ele
- Valores sem centavos também devem ter `,00` (ex: R$ 1.000,00)

## Instruções de Conversão

### 1. Identificação de Dados no PDF
Extraia do PDF bancário as seguintes informações de cada transação:
- **Data do movimento**: geralmente na coluna "Dt. movimento" ou "DATA"
- **Valor**: na coluna "Valor R$" ou "VALOR"
- **Histórico**: descrição da transação (ex: "821 Pix - Recebido", "109 Pagamento de Boleto", etc)
- **Documento/Identificador**: código ou número do documento quando disponível
- **Complemento**: nome do beneficiário, descrição adicional

### 2. Mapeamento de Códigos

#### Códigos de Histórico Comuns:
- `31`: Transferência enviada (pagamento de salário/funcionário)
- `41`: Recebimento de valores
- `53`: Tarifas bancárias
- `54`: Pagamentos diversos (fornecedores, serviços)
- `55`: Pix recebido de pessoa jurídica
- `66`: Saque/transferência
- `67`: BB Rende Fácil (aplicação)
- `68`: BB Rende Fácil (resgate)
- `78`: Compras/pagamentos
- `97`: Pagamento de funcionário
- `118`: Serviços específicos
- `133`: Financiamento/empréstimo
- `142`: Impostos municipais
- `162`: Seguros
- `168`: Taxas/cadastros
- `175`: Taxas municipais

#### Códigos de Contas Principais:
**Débito (saídas):**
- `7560`: Conta corrente principal
- `19`: Saque em dinheiro
- `434`: Folha de pagamento
- `1660`: Contabilidade
- `1724`: Serviços web/tecnologia
- `1759`: Água e saneamento
- `1830`: Benefícios (VR)
- `1880`: Tarifas bancárias
- `2941`: Energia elétrica
- `2950`: Telefonia
- `3220`: Pagamentos diversos
- `3298`: Serviços online
- `4316`: Seguros
- `6653`: Cartão de crédito
- `6807`: Impostos municipais IPTU
- `6815`: Impostos/taxas
- `9784`: Financiamentos

**Crédito (entradas):**
- `3239`: Recebimentos de clientes/PIX
- `7560`: Conta corrente principal (transferências internas)
- `7579`: Investimento BB Rende Fácil

### 3. Regras de Extração

1. **Ignore linhas de cabeçalho, rodapé e informações complementares**
   - Não extraia: "Saldo Anterior", "Taxa Limite Esp.", "CET", informações sobre limite especial

2. **Para cada lançamento bancário**:
   - Extraia a DATA do movimento (não a data de balancete)
   - Extraia o VALOR sempre em formato monetário brasileiro
   - Identifique se é DÉBITO (saída) ou CRÉDITO (entrada) pela descrição
   - Extraia o CÓDIGO DO HISTÓRICO do campo "Ag. origem" ou do código numérico no início da descrição
   - Monte o COMPLEMENTO com a descrição principal da transação

3. **Tratamento de Valores**:
   - Débitos: valores que saem da conta principal (7560 no débito)
   - Créditos: valores que entram na conta principal (7560 no crédito)
   - Aplicações/Resgates: investimentos (7579)

4. **Limpeza de Dados**:
   - Remova caracteres especiais mal formatados (ex: � por ê, á, ç, etc)
   - Mantenha apenas letras, números e espaços no COMPLEMENTO
   - Normalize a formatação dos valores monetários

### 4. Exemplo de Conversão

**Entrada no PDF:**
```
01/09/2025  0000  14397  821 Pix - Recebido
                         31/08 19:50 05951601665 THAIS KARINA P
                         311.950.207.494.141    957,00 C
```

**Saída no CSV:**
```csv
01/09/2025;R$ 957,00;7560;3239;55;THAIS KARINA P
```

**Entrada no PDF:**
```
01/09/2025  0000  13105  109 Pagamento de Boleto
                         MASTER DIESEL BOMBAS INJETORA
                         90.101    1.127,60 D
```

**Saída no CSV:**
```csv
01/09/2025;R$ 1.127,60;3220;7560;54;MASTER DIESEL BOMBAS INJETORA
```

## Prompt para IA

```
Você é um especialista em conversão de extratos bancários. Sua tarefa é converter extratos em PDF para formato CSV seguindo estas especificações:

FORMATO CSV:
DATA;VALOR;DÉBITO;CRÉDITO;CÓDIGO DO HISTÓRICO;COMPLEMENTO

REGRAS:
1. Use ponto e vírgula (;) como separador
2. DATA no formato DD/MM/AAAA
3. VALOR no formato R$ X.XXX,XX (sempre com R$ e duas casas decimais)
4. DÉBITO e CRÉDITO são códigos numéricos das contas
5. CÓDIGO DO HISTÓRICO é um número que identifica o tipo de transação
6. COMPLEMENTO é a descrição da transação (sem caracteres especiais mal formatados)

MAPEAMENTO:
- Conta principal (débito/crédito): 7560
- Recebimentos PIX/Clientes: 3239 no crédito
- Investimento BB Rende Fácil: 7579
- Pagamentos diversos: 3220 no débito
- Folha de pagamento: 434 no débito
- Tarifas bancárias: 1880 no débito
- Código 55: PIX recebido
- Código 54: Pagamentos de fornecedores/serviços
- Código 67: Aplicação financeira
- Código 68: Resgate financeiro
- Código 31: Pagamento de salário

INSTRUÇÕES:
1. Analise o PDF do extrato bancário
2. Para cada lançamento, extraia: data, valor, tipo (débito/crédito), descrição
3. Mapeie os códigos corretos de débito, crédito e histórico
4. Limpe o texto do complemento (remova caracteres especiais)
5. Formate cada linha exatamente como especificado
6. Não inclua linhas de saldo, totalizadores ou informações administrativas
7. Mantenha apenas os lançamentos financeiros reais

Converta o seguinte extrato:
[INSIRA O TEXTO DO PDF AQUI]
```

## Observações Importantes

1. **Caracteres especiais**: O PDF pode conter caracteres mal codificados (� ao invés de ã, ê, ç). Normalize-os na conversão.

2. **Tarifas agrupadas**: Quando aparecer "Tar. agrupadas - ocorrencia XX/XX/XXXX", é um totalizador de tarifas do dia, geralmente ignore ou consolide.

3. **BB Rende Fácil**: 
   - Aplicação (débito): 7560 → 7579 (código 67)
   - Resgate (crédito): 7579 → 7560 (código 68)

4. **PIX**: Geralmente código 55 para recebimentos, com CNPJ/CPF e nome no complemento.

5. **Valores com vírgula**: No PDF podem aparecer valores soltos como "749" ou "530" - verifique se são R$ 749,00 ou R$ 7,49 pelo contexto.

6. **Primeira linha**: Deve ser o cabeçalho com os nomes das colunas.
