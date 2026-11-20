"""Unlinks billing from the project once spend passes the budget (see budget.tf)."""

import base64
import json
import os

import functions_framework
from google.cloud import billing_v1


def over_budget(notification: dict) -> bool:
    return notification["costAmount"] > notification["budgetAmount"]


@functions_framework.cloud_event
def on_budget_update(event) -> None:
    notification = json.loads(base64.b64decode(event.data["message"]["data"]))
    if not over_budget(notification):
        return

    project = f"projects/{os.environ['PROJECT_ID']}"
    billing = billing_v1.CloudBillingClient()
    if not billing.get_project_billing_info(name=project).billing_enabled:
        return

    billing.update_project_billing_info(
        name=project,
        project_billing_info=billing_v1.ProjectBillingInfo(billing_account_name=""),
    )
    print(
        f"Spend {notification['costAmount']} passed the budget "
        f"{notification['budgetAmount']}: billing is now off for {project}"
    )
