> Messages pages and chat entry points are temporarily removed. Internal review notes and system notifications remain; backend messaging APIs and stored history are retained. Historical chat screenshots below do not represent an available feature.

> Placement workflow: **My TA Placement** shows course / Section arrangements and allows students to accept or decline. Publishing requires no reply deadline; pending placements remain reserved until a reply or an authorised cancellation. This is not daily teaching-task management.

> Update: Interview management has been removed. Older PDFs and screenshots may still show the retired workflow; see [current removal notes](docs/interview-retirement.md).

> Current access policy: the primary role labels are **Applicant**, **Teacher**, and **admin**. Teacher retains the internal `organiser` role key and routes. Reviewer, Decision-maker, and Viewer are explicitly advanced review roles; existing accounts and grants are preserved. Full CSV Import / Export, templates, previews, and the shared CSV API require an active **admin** account. Teacher has no CSV page or menu.

> Sponsor-aligned forms and CSV import/export are implemented. See [CSV workflow guide](SPONSOR_CSV_GUIDE_zh.md).

> For the current proposal workflow, permissions, and backup verification, see [handoff notes](PROPOSAL_HANDOFF_zh.md). CSV import/export is covered in the guide above.

> The local version now uses PostgreSQL. See [database setup, backup, and restore](DATABASE_zh.md). Local URL: `http://localhost:8080/tamo/`.

[English](User_Manual.md) | [简体中文](User_Manual_zh.md)

# TA&MO User Manual

<div align="center">
  <a href="http://47.93.90.208:8080/tamo" target="_blank">
    <img src="backend/src/main/webapp/static/images/image.png" alt="TA&MO Logo" width="150">
  </a>
  <br>
  <strong>🌍 <a href="http://47.93.90.208:8080/tamo" target="_blank">Official Website: http://47.93.90.208:8080/tamo</a></strong>
</div>
<br>

The TA&MO platform is a robust role-based system for managing Teaching Assistant applications. Use this documentation to understand core concepts, set up your account, and execute role-specific tasks.

---

## Table of Contents
- [Quickstart](#quickstart)
- [Applicant Guide](#applicant-guide)
- [Teacher Guide](#teacher-guide)
- [Admin Reference](#admin-reference)

---

## Quickstart

Get authenticated and set up your profile immediately.

### Log in to the platform
Navigate to `http://47.93.90.208:8080/tamo`. Enter your university email and password. Your account role is determined automatically; no role selection is needed.

![Login Page](screenshot/01_login_page.png)
**UI Breakdown:**
- 🔹 **Email & Password Fields**: Inputs for credentials.
- 🔹 **Interactive Security Animation**: The Rive character tracks cursor input and covers its eyes during password entry.

### Create a new account
If you do not have an account, register as a new applicant.

![Sign Up Page](screenshot/02_signup_page.png)
**UI Breakdown:**
- 🔹 **Student registration**: Self-registration creates an applicant account; staff accounts are created in the admin interface.
- 🔹 **Form Validation**: Real-time error highlighting for mismatched passwords or invalid emails.

---

## Applicant Guide

Instructions for browsing jobs, submitting applications, and communicating with Teachers.

### Navigate the Dashboard
The Applicant Dashboard provides a high-level summary of your application status.

![Applicant Dashboard](screenshot/03_applicant_dashboard.png)
**UI Breakdown:**
- 🔹 **Left Sidebar**: Primary navigation hub.
- 🔹 **Welcome Header**: Account overview and total active applications.
- 🔹 **Quick Stats Cards**: Aggregate status counts.

### View system notifications
Check real-time alerts regarding your applications.

![Dashboard Notifications](screenshot/11_applicant_dashboard_notifications.png)
**UI Breakdown:**
- 🔹 **Notifications Panel (Right)**: Displays alerts like "Assignment Published" or "Status Updated".

### Search for open positions
Browse available modules and filter by department.

![Find Jobs](screenshot/04_applicant_find_jobs.png)
**UI Breakdown:**
- 🔹 **Search & Filter Bar**: Filter by academic department.
- 🔹 **Job Cards**: High-level summary of module code, title, and required hours.

### Review job prerequisites
Click a job card to view detailed requirements.

![Job Details](screenshot/05_applicant_find_jobs_details.png)
**UI Breakdown:**
- 🔹 **Description Panel**: Full list of responsibilities and required skills.
- 🔹 **Apply Button**: Triggers the application workflow.

### Submit an application
Provide a cover letter to finalize your application submission.

![Apply Modal](screenshot/06_applicant_apply_modal.png)
**UI Breakdown:**
- 🔹 **Cover Letter Input**: Textarea for customized application intent.
- 🔹 **Submit Button**: Locks in current profile data and submits application.

### Track application status
Monitor progression through the application pipeline.

![My Applications](screenshot/07_applicant_my_applications.png)
**UI Breakdown:**
- 🔹 **Status Badges**: Color-coded indicators denoting pipeline stage (Review, Shortlisted, Offer, Rejected).
- 🔹 **Withdraw Action**: Button to retract an active application.

### Build your academic profile
Complete your profile to satisfy job prerequisites.

![Applicant Profile](screenshot/10_applicant_profile.png)
**UI Breakdown:**
- 🔹 **Academic Details**: Inputs for GPA, major, and degree level.
- 🔹 **Skills Tags**: Selectable skills relevant to technical modules.
- 🔹 **Document Upload**: Secure attachment of CVs and transcripts.

### Message module Teachers
Initiate or respond to chat threads with professors.

![Messages Empty State](screenshot/08_applicant_messages_empty.png)
**UI Breakdown:**
- 🔹 **Contact List (Left)**: Permitted contacts based on active applications.
- 🔹 **Empty State**: Default view when no conversation is active.

![Messages Chat](screenshot/09_applicant_messages_chat.png)
**UI Breakdown:**
- 🔹 **Chat History**: Chronological message feed.
- 🔹 **Message Input**: Textarea for drafting inquiries.

---

## Teacher Guide

Instructions for managing job postings, reviewing candidates, and recording assignments.

### View module overview
Access aggregated metrics for your assigned modules.

![Organiser Overview](screenshot/13_organiser_overview.png)
**UI Breakdown:**
- 🔹 **Module Capacity Cards**: Ratio of hired TAs vs. required TAs.
- 🔹 **Recent Activity**: Timeline of incoming applications.

### Manage TA postings
Publish, edit, or toggle visibility of module listings.

![TA Positions](screenshot/14_organiser_ta_positions.png)
**UI Breakdown:**
- 🔹 **Create New Job**: Button to initialize a new job posting.
- 🔹 **Job List Table**: Actionable rows for toggling visibility or editing details.

### Progress candidates through the pipeline
Use the Kanban board to manage application stages.

![Pipeline Workflow](screenshot/15_organiser_pipeline.png)
**UI Breakdown:**
- 🔹 **Pipeline Columns**: Distinct stages (Applied, Review, Shortlisted, Offer, Rejected).
- 🔹 **Drag-and-Drop Interface**: Click and drag candidate cards across columns to update their status.
- 🔹 **Automated Notifications**: System dispatches alerts automatically upon status change.

### Message applicants
Communicate directly with candidates.

![Organiser Messages](screenshot/12_organiser_messages.png)
**UI Breakdown:**
- 🔹 **Applicant Roster**: List of candidates grouped by module.
- 🔹 **Chat Interface**: Rich text area for sending feedback.

---

## Admin Reference

Tools for platform auditing, workload intelligence, and global configuration.

> [!CAUTION]
> Admin tools directly modify global state and persistent data. Ensure configurations align with university policy before saving.

### Review platform analytics
Monitor aggregate system health.

![Admin Analytics](screenshot/19_admin_analytics.png)
**UI Breakdown:**
- 🔹 **Global Metrics**: Total users, applications, and conversion rates.
- 🔹 **Department Charts**: Visual distribution of TA demand.

### Balance TA workloads via AI
Utilize Workload Intelligence to ensure compliance with maximum working hours.

![Workload Intelligence](screenshot/20_admin_workload_intel.png)
**UI Breakdown:**
- 🔹 **AI Insights Panel**: Automatically flags students exceeding hourly limits.
- 🔹 **Reassignment Tools**: Admin override to transfer offers between candidates.

### Manage global modules
Register new curriculum modules and assign Teachers.

![Global Modules](screenshot/21_admin_global_modules.png)
**UI Breakdown:**
- 🔹 **Module Registry**: Table of all system-recognized courses.
- 🔹 **Teacher Assignment**: Dropdown to map professors to specific modules.

### Configure system properties
Set core platform variables.

![Properties Settings](screenshot/22_admin_properties.png)
**UI Breakdown:**
- 🔹 **Term Configuration**: Input for current academic semester.
- 🔹 **Department List**: Dynamic CRUD interface for university departments.

### Manage user accounts
Oversee registration and role assignments.

![User Management](screenshot/23_admin_users.png)
**UI Breakdown:**
- 🔹 **Role Assignment**: Upgrade standard accounts to Teacher or Admin.
- 🔹 **Account Actions**: Suspend or delete malicious users.

### Edit persistent data directly
Bypass business logic to manage PostgreSQL records through the existing collection API; database constraints still apply.

![Data Editor](screenshot/24_admin_data_editor.png)
**UI Breakdown:**
- 🔹 **JSON CRUD Interface**: Tabular view of underlying database collections.
- 🔹 **Action Buttons**: Direct edit/delete capabilities for raw entities.

### Audit system logs
Review secure logs for platform activity.

![System Logs](screenshot/25_admin_system_logs.png)
**UI Breakdown:**
- 🔹 **Log Table**: Chronological record of actions, user IDs, and timestamps.
- 🔹 **Severity Filters**: Toggle between INFO, WARNING, and ERROR logs.
