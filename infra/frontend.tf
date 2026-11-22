# Not linked to the Git repository on purpose: the deploy workflow publishes
# the frontend after CI passes and the backend is live, never before.
resource "vercel_project" "frontend" {
  name           = "leuven-go"
  framework      = "vite"
  root_directory = "frontend"
}

resource "vercel_project_domain" "frontend" {
  project_id = vercel_project.frontend.id
  domain     = var.frontend_domain
}

# Build-time values that end up in the public bundle, so none are secret.
resource "vercel_project_environment_variables" "frontend" {
  project_id = vercel_project.frontend.id
  variables = [
    for key, value in {
      VITE_API_BASE_URL       = google_cloud_run_v2_service.api.uri
      VITE_WHATSAPP_NUMBER    = var.whatsapp_number
      VITE_WHATSAPP_JOIN_CODE = var.whatsapp_join_code
      SITE_URL                = "https://${var.frontend_domain}"
    } : { key = key, value = value, target = ["production"], sensitive = false } if value != ""
  ]
}
