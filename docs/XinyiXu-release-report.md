# UCI BioSci TA Application System — Change Report

This report covers the changes on `XinyiXu` since `2c1fb97`. The release makes the Teacher responsible for course staffing, gives admin a system-wide oversight view, simplifies TA placement replies, and lets Applicants describe their schedule by marking unavailable time.

## System state machine

| Lifecycle | States and transitions | Actor and conditions |
| --- | --- | --- |
| Account | Public registration creates an Applicant account. Login authenticates the stored account and routes to its assigned workspace. | An Applicant can register publicly. Admin creates staff accounts through Users. The stored account role determines access. |
| Application period | Draft → Scheduled or Open → Closed or Expired. A scheduled period becomes effectively open at its start time. An open period becomes effectively expired at its deadline. | Admin manages the window. Submissions and availability edits require an eligible open window. Staff can continue reviewing after intake ends. |
| TA posting | An active posting is eligible for display when the relevant application window and posting conditions are satisfied. | Admin assigns course ownership. The Teacher configures Sections and matching criteria and publishes a posting for their course. An open application period alone does not guarantee a visible posting. |
| Application | Applied → Review or Shortlist → Offer or Rejected. Review stages are optional. An eligible Applicant can withdraw an undecided submission, which removes it from the active list. | Applicants submit qualification and availability snapshots. Staff use Pipeline to organize progress and Review Workspace to record notes, recommendations and decisions. The formal approval creates an Offer; formal rejection records a reason. |
| TA placement | Draft → Published and Awaiting Reply → Accepted or Declined. Authorized staff can cancel an active placement. | Direct TA selection records approval and creates a Section assignment draft together. The Applicant sees the placement after publication and can accept or decline online. Decline or cancellation releases capacity and preserves history. Pending placements remain pending until reply or cancellation. |

An application decision and a placement reply are separate records. An **Offer** in Pipeline means the Applicant was selected. **Accepted** in TA Placements means the Applicant accepted a published Section assignment. Direct selection checks the course and period, Section eligibility, capacity, duplicate placements, and schedule conflicts. When availability evidence is incomplete, staff must confirm it and record why.

## Changes in this release

### Sign-in, navigation, and branding

- Removed the role selector from sign-in. Credentials are checked against the stored account, and routing supports Applicant, Teacher, admin, and existing advanced review roles. Public registration creates Applicant accounts; the backend rejects attempts to register as staff.
- Standardized visible wording around the **TA Application System** and the **Applicant** label. Updated help material, the demonstration script, and architecture diagram sources.
- Linked the BioSci wordmark on sign-in and registration to the school's website. Consolidated sidebar branding and updated the landing page with a spaced, left-aligned introduction, an **Apply now** call to action, and lab and student photography. Photo credits are in [Homepage photo sources](../backend/src/main/webapp/static/images/HOMEPAGE_PHOTO_SOURCES.md).
- Reordered Applicant navigation to Dashboard, TA Application Profile, Weekly Availability, Find Jobs, My Applications, and My TA Placements. The account area opens the personal profile on desktop and mobile. Restyled native dropdown arrows and adjusted the Teacher Pipeline to keep cards accessible at narrower widths.

### Weekly Availability and matching

- Replaced the availability inputs with a Monday–Friday calendar from **8 a.m. to 8 p.m. in UCI campus time**. Applicants choose the start and end of each unavailable range on ten-minute boundaries. The calendar supports keyboard selection and shows selected ranges and saved schedules.
- Applicants confirm that unmarked working hours are available. The server calculates availability within that weekday window and retains minute precision for older records. Previously submitted application snapshots retain their original evidence.
- The page reports empty, read-only, and failed loading states and distinguishes imported conflicts from confirmed availability.

### TA Placements and messaging

- Renamed the Applicant assignment view to **My TA Placement**. It shows published Section arrangements, weekly hours, and the online accept and decline actions. My Applications remains the place to track application status.
- Removed the reply deadline, countdown, and automatic expiry process. Migration `V15__optional_assignment_reply.sql` removes the database constraint requiring a deadline and preserves prior records. Previously expired placements remain visible as cancelled history where applicable.
- Displayed dated assignment events in the browser's local time and labeled recurring class meetings with their course time zone.
- Removed the Messages pages, sidebar entries, and Data Editor tab. The message API and stored history remain available in the backend. In-app issue submission is a future design discussion.

### Teacher and admin permissions

- Teachers operate on courses they own. Admin manages accounts, course ownership, application periods, access, and CSV. Review Workspace, Matching Criteria, Course Sections, and TA Assignments are globally readable for admin by default. The UI and API apply the same write restrictions.
- Another admin can delegate specific review, course configuration, or assignment permissions to an admin for selected courses in **Review Access**. The audit log records the grant and rationale. Direct TA selection requires both approval and assignment permissions. Delegated draft creation and publication require an operation reason; decisions and cancellations also retain their reasons.
- CSV remains admin-only. Student intake and exports are available to active admin accounts. Importing Sections requires an existing course with an assigned Teacher and a course configuration grant. Importing selection records requires the relevant course review grant. The Data Editor exposes courses, postings, applications, workload assignments, and audit logs as read-only collections. Course management uses Modules.

## Verification and rollout

- The latest application regression run passed **415 Java tests** excluding the PostgreSQL integration suite and **53 frontend tests**. Browser tests used synthetic API fixtures. Coverage includes account routing, forged signup roles, availability range selection, placement replies, default admin read access, scoped delegation, grant revocation, and CSV authorization.
- A separate earlier run documented in the [change log](XinyiXu-change-log.md) passed 23 isolated PostgreSQL integration tests for the placement migration. The latest combined changes have not been exercised against a running application database.
- Restart the backend when deploying this release so it loads the new code and applies V15. Existing applications, historical assignment replies, and submitted schedule snapshots remain available.
