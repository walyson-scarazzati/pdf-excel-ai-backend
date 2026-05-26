# Deploy no Google Cloud com Terraform

Esta pasta provisiona infraestrutura para rodar o backend `pdf-excel-ai` no GCP com foco em baixo custo:

- Cloud Run (servico HTTP)
- Artifact Registry (imagens Docker)
- Secret Manager (DB_URL, DB_USERNAME, DB_PASSWORD, AI_API_KEY)
- Service Account dedicada para o Cloud Run

## Arquitetura recomendada para creditos/free tier

Para usar o credito de 90 dias com menor risco de custo fixo:

1. Use **Cloud Run** com `min_instances = 0`.
2. Use banco externo ja existente (Supabase/Neon/Render Postgres), via `DB_URL` em segredo.
3. Mantenha `max_instances` baixo (ex.: 2) e ajuste depois.

> Observacao: Cloud SQL nao faz parte do free tier recorrente, e tende a consumir credito continuamente.

## Pre-requisitos

- Terraform >= 1.6
- Google Cloud SDK autenticado (`gcloud auth application-default login`)
- Projeto GCP criado e billing habilitado
- Docker local (para build/push da imagem)

## Passo a passo

1. Copie o exemplo de variaveis:

```bash
cd infra/gcp
cp terraform.tfvars.example terraform.tfvars
```

2. Edite `terraform.tfvars`:

- `project_id`
- `container_image`
- `plain_env.APP_CORS_ALLOWED_ORIGINS`
- `secret_values` com credenciais reais

Se o `terraform apply` falhar com `projects/SEU_PROJECT_ID is invalid`, o arquivo `terraform.tfvars` ainda esta com o placeholder do exemplo. Substitua pelo ID real do projeto GCP e ajuste tambem `container_image` para usar esse mesmo projeto.

3. Inicialize o Terraform:

```bash
terraform init
```

Exemplo minimo:

```hcl
project_id      = "meu-projeto-gcp"
container_image = "us-central1-docker.pkg.dev/meu-projeto-gcp/pdf-excel-ai/backend:latest"
```

4. No primeiro deploy, crie primeiro as APIs e o Artifact Registry:

```bash
terraform apply \
	-target='google_project_service.enabled' \
	-target='google_artifact_registry_repository.docker'
```

> Observacao: o Cloud Run referencia `container_image`, entao o deploy completo depende de a imagem ja existir no Artifact Registry.

> Observacao: nao defina manualmente a env `PORT` no Terraform para Cloud Run. Esse valor e reservado e injetado automaticamente pela plataforma.

5. Build e push da imagem:

```bash
PROJECT_ID="SEU_PROJECT_ID"
REGION="us-central1"
REPO="pdf-excel-ai"
IMAGE="backend:latest"

gcloud auth configure-docker ${REGION}-docker.pkg.dev

docker build -t ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${IMAGE} ../..
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${IMAGE}
```

6. Rode o deploy completo:

```bash
terraform plan
terraform apply
```

7. Consulte a URL publicada:

```bash
terraform output cloud_run_url
```

## Variaveis principais

- `container_image`: imagem Docker no Artifact Registry
- `plain_env`: variaveis nao sensiveis
- `secret_env`: nome das env vars mapeadas para Secret Manager
- `secret_values`: valores opcionais para criar versoes de segredo no apply

## Outputs

- `cloud_run_url`: URL publica da API
- `artifact_registry_repository`: base para push de imagens
- `run_service_account_email`: SA usada pelo Cloud Run

## Atualizar segredos sem recriar stack

Edite `secret_values` no `terraform.tfvars` e rode:

```bash
terraform apply
```

O Terraform cria uma nova versao no Secret Manager e o Cloud Run usa `latest`.
