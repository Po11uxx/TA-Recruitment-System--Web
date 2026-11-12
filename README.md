> Messages UI is temporarily removed. Internal review notes and system notifications remain available; backend messaging APIs and stored history are retained.

> Placement workflow: **My TA Placement** shows course / Section arrangements and allows students to accept or decline. Publishing requires no reply deadline; pending placements remain reserved until a reply or an authorised cancellation. This is not daily teaching-task management.

> Interview management has been removed. See [removal and compatibility notes](docs/interview-retirement.md); historical Version 1 documents and PDFs may describe the former workflow.

> Current access policy: the primary role labels are **Applicant**, **Teacher**, and **admin**. Teacher retains the internal `organiser` role key and routes. Reviewer, Decision-maker, and Viewer are explicitly advanced review roles; existing accounts and grants are preserved. Full CSV Import / Export, templates, previews, and the shared CSV API require an active **admin** account. Teacher has no CSV page or menu.

> Current baseline: **Version 1 (v1.0)**. See the [Version 1 release notes (Chinese)](VERSION_1_zh.md) for implemented changes, validation and limitations.

> Sponsor-aligned forms and CSV import/export are implemented. See [CSV workflow guide](SPONSOR_CSV_GUIDE_zh.md).

> For the current proposal workflow, permissions, and backup verification, see [handoff notes](PROPOSAL_HANDOFF_zh.md). CSV import/export is covered in the guide above.

> Application period management is available under **Application Periods** in the admin sidebar. See [workflow and API instructions](APPLICATION_PERIODS_zh.md).

[English](README.md)

# TA&MO — TA Application & Management System

<div align="center">
  <a href="http://47.93.90.208:8080/tamo" target="_blank">
    <img src="backend/src/main/webapp/static/images/image.png" alt="TA&MO Logo" width="150">
  </a>
  <br>
  <strong>🌍 <a href="http://47.93.90.208:8080/tamo" target="_blank">Official Website: http://47.93.90.208:8080/tamo</a></strong>
</div>
<br>

> University-oriented Teaching Assistant application and management platform built with **Java Servlet/JSP** + **Embedded Tomcat**.

> [!NOTE]
> **📖 Looking for the User Manual?**
> A comprehensive, illustrated user guide covering all 25 interface features is available in this repository.
> - 📄 [Read it online in Markdown](User_Manual.md)
> - 📥 [Download the High-Res PDF Version](User_Manual.pdf)

---

## Table of Contents

- [Project Overview](#project-overview)
- [System Interface Showcase](#system-interface-showcase)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Getting Started (Setup & Configuration)](#getting-started-setup--configuration)
- [Running the Software](#running-the-software)
- [Running Tests](#running-tests)
- [Generating Javadoc](#generating-javadoc)
- [Project Structure](#project-structure)
- [User Roles & Test Accounts](#user-roles--test-accounts)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Project Overview

TA&MO is a full-stack web application designed for managing the Teaching Assistant application lifecycle at a university. It provides a robust, role-based platform that streamlines the hiring process from start to finish.

| Role | Key Features |
|------|-------------|
| **Applicant (TA)** | Browse & apply for TA positions, track application status, manage profile & CV documents, chat system |
| **Teacher (Professor)** | Manage TA job postings, review applications via Kanban pipeline, record approved Section assignments |
| **Admin** | Analytics dashboard, user & module management, workload monitoring, system logs, data editor, configuration |

---

## Feature Walkthrough & Usage Guide

TA&MO offers distinct interfaces and features based on your logged-in role. Below is a guide on how to navigate and utilize the primary functions of the system, complete with hidden visual references you can expand to see the UI.

### 1. Authentication
To get started, users must either log in to an existing account or sign up.
- **Login**: Navigate to `/auth/login.jsp`. Enter your email and password. The system uses an interactive animated character that reacts to your input fields.
- **Sign Up**: Navigate to `/auth/signup.jsp` to create a new account as an Applicant.

<details>
<summary>Click to view Authentication UI</summary>
<br>

- **Login Page**
  <img src="screenshot/01_login_page.png" alt="Login Page" width="800">
- **Sign Up Page**
  <img src="screenshot/02_signup_page.png" alt="Sign Up Page" width="800">
</details>

### 2. Using the System as an Applicant
Once logged in as an Applicant (TA), you have access to a suite of tools to find and secure jobs.
- **Dashboard & Notifications**: Your landing page gives you a bird's-eye view of your active applications and recent notifications.
- **Finding & Applying for Jobs**: Go to the "Find Jobs" tab to browse open module positions. Click on a job to view its details, and use the Apply modal to submit your application and cover letter.
- **Tracking Applications**: Use the "My Applications" section to track your progress through the application pipeline.
- **Profile Management**: Update your academic details, skills, and CVs under the "Profile" tab to improve your chances.

<details>
<summary>Click to view Applicant UI</summary>
<br>

- **Dashboard**
  <img src="screenshot/03_applicant_dashboard.png" alt="Applicant Dashboard" width="800">
- **Find Jobs**
  <img src="screenshot/04_applicant_find_jobs.png" alt="Find Jobs" width="800">
- **Job Details**
  <img src="screenshot/05_applicant_find_jobs_details.png" alt="Job Details" width="800">
- **Apply for Job**
  <img src="screenshot/06_applicant_apply_modal.png" alt="Apply Modal" width="800">
- **My Applications & Tracker**
  <img src="screenshot/07_applicant_my_applications.png" alt="My Applications" width="800">
- **Profile**
  <img src="screenshot/10_applicant_profile.png" alt="Applicant Profile" width="800">
- **Notifications**
  <img src="screenshot/11_applicant_dashboard_notifications.png" alt="Dashboard Notifications" width="800">
</details>

### 3. Using the System as an Teacher
Teachers (Professors) can manage their job postings and review candidates.
- **Managing Postings**: Use the "TA Positions" tab to create, edit, or delete job postings for your modules.
- **Application Pipeline**: The Kanban-style pipeline allows you to drag-and-drop applicants between stages (Review, Shortlisted, Offer, Rejected).

<details>
<summary>Click to view Teacher UI</summary>
<br>

- **Teacher Overview**
  <img src="screenshot/13_organiser_overview.png" alt="Organiser Overview" width="800">
- **TA Positions**
  <img src="screenshot/14_organiser_ta_positions.png" alt="TA Positions" width="800">
- **Application Pipeline (Kanban)**
  <img src="screenshot/15_organiser_pipeline.png" alt="Application Pipeline" width="800">
</details>

### 4. Using the System as an Admin
System admins maintain the platform and oversee all activities.
- **Analytics & Workload**: Monitor platform usage on the Analytics dashboard. The AI-assisted Workload Intelligence tab helps balance TA hours across departments.
- **Platform Management**: Use "Global Modules", "Properties", and "User Management" to configure system-wide settings, departments, terms, and roles.
- **Data & Logs**: The "Data Editor" provides raw CRUD access to the underlying storage collections, while "System Logs" tracks every action taken on the platform for auditing.

<details>
<summary>Click to view Admin UI</summary>
<br>

- **Platform Analytics**
  <img src="screenshot/19_admin_analytics.png" alt="Admin Analytics" width="800">
- **Workload Intelligence (AI Assisted)**
  <img src="screenshot/20_admin_workload_intel.png" alt="Workload Intel" width="800">
- **Global Modules**
  <img src="screenshot/21_admin_global_modules.png" alt="Global Modules" width="800">
- **Properties & Settings**
  <img src="screenshot/22_admin_properties.png" alt="Properties" width="800">
- **User Management**
  <img src="screenshot/23_admin_users.png" alt="Users" width="800">
- **Data Editor (CRUD)**
  <img src="screenshot/24_admin_data_editor.png" alt="Data Editor" width="800">
- **System Logs**
  <img src="screenshot/25_admin_system_logs.png" alt="System Logs" width="800">
</details>

---

## Architecture & Tech Stack

```
┌───────────────────────────────────────────────────┐
│              Embedded Tomcat (port 8080)          │
│                                                   │
│  ┌───────────────┐     ┌───────────────────────┐  │
│  │  JSP Frontend │     │  Servlet Backend API  │  │
│  │  (18+ pages)  │────▶│  (40+ REST endpoints) │  │
│  │  Tailwind CSS │     │  Session Auth + RBAC  │  │
│  │  Vanilla JS   │     │  PostgreSQL storage    │  │
│  └───────────────┘     └───────────────────────┘  │
│                              │                    │
│                        ┌─────▼─────┐              │
│                        │ PostgreSQL│              │
│                        └───────────┘              │
└───────────────────────────────────────────────────┘
```

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| Build Tool | Apache Maven | 3.6+ |
| Web Server | Embedded Apache Tomcat | 10.1.19 |
| Servlet API | Jakarta Servlet | 6.0.0 |
| JSP API | Jakarta JSP | 3.1.0 |
| JSON Parsing | Gson | 2.10.1 |
| Testing | JUnit | 5.x |
| Styling & UI | Tailwind CSS, Lucide Icons, Chart.js, Rive | Latest (via CDN) |

---

## Getting Started (Setup & Configuration)

### Prerequisites

Before setting up the project, ensure you have the following installed on your system:

1. **Java Development Kit (JDK) 17+**
   - Verify installation: `java -version` and `javac -version`
2. **Apache Maven 3.6+**
   - Verify installation: `mvn -version`
3. **Docker Desktop / Docker Engine + Compose** for local PostgreSQL.
4. **Active Internet Connection**
   - Required to fetch CDN resources (Tailwind CSS, Icons, JS libraries).

### 1. Clone the Repository

```bash
git clone https://github.com/Elrond-JJ/Elrond-JJ-EBU6304-TA-Recruitment-System-Group100.git
cd Elrond-JJ-EBU6304-TA-Recruitment-System-Group100
git checkout dev
```

### 2. Configuration Settings

The system is configured out of the box to run on your local machine, but the production version is hosted remotely. You can modify the following default behaviors if needed:

- **Server Port**: The embedded Tomcat server defaults to **Port 8080**. If this port is occupied, you can change it by editing `tomcat.setPort(8080);` in `backend/src/main/java/com/tamo/Main.java`.
- **Database**: The application uses local PostgreSQL 17. Existing `backend/src/main/resources/data.json` is imported once on first startup and preserved as a backup. See [database setup and backups](DATABASE_zh.md).
- **Context Path**: The application is served under the `/tamo` context path.

---

## Running the Software

We use a Maven plugin to compile and boot the Embedded Tomcat server in a single command.

### Start the Application

Start Docker Desktop, then run `docker compose up -d --wait db` from the repository root before the steps below. On macOS/Linux, `./run.sh` from the root starts both the database and application.

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Run the application:
   ```bash
   mvn compile exec:java
   ```
   *This command will download Maven dependencies, compile Java source files, and start the Tomcat server on port 8080.*

### Access the System

Once the server has successfully started (you will see the "TA&MO Backend Started" message in the terminal), open your browser and navigate to:

- **Landing Page**: [http://localhost:8080/tamo](http://localhost:8080/tamo)
- **Login Page**: [http://localhost:8080/tamo/auth/login.jsp](http://localhost:8080/tamo/auth/login.jsp)
- **Registration**: [http://localhost:8080/tamo/auth/signup.jsp](http://localhost:8080/tamo/auth/signup.jsp)

*To stop the server, simply press `Ctrl + C` in the terminal.*

---

## Running Tests

The project includes an extensive suite of automated tests (200+ unit test cases) built with **JUnit 5**. These tests validate the integrity of our core service logic, models, authentication filters, and utility classes.

### How to Execute Tests

Navigate to the `backend` directory and run the standard Maven test command:

```bash
cd backend
mvn test
```

Maven will execute all tests automatically via the Maven Surefire Plugin. You should see a `BUILD SUCCESS` message along with the summary of tests run, failures, and skipped tests.

---

## Generating Javadoc

The entire Java codebase is fully documented with comprehensive Javadoc comments detailing classes, attributes, methods, parameters, and return types.

### How to Generate and View Javadocs

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Generate the documentation using the Maven Javadoc plugin:
   ```bash
   mvn javadoc:javadoc
   ```
3. Open the generated API documentation in your browser. If you are on macOS, you can use the following command:
   ```bash
   open target/site/apidocs/index.html
   ```
   *Alternatively, manually navigate to `backend/target/site/apidocs/index.html` and open it with Chrome/Safari/Edge.*

---

## Project Structure

```
TA&MO_v1.0/
├── backend/                           # Main application
│   ├── pom.xml                        # Maven project config
│   ├── TEST_DOCUMENT.md               # API test documentation
│   │
│   ├── src/main/java/com/tamo/        # Java Source Code
│   │   ├── Main.java                  # Entry point — embedded Tomcat bootstrap
│   │   ├── filter/                    # Security & CORS filters
│   │   ├── listener/                  # Servlet Context Listeners
│   │   ├── model/                     # Data Entities (POJOs)
│   │   ├── service/                   # Core Business Logic Services
│   │   ├── servlet/                   # RESTful API Controllers
│   │   └── util/                      # Helper Classes
│   │
│   ├── src/main/resources/
│   │   └── data.json                  # One-time import seed (PostgreSQL is authoritative)
│   │
│   ├── src/main/webapp/               # Frontend Files
│   │   ├── WEB-INF/web.xml            # Servlet configurations
│   │   ├── index.jsp                  # Landing page
│   │   ├── auth/, applicant/, admin/, organiser/ # Role-based JSP Pages
│   │   ├── common/                    # Shared UI Fragments (Sidebar, Head)
│   │   └── static/                    # CSS, JS APIs, Rive assets
│   │
│   └── src/test/java/com/tamo/        # JUnit Test Cases
│
├── screenshot/                        # System Interface Screenshots
└── README.md                          # Documentation
```

---

## User Roles & Test Accounts

After the initial import, use the demo accounts below. If an account has been changed, use its current database credentials.

| Role | Name | Email | Password |
|------|------|-------|----------|
| Applicant | Alex Johnson | `alex.j@university.edu` | `demo123` |
| Applicant | Bob Smith | `bob.s@university.edu` | `demo123` |
| Organiser | Alan Turing | `alan@university.edu` | `demo123` |
| Organiser | Grace Hopper | `grace@university.edu` | `demo123` |
| Admin | System Admin | `admin@university.edu` | `admin123` |

---

## API Reference

The backend exposes **42 RESTful API endpoints** under the `/tamo/api/` prefix. All protected endpoints utilize HTTP Session-based authentication enforced via the `AuthFilter`.

- `/api/auth/*` : Registration, Login, Session validation.
- `/api/applicant/*` : Job browsing, profile management, and application submissions.
- `/api/organiser/*` : Job publishing, application review and approved Section assignments.
- `/api/admin/*` : System analytics, user roles, terminology config, system logs, and workload AI assignments.

*For detailed request/response schemas, please see `backend/TEST_DOCUMENT.md`.*

---

## Troubleshooting

### Port 8080 already in use
If another application is using port 8080, kill the process before running:
```bash
lsof -ti :8080 | xargs kill -9
```

### Pages load without CSS or Icons
The frontend relies on CDN links for Tailwind CSS and Lucide icons. Verify your internet connection.

### Missing Data Changes
PostgreSQL persists changes in a Docker volume. Editing or restoring `data.json` does not reset an initialized database. See [database backup and restore instructions](DATABASE_zh.md).

---

## License

This project is developed by **Group 100** for the **EBU6304 Software Engineering** coursework. See the `LICENSE` file for distribution terms.
