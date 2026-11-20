output "api_url" {
  value = google_cloud_run_v2_service.api.uri
}

output "frontend_url" {
  value = "https://${var.frontend_domain}"
}

# Repository variables the deploy workflow reads (gh variable set NAME --body VALUE).
output "github_variables" {
  value = {
    GCP_PROJECT_ID                 = var.project_id
    GCP_REGION                     = var.region
    GCP_WORKLOAD_IDENTITY_PROVIDER = google_iam_workload_identity_pool_provider.github.name
    GCP_DEPLOYER                   = google_service_account.deployer.email
    GCP_REGISTRY                   = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.images.repository_id}"
    VERCEL_ORG_ID                  = vercel_project.frontend.team_id
    VERCEL_PROJECT_ID              = vercel_project.frontend.id
  }
}
