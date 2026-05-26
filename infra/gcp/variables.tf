variable "project_id" {
  description = "GCP project ID"
  type        = string

  validation {
    condition = (
      trimspace(var.project_id) != "" &&
      lower(trimspace(var.project_id)) != "seu_project_id" &&
      can(regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$", trimspace(var.project_id)))
    )
    error_message = "project_id must be a real GCP project ID, not the placeholder SEU_PROJECT_ID. Update terraform.tfvars before running plan/apply."
  }
}

variable "region" {
  description = "GCP region for Cloud Run and Artifact Registry"
  type        = string
  default     = "us-central1"
}

variable "service_name" {
  description = "Cloud Run service name"
  type        = string
  default     = "pdf-excel-ai-backend"
}

variable "artifact_repository_id" {
  description = "Artifact Registry repository ID"
  type        = string
  default     = "pdf-excel-ai"
}

variable "container_image" {
  description = "Full container image URL (Artifact Registry), e.g. us-central1-docker.pkg.dev/my-project/pdf-excel-ai/backend:latest"
  type        = string

  validation {
    condition = (
      trimspace(var.container_image) != "" &&
      !strcontains(lower(var.container_image), "seu_project_id")
    )
    error_message = "container_image must point to a real Artifact Registry image and cannot contain the placeholder SEU_PROJECT_ID."
  }
}

variable "container_port" {
  description = "Port used by the Spring Boot container"
  type        = number
  default     = 8080
}

variable "allow_unauthenticated" {
  description = "Allow public unauthenticated access to Cloud Run"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Enable Cloud Run deletion protection"
  type        = bool
  default     = false
}

variable "min_instances" {
  description = "Minimum number of Cloud Run instances"
  type        = number
  default     = 0
}

variable "max_instances" {
  description = "Maximum number of Cloud Run instances"
  type        = number
  default     = 2
}

variable "cpu" {
  description = "CPU limit for Cloud Run container"
  type        = string
  default     = "1"
}

variable "memory" {
  description = "Memory limit for Cloud Run container"
  type        = string
  default     = "1Gi"
}

variable "timeout_seconds" {
  description = "Request timeout for Cloud Run service"
  type        = number
  default     = 300
}

variable "ingress" {
  description = "Ingress policy for Cloud Run. Values: INGRESS_TRAFFIC_ALL, INGRESS_TRAFFIC_INTERNAL_ONLY, INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER"
  type        = string
  default     = "INGRESS_TRAFFIC_ALL"
}

variable "plain_env" {
  description = "Non-sensitive environment variables passed directly to Cloud Run"
  type        = map(string)
  default = {
    SPRING_PROFILES_ACTIVE   = "prod"
    APP_CORS_ALLOWED_ORIGINS = "*"
    AI_PROVIDER              = "github-models"
    AI_API_URL               = "https://models.github.ai/inference/chat/completions"
    AI_MODEL                 = "openai/gpt-4.1-mini"
    AI_GITHUB_API_VERSION    = "2026-03-10"
    OCR_ENABLED              = "true"
    OCR_COMMAND              = "tesseract"
    OCR_MODE                 = "auto"
    OCR_LANGUAGE             = "por"
    OCR_PSM                  = "4"
    OCR_TIMEOUT_SECONDS      = "25"
    OCR_MAX_PAGES            = "2"
    OCR_PREPROCESS_ENABLED   = "true"
    OCR_CONTRAST_FACTOR      = "1.35"
    OCR_THRESHOLD            = "168"
    OCR_UPSCALE_FACTOR       = "1"
    OCR_DESKEW_ENABLED       = "true"
    OCR_MAX_DESKEW_ANGLE     = "3.0"
    OCR_CROP_TOP_RATIO       = "0.12"
    OCR_CROP_BOTTOM_RATIO    = "0.03"
    OCR_CROP_SIDE_RATIO      = "0.03"
    LOG_LEVEL_ROOT           = "INFO"
    LOG_LEVEL_SPRING         = "INFO"
  }
}

variable "secret_env" {
  description = "Map env var -> Secret Manager secret_id. Secrets must exist or be created by secret_values."
  type        = map(string)
  default = {
    DB_URL      = "db-url"
    DB_USERNAME = "db-username"
    DB_PASSWORD = "db-password"
    AI_API_KEY  = "ai-api-key"
  }
}

variable "secret_values" {
  description = "Optional secret values to create secret versions. Keep empty in VCS and pass via tfvars or -var-file"
  type        = map(string)
  default     = {}

  validation {
    condition = alltrue([
      for value in values(var.secret_values) :
      trimspace(value) == "" || (
        !strcontains(lower(value), "seu_banco_host") &&
        !strcontains(lower(value), "seu_usuario") &&
        !strcontains(lower(value), "sua_senha") &&
        !strcontains(lower(value), "seu_token_github_models")
      )
    ])
    error_message = "secret_values must contain real credentials or be left empty. Remove placeholders such as SEU_BANCO_HOST, SEU_USUARIO, SUA_SENHA, and SEU_TOKEN_GITHUB_MODELS before plan/apply."
  }
}

variable "labels" {
  description = "Labels applied to resources"
  type        = map(string)
  default = {
    app         = "pdf-excel-ai"
    managed_by  = "terraform"
    environment = "prod"
  }
}
