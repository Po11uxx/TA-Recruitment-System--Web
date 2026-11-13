# TA offer email deployment handoff

**Requirement** — Email the student immediately when Admin formally publishes a TA offer. This applies to both application-based offers and direct assignments. Approval and draft creation precede formal publication.

**Status** — The application records one email request in PostgreSQL in the same transaction as publication. Campus IT owns mail delivery and production acceptance. Keep this item open in deployment and acceptance documents until IT verifies delivery.

## Application integration contract

Migration V17 creates the `offer_emails` outbox. A committed row represents one published offer.

| Field | Consumer use |
| --- | --- |
| `id` | Placement ID and stable idempotency key. |
| `recipient_email` | Student's account email at publication. |
| `course_code`, `course_name`, `section_name` | Offer details for the email template. |
| `placement_path` | Append to the configured public application base URL, including the servlet context path. The student signs in to view and respond to their placement. |
| `created_at` | Publication timestamp for measuring dispatch latency. |
| `row_order` | Application persistence metadata. |

IT should give the mail worker read access to the outbox and track delivery attempts in its own durable ledger. Use the placement ID for deduplication and provider idempotency where supported. Consume new committed requests promptly and retry transient failures. The deployment team owns template approval, service credentials, the public base URL, monitoring, and handling of failed deliveries. Agree on the acceptable dispatch latency with the sponsor before production acceptance.

## Production acceptance

Publish an application-based offer and a direct assignment using approved test accounts. For each, verify that exactly one email is delivered within the agreed time and that its link reaches the correct placement. Verify that Teacher approval and draft creation leave the outbox empty for those placements. Simulate a temporary delivery failure and confirm recovery without duplicate delivery.
