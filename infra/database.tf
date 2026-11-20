# Neon's free plan: Postgres that sleeps after five idle minutes and never bills.
resource "neon_project" "db" {
  name                      = "leuven-go"
  region_id                 = var.neon_region
  pg_version                = 17
  history_retention_seconds = 21600

  branch {
    name          = "main"
    database_name = "leuvengo"
    role_name     = "leuvengo"
  }
}

locals {
  # The direct host, not the pooler: Flyway and Hibernate use prepared
  # statements that PgBouncer's transaction mode doesn't keep.
  jdbc_url = "jdbc:postgresql://${neon_project.db.database_host}/${neon_project.db.database_name}?sslmode=require"
}
