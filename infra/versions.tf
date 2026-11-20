terraform {
  required_version = ">= 1.9"

  # The bucket is passed at init: terraform init -backend-config="bucket=<project>-tfstate"
  backend "gcs" {
    prefix = "leuven-go"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.12"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.8"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
    neon = {
      source  = "kislerdm/neon"
      version = "~> 0.18"
    }
    vercel = {
      source  = "vercel/vercel"
      version = "~> 5.27"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  # Billing budgets are checked against this project's quota, not a default one.
  billing_project       = var.project_id
  user_project_override = true
}

# Reads NEON_API_KEY.
provider "neon" {}

# Reads VERCEL_API_TOKEN.
provider "vercel" {
  team = var.vercel_team
}
