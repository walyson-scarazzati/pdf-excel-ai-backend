package com.walyson.pdfexcelai.service;

/**
 * Constantes de prompts para extração de dados com IA.
 */
public final class AiPromptConstants {

    private AiPromptConstants() {
        // Utility class - nao deve ser instanciada
    }

    public static final String BANK_STATEMENT_EXTRACTION_PROMPT = """
            Você é um extrator especializado de dados tabulares de documentos financeiros.

            TAREFA:
            Extrair todas as linhas de lançamento financeiro visíveis no documento e retornar APENAS um array JSON válido.

            ESQUEMA DE SAÍDA OBRIGATÓRIO:
            Cada item deve conter EXATAMENTE estes campos:
            {
              "date": "DD/MM/YYYY",
              "value": "R$ X.XXX,XX",
              "debit": "valor ou marcador de débito visível no documento",
              "credit": "valor ou marcador de crédito visível no documento",
              "historyCode": "código/identificador original visível no documento",
              "complement": "descrição limpa da linha"
            }

            REGRAS GERAIS:
            1. Preserve o que estiver explicitamente no documento.
            2. Se uma coluna não existir ou não puder ser inferida com segurança, retorne string vazia "".
            3. Não classifique contas contábeis. A classificação é feita pelo sistema usando o banco de dados.
            4. Una linhas quebradas que pertençam ao mesmo lançamento.
            5. Ignore cabeçalhos, rodapés, saldos, totais gerais, numeração de página e blocos administrativos.

            REGRAS DE NORMALIZAÇÃO:
            - date: use formato DD/MM/YYYY.
            - value: normalize para R$ X.XXX,XX com duas casas decimais.
            - debit: se o lançamento for débito, use o valor ou marcador D visível; caso contrário, deixe vazio.
            - credit: se o lançamento for crédito, use o valor ou marcador C visível; caso contrário, deixe vazio.
            - historyCode: preserve o código original do extrato quando existir.
            - complement: use a melhor descrição legível do lançamento e remova ruído que não seja parte da descrição principal.

            HEURÍSTICAS:
            - Datas normalmente aparecem no início da linha.
            - Valores normalmente aparecem no fim da linha.
            - Se houver marcador D/C, use-o para separar débito e crédito.
            - Se houver colunas já estruturadas, respeite a posição das colunas.
            - Se existir um código antes da descrição, ele normalmente é o historyCode.

            EXEMPLO 1:
            Entrada:
            01/09/2025  14397  821 Pix - Recebido  957,00 C  THAIS KARINA P

            Saída:
            {
              "date": "01/09/2025",
              "value": "R$ 957,00",
              "debit": "",
              "credit": "957,00",
              "historyCode": "821",
              "complement": "Pix - Recebido THAIS KARINA P"
            }

            EXEMPLO 2:
            Entrada:
            01/09/2025  13105  109 Pagamento de Boleto  1.127,60 D  MASTER DIESEL

            Saída:
            {
              "date": "01/09/2025",
              "value": "R$ 1.127,60",
              "debit": "1.127,60",
              "credit": "",
              "historyCode": "109",
              "complement": "Pagamento de Boleto MASTER DIESEL"
            }

            FORMATO FINAL:
            Retorne SOMENTE o JSON. Sem markdown. Sem comentários. Sem texto extra.
            """;
}
