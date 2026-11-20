variable "project_id" {
  description = "Google Cloud project that runs the backend and the ML service."
  type        = string
}

variable "region" {
  description = "Cloud Run and Artifact Registry region. europe-west1 is in Belgium, next to Leuven."
  type        = string
  default     = "europe-west1"
}

variable "billing_account_id" {
  description = "Billing account the budget watches, e.g. 012345-6789AB-CDEF01."
  type        = string
}

variable "monthly_budget" {
  description = "Spend, in the billing account's currency, at which billing is switched off for the project."
  type        = number
  default     = 1
}

variable "github_repository" {
  description = "owner/name of the repository whose main branch may deploy."
  type        = string
  default     = "ZakariaShahruri/HackTheWaste"
}

variable "github_repository_id" {
  description = "Numeric id of that repository. Matching on it stops a renamed or recreated repo from inheriting deploy rights."
  type        = string
  default     = "1233105426"
}

variable "neon_region" {
  description = "Neon region for Postgres, close to the Cloud Run region."
  type        = string
  default     = "aws-eu-central-1"
}

variable "vercel_team" {
  description = "Vercel team slug or id that owns the frontend project."
  type        = string
}

variable "frontend_domain" {
  description = "Production domain of the frontend; must be free if it's a *.vercel.app name."
  type        = string
}

variable "gemini_api_key" {
  description = "Google AI Studio key for photo scoring. Empty runs without AI scores."
  type        = string
  default     = ""
  sensitive   = true
}

variable "gemini_model" {
  description = "Gemini model for photo scoring."
  type        = string
  default     = "gemini-2.5-flash-lite"
}

variable "twilio_account_sid" {
  description = "Twilio account for the WhatsApp channel. Empty leaves WhatsApp off."
  type        = string
  default     = ""
}

variable "twilio_auth_token" {
  description = "Twilio auth token, used to download media and check webhook signatures."
  type        = string
  default     = ""
  sensitive   = true
}

variable "whatsapp_number" {
  description = "WhatsApp number for the report page, digits only as wa.me expects (e.g. 14155238886). Empty hides it."
  type        = string
  default     = ""
}

variable "whatsapp_join_code" {
  description = "Twilio sandbox join code shown on the report page."
  type        = string
  default     = ""
}
