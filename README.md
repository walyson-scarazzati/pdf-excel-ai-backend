# Sistema de Extração de Extratos Bancários - PDF/CSV para Excel

Sistema completo para converter extratos bancários em PDF ou CSV para planilhas Excel formatadas.

## 🎯 Funcionalidades

- ✅ Upload de arquivos PDF ou CSV
- ✅ Extração automática de dados de extrato bancário
- ✅ Visualização em tabela formatada
- ✅ Cálculo de totais (créditos e débitos)
- ✅ Exportação para Excel (.xlsx)
- ✅ Suporte a formato brasileiro (DD/MM/YYYY, R$ X.XXX,XX)

## 📋 Estrutura de Dados

O sistema processa extratos com as seguintes colunas:
- **Data**: Data da transação (DD/MM/YYYY)
- **Valor**: Valor da transação
- **Débito**: Conta de débito
- **Crédito**: Conta de crédito
- **Código do Histórico**: Código numérico da transação
- **Complemento**: Descrição/complemento da transação

## 🏗️ Arquitetura

### Backend (Java/Spring Boot)
- **Java 21** + **Spring Boot 3.5.13**
- **Apache PDFBox 3.0.3** - Extração de texto de PDF
- **Apache POI 5.3.0** - Geração de arquivos Excel
- Endpoints REST para upload e processamento

### Frontend (Angular 18)
- **Angular 18** standalone components
- Interface moderna e responsiva
- Visualização de dados em tempo real

## 🚀 Como Executar

### Backend

./mvnw clean install
./mvnw spring-boot:run
```

O backend estará disponível em `http://localhost:8081`

### Frontend

```bash
cd pdf-excel-ai-frontend
npm install
npm start
```

O frontend estará disponível em `http://localhost:4200`

## 🐳 Docker

### Backend
```bash
cd pdf-excel-ai-backend
docker build -t pdf-excel-backend .
docker run -p 8081:8081 pdf-excel-backend
```

### Frontend
```bash
cd pdf-excel-ai-frontend
docker build -t pdf-excel-frontend .
docker run -p 4200:80 pdf-excel-frontend
```

### Docker Compose
```bash
cp .env.local.example .env
docker compose up -d postgres backend
```

O compose sobe Postgres e backend. O Flyway roda na inicialização do backend e cria as tabelas/regras contábeis a partir das migrations em `src/main/resources/db/migration`.

Para subir tambem o frontend pelo compose, mantenha o repositorio `pdf-excel-ai-frontend` ao lado deste projeto e use o profile:

```bash
docker compose --profile frontend up -d
```

Para subir apenas o banco local:

```bash
docker compose up -d postgres
```

Configuração padrão do banco:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/pdf_excel_ai
DB_USERNAME=pdf_excel_ai
DB_PASSWORD=pdf_excel_ai
```

### Variaveis para local e nuvem

Use `.env.local.example` para rodar com Postgres local via Docker Compose. Use `.env.cloud.example` quando o banco for externo, como Supabase ou DigitalOcean Managed PostgreSQL. Localmente, o compose le `.env` automaticamente. Em nuvem, cadastre as mesmas chaves como environment variables da plataforma, sem commitar `.env`.

Variaveis essenciais:

```dotenv
PORT=8081
HOST_PORT=8081
DB_URL=jdbc:postgresql://postgres:5432/pdf_excel_ai
DB_USERNAME=pdf_excel_ai
DB_PASSWORD=<senha-forte>
AI_PROVIDER=github-models
AI_API_URL=https://models.github.ai/inference/chat/completions
AI_API_KEY=<token>
AI_MODEL=openai/gpt-4.1-mini
OCR_ENABLED=true
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200,https://seu-frontend.vercel.app
```

Em plataformas como DigitalOcean App Platform, Render ou Fly.io, a variavel `PORT` pode ser definida pela propria plataforma. Em Droplet com Docker Compose, `HOST_PORT` controla a porta publicada no servidor.

Para rodar em um servidor com banco externo e sem subir o Postgres local:

```bash
cp .env.cloud.example .env
docker compose -f docker-compose.cloud.yml up -d --build backend
```

Em deploys como Render usando Supabase, `DB_URL` precisa ser uma URL JDBC completa. A tela do Supabase avisa que conexoes diretas usam IPv6 por padrao; como o Render pode ser IPv4-only, prefira o Session Pooler em `Connect > Pooler settings > Session pooler`.

```dotenv
DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<sua-senha>
```

Se voce ativar o IPv4 add-on no Supabase ou estiver em uma rede com IPv6, a conexao direta tambem funciona:

```dotenv
DB_URL=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=<sua-senha>
```

Se `DB_URL` estiver apenas como `db.<project-ref>.supabase.co`, o driver PostgreSQL rejeita a conexão com o erro `claims to not accept jdbcUrl`.
O backend tambem normaliza esse formato automaticamente para Supabase, mas manter a URL JDBC completa no Render evita ambiguidade.

## 📝 Formato CSV Aceito

O sistema aceita arquivos CSV no formato:

```csv
DATA ;VALOR;DEBITO ;CRÉDITO ;CÓDIGO DO HISTÓRICO ;COMPLEMENTO 
01/09/2025;R$ 957,00;7560;3239;55;THAIS KARINA P
02/09/2025;R$ 500,00;1830;7560;54;VR BENEFICIOS
```

## 📊 Exemplo de Uso

1. Acesse `http://localhost:4200`
2. Clique em "Escolher documento (PDF ou CSV)"
3. Selecione seu arquivo de extrato bancário
4. Clique em "Analisar documento" para visualizar os dados
5. Clique em "Exportar Excel" para baixar o arquivo .xlsx

## 🔧 Principais Serviços

### Backend

- **DocumentController**: Endpoints REST
- **PdfTextExtractor**: Extração de texto de PDF
- **BankStatementParserService**: Parser de extratos bancários
- **AccountingClassificationService**: Classificação contábil baseada em regras no Postgres
- **JdbcAccountingClassificationRepository**: Leitura das regras semeadas pelo Flyway
- **ExcelExportService**: Geração de arquivos Excel
- **DocumentProcessingService**: Orquestração do processo

### Frontend

- **DocumentService**: Comunicação com API
- **AppComponent**: Interface principal

## 🎨 Recursos do Excel Gerado

- ✅ Cabeçalhos formatados
- ✅ Informações da conta e período
- ✅ Colunas dimensionadas automaticamente
- ✅ Linha de totais com fórmulas
- ✅ Formato de moeda brasileiro (R$)
- ✅ Auto-filtro ativado
- ✅ Congelamento de cabeçalho

## 📄 Endpoints da API

### POST /api/documents/preview
Upload de documento para visualização prévia dos dados extraídos.

**Request**: `multipart/form-data` com arquivo PDF ou CSV

**Response**: JSON com dados extraídos e metadados

### POST /api/documents/export  
Upload de documento para geração de arquivo Excel.

**Request**: `multipart/form-data` com arquivo PDF ou CSV

**Response**: Arquivo Excel (.xlsx) para download

## ☁️ Deploy no Google Cloud (Terraform)

Foi adicionada uma stack Terraform em `infra/gcp` para provisionar:

- Cloud Run
- Artifact Registry
- Secret Manager
- Service Account com permissões mínimas

Passos rápidos:

```bash
cd infra/gcp
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform apply
```

Depois, publique a imagem Docker no Artifact Registry e atualize `container_image` no `terraform.tfvars`.

Guia completo: `infra/gcp/README.md`.

## 🛠️ Configuração (Opcional)

O sistema suporta integração com IA para extração mais avançada (opcional):

### Variáveis de Ambiente

- `AI_PROVIDER`: Provider de IA (openai ou github-models)
- `AI_API_URL`: URL da API de IA
- `AI_API_KEY`: Chave de API
- `AI_MODEL`: Modelo a ser utilizado
- `DB_URL`: JDBC URL completa do Postgres, por exemplo `jdbc:postgresql://host:5432/database?sslmode=require`
- `DB_USERNAME`: Usuário do Postgres
- `DB_PASSWORD`: Senha do Postgres
- `OCR_ENABLED`: Ativar OCR para PDFs escaneados
- `APP_CORS_ALLOWED_ORIGINS`: Origens permitidas para CORS

## OCR Local no macOS

Para PDFs com texto embutido corrompido, o caminho mais confiável é OCR local.

1. Instale o Tesseract:

```bash
brew install tesseract
```

2. Ative o OCR no `.env`:

```dotenv
OCR_ENABLED=true
OCR_COMMAND=tesseract
OCR_MODE=auto
OCR_LANGUAGE=por
OCR_PSM=4
OCR_TIMEOUT_SECONDS=25
OCR_MAX_PAGES=5
OCR_PREPROCESS_ENABLED=true
OCR_CONTRAST_FACTOR=1.35
OCR_THRESHOLD=168
OCR_UPSCALE_FACTOR=2
OCR_DESKEW_ENABLED=true
OCR_MAX_DESKEW_ANGLE=3.0
OCR_CROP_TOP_RATIO=0.12
OCR_CROP_BOTTOM_RATIO=0.03
OCR_CROP_SIDE_RATIO=0.03
```

Use `por+eng` apenas se o documento misturar muito portugues com termos em ingles. Para extrato bancario em PT-BR, `por` como base tende a produzir menos ruido.

Para extratos, `OCR_PSM=4` costuma funcionar melhor do que `6`, porque o Tesseract tenta respeitar linhas de texto separadas em vez de assumir um unico bloco uniforme.

O backend agora detecta perfis como Banco do Brasil e Santander. Para Banco do Brasil, o modo padrao passa a ser `ocr-only`; para Santander, `ocr-first`. Tambem existe pre-processamento de imagem com binarizacao, contraste e recorte da regiao do corpo do extrato antes do OCR.

No layout Banco do Brasil, o OCR agora pode aplicar `upscale`, `deskew`, remocao simples de ruido e leitura por colunas fixas para gerar linhas mais rigidas para o parser.

3. Reinicie o backend.

4. Verifique o status em `http://localhost:8081/api/health`.

Se `ocrEnabled=true` e `ocrAvailable=true`, o backend está pronto para usar OCR local mesmo sem IA externa.

## 📄 Licença

MIT License

## 👨‍💻 Desenvolvido por

Walyson Scarazzati
