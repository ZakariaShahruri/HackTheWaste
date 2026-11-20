import base64
import json
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest

import main


def budget_event(cost: float, budget: float = 1.0):
    payload = {"costAmount": cost, "budgetAmount": budget, "currencyCode": "EUR"}
    data = base64.b64encode(json.dumps(payload).encode()).decode()
    return SimpleNamespace(data={"message": {"data": data}})


@pytest.fixture
def billing(monkeypatch):
    client = MagicMock()
    client.get_project_billing_info.return_value = SimpleNamespace(billing_enabled=True)
    monkeypatch.setattr(main.billing_v1, "CloudBillingClient", lambda: client)
    monkeypatch.setenv("PROJECT_ID", "leuven-go")
    return client


def test_spend_within_the_budget_leaves_billing_on(billing):
    main.on_budget_update(budget_event(cost=1.0))

    billing.update_project_billing_info.assert_not_called()


def test_spend_past_the_budget_unlinks_billing(billing):
    main.on_budget_update(budget_event(cost=1.01))

    billing.update_project_billing_info.assert_called_once()
    call = billing.update_project_billing_info.call_args.kwargs
    assert call["name"] == "projects/leuven-go"
    assert call["project_billing_info"].billing_account_name == ""


def test_a_project_already_without_billing_is_left_alone(billing):
    billing.get_project_billing_info.return_value = SimpleNamespace(billing_enabled=False)

    main.on_budget_update(budget_event(cost=5.0))

    billing.update_project_billing_info.assert_not_called()
