# Free tiers cover this demo, so any real spend means something is wrong.
# Past the budget, a function unlinks billing from the project, which stops
# every paid resource in it. Budget data lags by a few hours, so this caps a
# runaway bill at a few dollars rather than at exactly the budget.
resource "google_pubsub_topic" "budget" {
  name = "budget-alerts"

  depends_on = [google_project_service.enabled]
}

resource "google_billing_budget" "monthly" {
  billing_account = var.billing_account_id
  display_name    = "leuven-go monthly"

  budget_filter {
    projects = ["projects/${data.google_project.this.number}"]
  }

  amount {
    specified_amount {
      units = tostring(var.monthly_budget)
    }
  }

  # Emails the billing admins along the way.
  threshold_rules {
    threshold_percent = 0.5
  }
  threshold_rules {
    threshold_percent = 1.0
  }

  all_updates_rule {
    pubsub_topic = google_pubsub_topic.budget.id
  }
}

resource "google_service_account" "kill_switch" {
  account_id   = "billing-kill-switch"
  display_name = "Unlinks billing when the budget is exceeded"
}

resource "google_project_iam_member" "kill_switch_unlinks_billing" {
  project = var.project_id
  role    = "roles/billing.projectManager"
  member  = google_service_account.kill_switch.member
}

resource "google_project_iam_member" "kill_switch_receives_events" {
  project = var.project_id
  role    = "roles/eventarc.eventReceiver"
  member  = google_service_account.kill_switch.member
}

data "archive_file" "kill_switch" {
  type        = "zip"
  source_dir  = "${path.module}/kill-switch"
  output_path = "${path.module}/.build/kill-switch.zip"
  excludes    = ["test_main.py", "__pycache__"]
}

resource "google_storage_bucket" "functions" {
  name                        = "${var.project_id}-functions"
  location                    = var.region
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = true
}

resource "google_storage_bucket_object" "kill_switch" {
  name   = "kill-switch-${data.archive_file.kill_switch.output_md5}.zip"
  bucket = google_storage_bucket.functions.name
  source = data.archive_file.kill_switch.output_path
}

resource "google_cloudfunctions2_function" "kill_switch" {
  name     = "billing-kill-switch"
  location = var.region

  build_config {
    runtime     = "python313"
    entry_point = "on_budget_update"
    source {
      storage_source {
        bucket = google_storage_bucket.functions.name
        object = google_storage_bucket_object.kill_switch.name
      }
    }
  }

  service_config {
    max_instance_count    = 1
    available_memory      = "256M"
    service_account_email = google_service_account.kill_switch.email
    environment_variables = {
      PROJECT_ID = var.project_id
    }
  }

  event_trigger {
    trigger_region        = var.region
    event_type            = "google.cloud.pubsub.topic.v1.messagePublished"
    pubsub_topic          = google_pubsub_topic.budget.id
    retry_policy          = "RETRY_POLICY_RETRY"
    service_account_email = google_service_account.kill_switch.email
  }

  depends_on = [google_project_service.enabled]
}

# Eventarc delivers to the function through its Cloud Run service.
resource "google_cloud_run_v2_service_iam_member" "kill_switch_invoked_by_eventarc" {
  name     = google_cloudfunctions2_function.kill_switch.name
  location = var.region
  role     = "roles/run.invoker"
  member   = google_service_account.kill_switch.member
}
