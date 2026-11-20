resource "random_password" "pseudonym_secret" {
  length  = 48
  special = false
}

locals {
  # Optional integrations only get a secret once they have a value.
  secret_values = merge(
    {
      "database-password" = neon_project.db.database_password
      "pseudonym-secret"  = random_password.pseudonym_secret.result
    },
    var.gemini_api_key == "" ? {} : { "gemini-api-key" = var.gemini_api_key },
    var.twilio_auth_token == "" ? {} : { "twilio-auth-token" = var.twilio_auth_token },
  )
}

resource "google_secret_manager_secret" "api" {
  for_each = nonsensitive(toset(keys(local.secret_values)))

  secret_id = each.value
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }

  depends_on = [google_project_service.enabled]
}

resource "google_secret_manager_secret_version" "api" {
  for_each = google_secret_manager_secret.api

  secret      = each.value.id
  secret_data = local.secret_values[each.key]
}

resource "google_secret_manager_secret_iam_member" "api_reads" {
  for_each = google_secret_manager_secret.api

  secret_id = each.value.id
  role      = "roles/secretmanager.secretAccessor"
  member    = google_service_account.api.member
}
