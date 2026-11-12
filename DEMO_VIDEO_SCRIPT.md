# TA&MO Demo Video Script (≤ 10 minutes)

> Target length: **9 min 30 sec – 10 min 00 sec**
> Speaking pace: ~140 WPM (slow & clear for marking).
> Total narration words: ~750 (short, simple English for voice-over).
> Recording tip: 提前在浏览器登好 3 个标签页（Applicant / Organiser / Admin），录制时直接切换 tab，避免每次重新登录浪费时间。
> Recording tip: 在录前用 admin 把 `data.json` 备份一份；演示完误删等错误处理后可以恢复。

---

## Pre-Recording Checklist

| # | 检查项 | 说明 |
|---|--------|------|
| 1 | `mvn compile exec:java` 已启动，控制台显示 `TA&MO 后端已启动: http://localhost:8080/tamo` | 服务必须先跑起来 |
| 2 | 浏览器窗口最大化，缩放 100%，关闭所有其他 tab、扩展通知 | 画面干净 |
| 3 | 麦克风测试：环境安静，无回声 | 录音必须有人声讲解 |
| 4 | 在 3 个浏览器窗口分别预登录：`alex.j@university.edu` / `alan@university.edu` / `admin@university.edu`（密码：applicant/organiser 全部用 `demo123`，admin 用 `admin123`），但**录视频时仍从 login 页演示一次登录** | 节省切换时间 |
| 5 | 准备好一个 PDF 文件（任意 < 5 MB）用于上传 CV 演示 | 文件可重命名为 `Demo_CV.pdf` |
| 6 | 全程用 **OBS / QuickTime** 录屏 + 系统麦克风 | 满足 "voice must be recorded" |

---

## Scene 1 — Opening Hook (0:00 – 0:25)

**画面操作**：停留在登录页 `http://localhost:8080/tamo/auth/login.jsp`，背景动画在播放。

**Narration (English):**
> "Hello. This is **TA&MO**, our TA application system for EBU 6304. It supports three roles: applicants, organisers, and admins. The app uses Java, JSP pages, and REST APIs. Let me show you how it works."

---

## Scene 2 — Landing Page & Login (0:25 – 1:10)

**画面操作**：
1. 打开 `http://localhost:8080/tamo/index.jsp` — 滚动一下展示落地页。(5s)
2. 点 "Log In" 进入登录页。
3. **故意先输入错误密码** `wrong@xx.com / 12345`，点 Log In → 出现红色错误提示。(8s)
4. 改回正确账号 `alex.j@university.edu / demo123`，登录成功。

**Narration (English):**
> "This is the landing page. I click **Log In**. My account role is determined automatically after authentication. First I use a wrong password. The page shows a clear error — it does not crash. Now I log in as **Alex Johnson**, an applicant. The system opens the applicant dashboard."

---

## Scene 3 — Applicant Role (1:10 – 3:30)

### 3.1 Dashboard (1:10 – 1:30)

**画面操作**：停留 5s 展示统计卡片（Total Apps / Pending / Profile Completion）、最近申请、推荐岗位。

**Narration:**
> "This is Alex's dashboard. He can see his application count, pending items, profile progress, and suggested jobs."

### 3.2 Browse & Search Jobs (1:30 – 2:00)

**画面操作**：
1. 左侧菜单点 **Jobs**。
2. 在搜索框输入 `data` —— 列表实时筛选（debounce 300ms）。
3. 用 status filter 切换 *Active* / *All*。
4. 点击其中一张 job 卡片 → 进入 Job Detail 页。

**Narration:**
> "On **Jobs**, Alex can search and filter by status. I open a Data Structures job to see the details and deadline."

### 3.3 Apply (with error) (2:00 – 2:30)

**画面操作**：
1. 在 Job Detail 点 **Apply Now** → 弹窗确认 → 提交成功 toast。
2. 立即再点一次 Apply → **出现 "You have already applied" 错误提示**（错误处理）。

**Narration:**
> "Alex applies with one click. If he applies again to the same job, the system blocks it and shows a message. This stops duplicate applications."

### 3.4 My Applications & Status Tracking (2:30 – 2:50)

**画面操作**：
1. 菜单点 **My Applications** → 展示列表（带状态徽章：applied / review / shortlisted / interview / offer / rejected）。
2. 点其中一个进入详情，展示 timeline。

**Narration:**
> "In **My Applications**, each status has a badge. Alex opens one item to see the timeline step by step."

### 3.5 Profile & CV Upload (2:50 – 3:20)

**画面操作**：
1. 菜单 **Profile**。
2. 编辑 GPA / Skills 字段，点 Save → toast 成功。
3. 在 Documents 区块点 **Upload** → 选择本地 `Demo_CV.pdf` → 上传成功，列表新增一行。
4. 点新文档旁边的删除按钮，确认弹窗 → 删除。

**Narration:**
> "On **Profile**, Alex updates his GPA and skills, then uploads a CV. He can delete a file after a confirmation step."

### 3.6 Messages (3:20 – 3:30)

**画面操作**：菜单 **Messages**，展示与 organiser 的对话记录。(快速展示 8 秒)

**Narration:**
> "**Messages** lets Alex talk to organisers in the app. Next I switch to the organiser role."

---

## Scene 4 — Organiser Role (3:30 – 6:15)

**画面操作**：右上角 logout（或直接打开预登录的 Organiser 浏览器窗口）。
切换登录账号 `alan@university.edu / demo123`，role = Organiser。

### 4.1 Dashboard (3:30 – 3:45)

**画面操作**：展示数据卡片（My Open Jobs / Pending Reviews / Upcoming Interviews）。

**Narration:**
> "I log in as **Dr. Alan Turing**. His dashboard shows open jobs, pending reviews, and upcoming interviews."

### 4.2 Create Job — with validation error (3:45 – 4:25)

**画面操作**：
1. 菜单 **Manage Jobs** → 点 **+ New Job**。
2. **故意只填部分字段**（比如不填 module）→ 点 Save → 出现红色字段提示 "Module is required"。(错误处理)
3. 补全所有字段：Title=`TA - Linear Algebra`, Module 选 `MATH201`, Department=`Mathematics`, Term=`Fall 2026`, Weekly Hours=`6`, Positions=`2`, Description 填一段简介（"Support MATH201 tutorials and mark weekly problem sets."），Deadline 选未来日期（比如 `2026-12-30`） → Save。
4. 列表里出现新岗位。

**Narration:**
> "On **Manage Jobs**, Alan creates a new posting. If a required field is empty, the form shows an error. I fill all fields and save. The new job appears in the list."

### 4.3 Application Pipeline (Kanban) (4:25 – 5:25)

**画面操作**：
1. 菜单 **Pipeline** → 展示六列 Kanban：Applied / Review / Shortlisted / Interview / Offer / Rejected。
2. 用鼠标把某个 Applied 状态的卡片拖到 **Review** 列 → toast 成功。
3. 把另一张卡拖到 **Interview** 列 → 弹出 *Schedule Interview* 模态框：填写日期、时间、地点 → 保存。
4. **故意把一张已经在 Offer 列的卡拖回 Applied** → 出现 toast "Cannot move candidate backwards once they are in a later stage."（错误处理）

**Narration:**
> "The **Pipeline** is a Kanban board with six stages. I drag cards to move candidates. Moving to **Interview** opens a scheduling window. I also try to drag backwards — the system blocks it and shows a warning."

### 4.4 Candidate Detail & Accept / Reject (5:25 – 5:50)

**画面操作**：
1. 点击 Pipeline 中某张卡片 → 滑出右侧详情面板，展示 CV、GPA、coursework、申请理由。
2. 点 **Accept** 或 **Reject** 按钮 → 状态更新。

**Narration:**
> "Clicking a card shows CV, GPA, and motivation. Alan can accept or reject from here."

### 4.5 Interviews List (5:50 – 6:15)

**画面操作**：
1. 菜单 **Interviews** → 列表显示已安排面试。
2. 编辑其中一条面试时间 → 保存。

**Narration:**
> "The **Interviews** page lists all meetings. Alan can edit the time. Now I switch to the admin role."

---

## Scene 5 — Admin Role (6:15 – 9:15)

**画面操作**：登出，登录 `admin@university.edu / admin123`，role = Admin。

### 5.1 Analytics Dashboard (6:15 – 6:50)

**画面操作**：展示统计卡 + 两张 Chart.js 图表（Application Trend area chart, Status Breakdown bar chart）+ 系统状态告警条。

**Narration:**
> "The admin dashboard shows totals, charts, and system status. Admins get a full view of the platform."

### 5.2 User Management (6:50 – 7:30)

**画面操作**：
1. 菜单 **Users** → 列表展示所有 10 个用户。指给观众看 Edward Park 已经是 *suspended* 状态。
2. 用 role filter 切到 *Organiser* → 列表过滤为 3 个 organiser。再切回 *All*。
3. 点 **+ Add User** → 填写：Name=`Dr. Ada Lovelace`, Email=`ada@university.edu`, Password=`demo123`, Role=`Organiser`, Department=`Computer Science` → Save。新用户出现在列表。
4. 点新用户的编辑按钮 → 修改 status 为 *suspended* → 保存。
5. 点删除按钮 → **弹出二次确认 modal**（错误处理：防误删） → 取消。

**Narration:**
> "In **Users**, the admin can search, filter, add, edit, or suspend accounts. Delete needs a second confirmation, so mistakes are harder."

### 5.3 Module Management (7:30 – 7:50)

**画面操作**：
1. 菜单 **Modules** → 列表 + keyword search。
2. 新增一个 module：code=`EBU7000`, name=`AI for Education`, dept=`CS` → 保存。
3. 编辑或删除一个旧 module（演示编辑即可）。

**Narration:**
> "**Modules** stores course codes. The admin can add or edit a module. Organisers see the updates when they create jobs."

### 5.4 Properties — Terms & Departments (7:50 – 8:10)

**画面操作**：
1. 菜单 **Properties** → 两个 tab: Terms / Departments。
2. Terms tab 添加 term `Fall 2027` (startDate=`2027-09-15`, endDate=`2027-12-20`)。
3. Departments tab 添加 department `Materials Science`。

**Narration:**
> "**Properties** manages terms and departments in one place."

### 5.5 Workload Management (8:10 – 8:40)

**画面操作**：
1. 菜单 **Workload** → 表格显示每个 TA 的 current/max weekly hours 与 utilisation bar。**Alex Johnson 22/20 (Overloaded)** 应显示红色，名下有 3 个 confirmed offers：CS301 (8h)、CS101 (6h)、EBU6304 (8h)。Diana Chen 若无 offer 则可能不在列表中。
2. 点 Alex 行的 **Reassign** → 用 `transfer` 或 `reopen` 把其中一个 offer（如 CS101 6h）转给 Diana 或撤销 → 保存后总工时降到 20 以下，状态从 Overloaded 变回 Optimal。

**Narration:**
> "**Workload** shows hours per TA. Alex has 22 hours but the limit is 20, so he is **overloaded** — shown in red. The admin can use **Reassign** to move or cancel one offer and fix the balance."

### 5.6 System Logs (8:40 – 9:00)

**画面操作**：
1. 菜单 **Logs** → 显示按时间倒序的操作日志。
2. 用 level filter 选 `ERROR` / `INFO` / `WARN`，再用关键字搜索某用户名。

**Narration:**
> "**Logs** record logins, applications, and status changes. The admin can filter by type or search by user."

### 5.7 Messages & Data Editor (9:00 – 9:15)

**画面操作**：
1. 菜单 **Messages** —— 快速展示。
2. 菜单 **Data Editor** —— 显示原始 JSON 编辑界面（如时间允许，演示读、不修改）。

**Narration:**
> "Admins also have **Messages** and a **Data Editor** to view the JSON data."

---

## Scene 6 — Architecture Recap & Closing (9:15 – 9:55)

**画面操作**：切到 README 文件或 IDE，简短停留在架构图 / 项目结构上 5–8 秒；最后回到登录页。

**Narration:**
> "The system uses JSP for the UI, Java Servlets for the API, and JSON for storage. Thank you for watching. This is **TA&MO** from Group 100, EBU 6304 Software Engineering."

---

## Error-Handling Coverage Summary (for marking scheme)

| # | Error case demonstrated | Scene |
|---|--------------------------|-------|
| 1 | Wrong password → red error banner + UI feedback | 2 |
| 2 | Duplicate job application blocked by server | 3.3 |
| 3 | Required-field validation on Create Job form | 4.2 |
| 4 | Pipeline drag-backwards prevention with toast | 4.3 |
| 5 | Delete-user double-confirmation modal | 5.2 |
| 6 | (Optional) suspended user cannot log in (mention only) | 5.2 |

---

## Timing Cheat Sheet

| Scene | Section | Start | End | Duration |
|-------|---------|-------|-----|----------|
| 1 | Opening | 0:00 | 0:25 | 0:25 |
| 2 | Landing + Login (with error) | 0:25 | 1:10 | 0:45 |
| 3 | Applicant — full flow | 1:10 | 3:30 | 2:20 |
| 4 | Organiser — full flow | 3:30 | 6:15 | 2:45 |
| 5 | Admin — full flow | 6:15 | 9:15 | 3:00 |
| 6 | Recap & closing | 9:15 | 9:55 | 0:40 |
| **Total** | | | | **≈ 9:55** |

> Buffer of 5 seconds is intentional — leaves room for cursor pauses without exceeding 10 minutes.

---

## Recording Workflow Tips

1. **Rehearse once** end-to-end with the script open on a second monitor or printed sheet — aim to finish at 9:30–9:50.
2. **Don't start narration until the page has loaded** — silent wait kills pacing; either pre-load or cut in editing.
3. If you're not confident speaking live, record **screen first**, then record **voice-over separately** and align in OBS / Premiere / iMovie.
4. **Reset demo data between rehearsals**: before recording, create a baseline snapshot once with `cp backend/src/main/resources/data.json backend/src/main/resources/data.demo.bak`. Between takes, restore it with `cp backend/src/main/resources/data.demo.bak backend/src/main/resources/data.json` so the pipeline / dashboards always look the same on camera. If you want to revert to the **original** dataset (pre-demo cleanup), use `cp backend/src/main/resources/data.json.original.bak backend/src/main/resources/data.json`.
5. Export at **1080p, ~30 fps, MP4 (H.264)** to keep file size reasonable for QM+ upload.
