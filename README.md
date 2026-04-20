# PDF Excel AI Backend

API REST em Spring Boot para receber PDFs, extrair dados com apoio de IA e gerar ficheiros Excel.

## Responsabilidades deste repositório

- upload de PDFs
- preview estruturado das linhas extraidas
- exportacao para `.xlsx`
- integracao com endpoint compativel com OpenAI Chat Completions
- fallback heuristico local quando a IA nao estiver configurada

## Stack

- Java 21
- Spring Boot 3.3
- Apache PDFBox
- Apache POI
- Docker
- GitHub Actions

## Configuracao OpenAI

O backend ja envia requests compativeis com a OpenAI API. Para funcionar de verdade, precisas de uma chave de API da OpenAI e de um modelo com capacidade multimodal.

### Variaveis usadas

- `AI_API_URL`: por defeito usa `https://api.openai.com/v1/chat/completions`
- `AI_API_KEY`: chave da tua conta OpenAI API
- `AI_MODEL`: por defeito usa `gpt-4.1-mini`
- `APP_CORS_ALLOWED_ORIGINS`: origens permitidas no CORS, separadas por virgula

### Passo a passo exato

1. Cria ou usa uma chave em OpenAI API.
2. Preenche o ficheiro `.env` na raiz deste repositório.
3. Sobe a API com Maven ou Docker Compose.
4. Configura o frontend para apontar para `http://localhost:8080/api` ou para a URL publicada do backend.

### Exemplo de `.env`

```dotenv
AI_API_URL=https://api.openai.com/v1/chat/completions
AI_API_KEY=sk-coloca-aqui-a-tua-chave-real
AI_MODEL=gpt-4.1-mini
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
```

## Executar localmente

### Com Maven

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

API disponivel em `http://localhost:8080`.

### Versao automatica no Maven

O `pom.xml` usa `revision` em vez de uma versao fixa. Localmente, a default continua `0.0.1-SNAPSHOT`, mas podes injetar outra versao sem editar o ficheiro.

```bash
APP_VERSION=1.2.3 mvn -B clean package
```

Ou, se preferires passar diretamente para o Maven:

```bash
mvn -B clean package -Drevision=1.2.3
```

### Teste rapido da API

```bash
curl -X POST http://localhost:8080/api/documents/preview \
	-F "file=@/caminho/para/documento.pdf"
```

## Docker

### Build da imagem

```bash
docker build -t pdf-excel-ai-backend .
```

Para forcar uma versao especifica no jar e no label da imagem:

```bash
docker build --build-arg APP_VERSION=1.2.3 -t pdf-excel-ai-backend:1.2.3 .
```

### Executar container

```bash
docker run --rm -p 8080:8080 \
	-e AI_API_URL=https://api.openai.com/v1/chat/completions \
	-e AI_API_KEY=sk-coloca-aqui-a-tua-chave-real \
	-e AI_MODEL=gpt-4.1-mini \
	-e APP_CORS_ALLOWED_ORIGINS=http://localhost:4200 \
	pdf-excel-ai-backend
```

### Docker Compose

```bash
docker compose up --build
```

O `docker-compose.yml` deste repositório sobe apenas a API e le os valores do `.env` automaticamente.

### Stack local com frontend + backend

Se tiveres os dois repositórios lado a lado no mesmo diretório de trabalho, usa:

```bash
docker compose -f docker-compose.local.yml up --build
```

Este ficheiro sobe:

- backend em `http://localhost:8080`
- frontend em `http://localhost:4200`

Assume esta estrutura local:

```text
workspace/
	pdf-excel-ai-backend/
	pdf-excel-ai-frontend/
```

## GitHub Actions

### CI

Ficheiro: `.github/workflows/ci.yml`

- executa `mvn -B test`

### Publicacao da imagem Docker

Ficheiro: `.github/workflows/backend-image.yml`

- gera a imagem Docker a partir da raiz deste repositório quando uma tag `v*` e criada
- publica no GitHub Container Registry (`ghcr.io`)

### Versionamento semantico automatico

Ficheiro: `.github/workflows/release-version.yml`

- executa quando um pull request e merged para `main`
- incrementa a propriedade `revision` no `pom.xml`
- cria commit automatico e tag Git `vX.Y.Z`

Labels suportadas no pull request:

- `semver:major`
- `semver:minor`
- `semver:patch`

Se nenhuma label existir, o workflow usa `patch`.

## Integracao com o frontend

O frontend deve apontar para este backend usando uma URL como:

```text
https://teu-backend.exemplo.com/api
```

Se o frontend estiver noutro dominio, atualiza `APP_CORS_ALLOWED_ORIGINS` com essa origem.

## Observacoes

- Ter apenas uma conta ChatGPT nao fornece automaticamente uma `API key` para este backend.
- GitHub Copilot tambem nao substitui a OpenAI API no runtime da aplicacao.
- Para producao, recomenda-se publicar esta imagem em Render, Railway, Fly.io, Azure App Service ou plataforma equivalente.