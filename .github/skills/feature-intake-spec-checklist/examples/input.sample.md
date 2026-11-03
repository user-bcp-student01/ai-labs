## Business context (brief)
Reduce complaints caused by duplicate transfers.

## Feature request / user story
As a user, I want to confirm a transfer before sending it
so that I can avoid duplicates.

## Expected impact
Reduce duplicate-transfer complaints by 20% in one month.

## Scope (in/out)
In: pre-send confirmation, summary message, data validation.
Out: infrastructure changes and a complete UI redesign.

## Known technical constraints
Existing REST API, web frontend, standardized logging.

## Known risks
Sensitive data exposure in logs; amount-validation errors.

## Dependencies
Accounts service and transfers service.

## Team Definition of Done (DoD)
Unit tests, brief documentation, and reviewed PR.