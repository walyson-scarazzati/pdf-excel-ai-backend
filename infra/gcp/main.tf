locals {
  apis = toset([
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudbuild.googleapis.com",
    "iam.googleapis.com"
  ])

  service_account_id = substr(replace(var.service_name, "_", "-"), 0, 28)
  reserved_env_names = toset(["PORT", "K_SERVICE", "K_REVISION", "K_CONFIGURATION"])
  plain_env_filtered = {
    for k, v in var.plain_env : k => v
    if !contains(local.reserved_env_names, k)
  }
}

resource "google_project_service" "enabled" {
  for_each = local.apis
  project  = var.project_id
  service  = each.value

  disable_dependent_services = false
  disable_on_destroy         = false
}

resource "google_artifact_registry_repository" "docker" {
  project       = var.project_id
  location      = var.region
  repository_id = var.artifact_repository_id
  format        = "DOCKER"
  description   = "Docker images for ${var.service_name}"

  depends_on = [google_project_service.enabled]
}

resource "google_service_account" "run" {
  project      = var.project_id
  account_id   = local.service_account_id
  display_name = "Cloud Run SA - ${var.service_name}"

  depends_on = [google_project_service.enabled]
}

resource "google_project_iam_member" "run_secret_access" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.run.email}"
}

resource "google_secret_manager_secret" "env" {
  for_each  = var.secret_env
  project   = var.project_id
  secret_id = each.value

  replication {
    auto {}
  }

  labels = var.labels

  depends_on = [google_project_service.enabled]
}

resource "google_secret_manager_secret_version" "seed" {
  for_each = {
    for env_name, secret_id in var.secret_env : env_name => secret_id
    if contains(keys(var.secret_values), env_name) && trimspace(var.secret_values[env_name]) != ""
  }

  secret      = google_secret_manager_secret.env[each.key].id
  secret_data = var.secret_values[each.key]
}

resource "google_cloud_run_v2_service" "app" {
  name     = var.service_name
  project  = var.project_id
  location = var.region
  ingress  = var.ingress
  deletion_protection = var.deletion_protection

  template {
    service_account = google_service_account.run.email
    timeout         = "${var.timeout_seconds}s"

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    containers {
      image = var.container_image

      ports {
        container_port = var.container_port
      }

      resources {
        limits = {
          cpu    = var.cpu
          memory = var.memory
        }
      }

      dynamic "env" {
        for_each = local.plain_env_filtered
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = var.secret_env
        content {
          name = env.key
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.env[env.key].secret_id
              version = "latest"
            }
          }
        }
      }
    }
  }

  labels = var.labels

  depends_on = [
    google_project_service.enabled,
    google_artifact_registry_repository.docker,
    google_secret_manager_secret.env
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  count    = var.allow_unauthenticated ? 1 : 0
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.app.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
