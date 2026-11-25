locals {
  # Terraform creates the services with Google's sample image; the deploy
  # workflow then rolls out the real ones and owns the image from there on.
  placeholder_image = "us-docker.pkg.dev/cloudrun/container/hello"

  # Cloud Run URLs are deterministic, which lets the API know its own webhook
  # URL without a dependency cycle.
  api_name = "leuven-go-api"
  api_url  = "https://${local.api_name}-${data.google_project.this.number}.${var.region}.run.app"

  api_env = merge(
    {
      SPRING_PROFILES_ACTIVE     = "prod"
      SPRING_DATASOURCE_URL      = local.jdbc_url
      SPRING_DATASOURCE_USERNAME = neon_project.db.database_user
      ALLOWED_ORIGINS            = "https://${var.frontend_domain}"
      SITE_URL                   = "https://${var.frontend_domain}"
      ML_API_URL                 = google_cloud_run_v2_service.ml.uri
      ML_GOOGLE_ID_TOKEN         = "true"
      LEUVENGO_DEMO_ENABLED      = "true"
      GEMINI_MODEL               = var.gemini_model
    },
    var.twilio_account_sid == "" ? {} : {
      TWILIO_ACCOUNT_SID = var.twilio_account_sid
      TWILIO_WEBHOOK_URL = "${local.api_url}/api/whatsapp/webhook"
    },
  )

  api_secret_env = {
    SPRING_DATASOURCE_PASSWORD = "database-password"
    PSEUDONYM_SECRET           = "pseudonym-secret"
    GEMINI_API_KEY             = "gemini-api-key"
    TWILIO_AUTH_TOKEN          = "twilio-auth-token"
  }
}

resource "google_service_account" "api" {
  account_id   = "leuven-go-api"
  display_name = "Leuven Go API (runtime)"

  depends_on = [google_project_service.enabled]
}

resource "google_service_account" "ml" {
  account_id   = "leuven-go-ml"
  display_name = "Leuven Go ML service (runtime)"

  depends_on = [google_project_service.enabled]
}

resource "google_cloud_run_v2_service" "ml" {
  name                = "leuven-go-ml"
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = false

  template {
    service_account = google_service_account.ml.email

    # One instance at most: a traffic spike queues up instead of scaling the bill.
    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = local.placeholder_image

      ports {
        container_port = 8000
      }
      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }
      startup_probe {
        http_get {
          path = "/health"
        }
        period_seconds    = 2
        failure_threshold = 30
      }
    }
  }

  lifecycle {
    ignore_changes = [template[0].containers[0].image, client, client_version]
  }

  depends_on = [google_project_service.enabled]
}

resource "google_cloud_run_v2_service" "api" {
  name                = local.api_name
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = false

  template {
    service_account = google_service_account.api.email

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = local.placeholder_image

      ports {
        container_port = 8080
      }
      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        # Billed only while handling requests; startup gets extra CPU so a
        # cold JVM is ready in seconds.
        cpu_idle          = true
        startup_cpu_boost = true
      }

      dynamic "env" {
        for_each = local.api_env
        content {
          name  = env.key
          value = env.value
        }
      }
      dynamic "env" {
        for_each = { for name, secret in local.api_secret_env : name => secret if contains(keys(google_secret_manager_secret.api), secret) }
        content {
          name = env.key
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.api[env.value].secret_id
              version = "latest"
            }
          }
        }
      }

      # Flyway, and a demo replay when the week has gone stale, run before
      # the app reports healthy.
      startup_probe {
        http_get {
          path = "/actuator/health"
        }
        period_seconds    = 5
        timeout_seconds   = 3
        failure_threshold = 48
      }
    }
  }

  lifecycle {
    ignore_changes = [template[0].containers[0].image, client, client_version]
  }

  depends_on = [
    google_project_service.enabled,
    google_secret_manager_secret_version.api,
    google_secret_manager_secret_iam_member.api_reads,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "api_public" {
  name     = google_cloud_run_v2_service.api.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# The ML service stays private: only the API's service account may call it.
resource "google_cloud_run_v2_service_iam_member" "ml_from_api" {
  name     = google_cloud_run_v2_service.ml.name
  location = var.region
  role     = "roles/run.invoker"
  member   = google_service_account.api.member
}
