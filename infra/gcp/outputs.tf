output "cloud_run_url" {
  description = "Public URL of the Cloud Run service"
  value       = google_cloud_run_v2_service.app.uri
}

output "artifact_registry_repository" {
  description = "Artifact Registry repository URL base"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.docker.repository_id}"
}

output "run_service_account_email" {
  description = "Service account used by Cloud Run"
  value       = google_service_account.run.email
}

output "required_secrets" {
  description = "Secret IDs expected by Cloud Run"
  value       = { for env_name, secret_id in var.secret_env : env_name => secret_id }
}
