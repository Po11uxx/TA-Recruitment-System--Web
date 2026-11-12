> Messages 页面和聊天入口已暂时移除；审核备注与系统通知保留，后端消息 API 和历史数据未删除。下方历史聊天截图不代表当前可用功能。

> 更新：面试功能已移除，旧版 PDF 与截图可能仍包含旧流程。以当前页面及 [移除说明](docs/interview-retirement.md) 为准。

> 当前角色显示名称为 **Applicant、Teacher、admin**；Teacher 保留内部 `organiser` 键和路由。Reviewer、Decision-maker、Viewer 明确属于高级审核角色，不迁移或合并已有账号。完整 CSV 页面、模板、预览、导入导出及共享 API 仅限有效的 **admin** 账号；Teacher 无 CSV 入口。

> 当前版本：**Version 1（v1.0）**。本版本的完整改动说明见 [Version 1 版本说明](VERSION_1_zh.md)。

> Sponsor 新版字段、技能多选与 CSV 已实现，使用步骤见 [Sponsor 表单与 CSV 说明](SPONSOR_CSV_GUIDE_zh.md)。

> 当前 Proposal 流程与角色权限，以 [功能交接与验收](PROPOSAL_HANDOFF_zh.md) 为准（CSV 见上方新说明）。

> 分配确认流程已简化：保留学生接受 / 拒绝，取消截止时间、倒计时和自动过期。见 [当前分配确认说明](REPLY_DEADLINES_zh.md)。

> 新功能：[录用安排发布、学生接受/拒绝和站内通知](OFFER_RESPONSES_zh.md)。

> 新功能：[TA 正式分配与 My Assignments](TA_ASSIGNMENTS_zh.md)。

> 新功能：[Section 时间匹配与筛选](SECTION_MATCHING_zh.md)（Organiser 审核申请）。

> 新增：管理员和 Organiser 可在 **Course Sections** 维护上课时段及 TA 需求。详见 [课程 Section 说明](COURSE_SECTIONS_zh.md)。

> 新增：学生可在 **Weekly Availability** 按申请周期填写时间安排，提交申请时保存快照。详见 [每周可用时间说明](WEEKLY_AVAILABILITY_zh.md)。

> 新增：管理员可在 **Application Periods** 管理申请周期。操作步骤和规则见 [申请周期管理说明](APPLICATION_PERIODS_zh.md)。

> 本地版本已迁移至 PostgreSQL。启动、数据备份与恢复见 [数据库使用说明](DATABASE_zh.md)。本地访问地址为 `http://localhost:8080/tamo/`。

[English](User_Manual.md) | [简体中文](User_Manual_zh.md)

# TA&MO 用户手册

<div align="center">
  <a href="http://47.93.90.208:8080/tamo" target="_blank">
    <img src="backend/src/main/webapp/static/images/image.png" alt="TA&MO Logo" width="150">
  </a>
  <br>
  <strong>🌍 <a href="http://47.93.90.208:8080/tamo" target="_blank">官方网站: http://47.93.90.208:8080/tamo</a></strong>
</div>
<br>

TA&MO 平台是一个强大的、基于角色的助教招聘与管理系统。通过本文档，你将了解系统的核心概念、完成账号设置，并掌握如何执行各个角色的专属任务。

---

## 目录
- [快速入门](#快速入门)
- [申请人指南](#申请人指南)
- [组织者指南](#组织者指南)
- [管理员参考](#管理员参考)

---

## 快速入门

完成身份验证并立即设置你的个人资料。

### 登录平台
在浏览器中访问 `http://47.93.90.208:8080/tamo`。输入你的大学邮箱和密码。

![登录页面](screenshot/01_login_page.png)
**UI 功能分解:**
- 🔹 **邮箱与密码输入框**: 用于输入身份凭证。
- 🔹 **交互式安全动画**: Rive 动画角色会实时跟踪鼠标的移动，并在输入密码时捂住眼睛以提供额外的安全感。

### 注册新账号
如果你还没有账号，请注册成为一名新申请人。

![注册页面](screenshot/02_signup_page.png)
**UI 功能分解:**
- 🔹 **学生注册**: 自助注册固定创建 applicant 账号，不显示角色选择；职员账号由 admin 在后台创建。
- 🔹 **表单验证**: 当密码不匹配或邮箱无效时，系统会实时飘红提示错误。

---

## 申请人指南

指导申请人如何浏览职位、提交申请以及与组织者沟通。

### 导航仪表盘 (Dashboard)
申请人仪表盘提供了你招聘状态的全局概览。

![申请人仪表盘](screenshot/03_applicant_dashboard.png)
**UI 功能分解:**
- 🔹 **左侧导航栏**: 核心导航中枢。
- 🔹 **欢迎栏**: 账户概览与当前活跃的申请数量。
- 🔹 **数据卡片**: 各种状态（如待处理、已录用）的统计总计。

### 查看系统通知
随时查看关于你申请状态的实时警报。

![仪表盘通知](screenshot/11_applicant_dashboard_notifications.png)
**UI 功能分解:**
- 🔹 **通知面板 (右侧)**: 显示诸如“分配已发布”或“状态已更新”等系统警报。

### 搜索公开职位
浏览可用的模块课程，并按部门进行筛选。

![查找职位](screenshot/04_applicant_find_jobs.png)
**UI 功能分解:**
- 🔹 **搜索与筛选栏**: 按学术部门进行过滤。
- 🔹 **职位卡片**: 课程代码、名称以及要求的每周工作时间的高级概览。

### 查看职位先决条件
点击职位卡片以查看详细的任职要求。

![职位详情](screenshot/05_applicant_find_jobs_details.png)
**UI 功能分解:**
- 🔹 **描述面板**: 详尽列出岗位职责与必备技能。
- 🔹 **申请按钮**: 触发职位申请的工作流。

### 提交申请
提供求职信以完成最终的申请提交。

![申请模态框](screenshot/06_applicant_apply_modal.png)
**UI 功能分解:**
- 🔹 **求职信输入框**: 提供一个文本区，用于填写定制化的求职意向。
- 🔹 **提交按钮**: 锁定你当前的个人资料快照并正式提交申请。

### 追踪申请状态
监控你在招聘流水线中的进度。

![我的申请](screenshot/07_applicant_my_applications.png)
**UI 功能分解:**
- 🔹 **状态徽章**: 通过颜色区分不同的流水线阶段（如审查中、已入围、录用、拒绝）。
- 🔹 **撤销操作**: 点击按钮即可撤回进行中的申请。

### 完善学术资料
补全你的个人资料以满足职位的先决条件。

![申请人资料](screenshot/10_applicant_profile.png)
**UI 功能分解:**
- 🔹 **学术详情**: 输入你的 GPA、专业和学位水平。
- 🔹 **技能标签**: 选择与技术模块相关的专长。
- 🔹 **文档上传**: 安全上传简历 (CV) 与成绩单。

### 与模块组织者沟通
直接与负责的教授发起或回复聊天。

![消息空白页](screenshot/08_applicant_messages_empty.png)
**UI 功能分解:**
- 🔹 **联系人列表 (左侧)**: 基于你活跃的申请生成的许可联系人。
- 🔹 **空白状态**: 未选择任何对话时的默认视图。

![消息对话](screenshot/09_applicant_messages_chat.png)
**UI 功能分解:**
- 🔹 **聊天历史**: 包含时间戳的消息信息流。
- 🔹 **消息输入**: 用于撰写询问的文本区域。

---

## 组织者指南

指导模块组织者 (教授) 如何管理职位发布、审查候选人以及记录分配。

### 查看模块概览
访问分配给你的模块的综合指标。

![组织者概览](screenshot/13_organiser_overview.png)
**UI 功能分解:**
- 🔹 **模块容量卡片**: 当前已录用 TA 与需求 TA 的比例。
- 🔹 **近期活动**: 新收到申请的实时时间线。

### 管理 TA 职位
发布、编辑或切换模块招聘信息的可见性。

![TA 职位](screenshot/14_organiser_ta_positions.png)
**UI 功能分解:**
- 🔹 **创建新职位**: 触发创建新岗位的表单。
- 🔹 **职位列表**: 对各条目执行切换可见状态或编辑详情等操作。

**创建职位时设置时间（更新）：**
先在 **Course Sections** 中为课程和申请周期创建 Lecture、Lab 或 Discussion，并设置每周上课时间。同一个 Section 每周上多天课时，点击 **Add meeting time** 增加时间段。

在 **TA Positions → Create Posting** 中选择 **Course**，再在 **Sections & Weekly Schedule** 勾选该职位覆盖的 Section，可同时选择 Lecture 和 Lab，但必须属于同一个申请周期。系统自动显示各 Section 的全部上课时间，统一使用 UCI 时区。旧的 **Work Schedule / Day of Week / Generate** 已移除。

Section 选择表示职位的招聘范围；具体哪位 TA 承担哪些 Section，仍需审核后分配。已有申请的职位不能修改 Section 范围，应新建职位。历史职位未绑定 Section 时保留原来的课程范围；历史日期不再作为排课依据。

### 推进候选人流水线
使用 Kanban（看板）工具管理招聘阶段。

![流水线工作流](screenshot/15_organiser_pipeline.png)
**UI 功能分解:**
- 🔹 **流水线列**: 划分为不同的独立阶段（已申请、审查中、已入围、录用）。
- 🔹 **拖拽界面**: 用鼠标按住候选人卡片并在各列之间拖动以更新状态。
- 🔹 **自动通知**: 状态变更后，系统会自动向申请人发送警报。

### 给申请人发消息
直接与各个候选人进行沟通。

![组织者消息](screenshot/12_organiser_messages.png)
**UI 功能分解:**
- 🔹 **申请人名册**: 按所申请模块分组的候选人列表。
- 🔹 **聊天界面**: 用于发送反馈的富文本区域。

---

## 管理员参考

包含用于平台审计、智能工作负载分析以及全局配置的系统工具。

> [!CAUTION]
> 管理员工具会直接修改全局状态和持久化数据。保存任何更改之前，请确保其符合大学的相关政策。

### 审查平台分析数据
监控整个系统的健康状态与使用情况。

![管理员分析](screenshot/19_admin_analytics.png)
**UI 功能分解:**
- 🔹 **全局指标**: 呈现总用户数、总申请量及最终转化率。
- 🔹 **部门图表**: 按学术部门分类展示 TA 需求的可视化分布。

### 通过 AI 平衡 TA 工作负载
利用“智能工作负载”系统确保符合最高工作时数规定。

![工作负载智能](screenshot/20_admin_workload_intel.png)
**UI 功能分解:**
- 🔹 **AI 洞察面板**: 自动标记工作时长超限的学生。
- 🔹 **重新分配工具**: 赋予管理员强制权限，可将超载学生的录用名额转移给其他候选人。

### 管理全局模块
注册新课程并为其分配模块组织者。

![全局模块](screenshot/21_admin_global_modules.png)
**UI 功能分解:**
- 🔹 **模块注册表**: 系统识别的所有课程的数据表。
- 🔹 **组织者分配**: 通过下拉菜单将特定教授映射至对应的模块。

### 配置系统属性
设置核心平台的全局环境变量。

![属性设置](screenshot/22_admin_properties.png)
**UI 功能分解:**
- 🔹 **学期配置**: 用于输入当前的学术学期名称（例如“2026年秋季”）。
- 🔹 **部门列表**: 针对大学院系部门的动态增删改查（CRUD）界面。

### 管理用户账号
监督全站的注册行为以及角色分配情况。

![用户管理](screenshot/23_admin_users.png)
**UI 功能分解:**
- 🔹 **角色分配**: 将普通账号升级为组织者（Organiser）或管理员（Admin）。
- 🔹 **账号操作**: 具备暂停或删除恶意用户的能力。

### 直接编辑持久化数据
绕过所有业务逻辑，通过现有集合接口管理 PostgreSQL 中的数据；数据库仍会执行唯一性和关联约束检查。

![数据编辑器](screenshot/24_admin_data_editor.png)
**UI 功能分解:**
- 🔹 **JSON CRUD 界面**: 底层数据库集合的表格式视图。
- 🔹 **操作按钮**: 针对原生实体提供直接编辑/删除功能的快捷方式。

### 审计系统日志
查看并追踪平台上所有活动的加密安全日志。

![系统日志](screenshot/25_admin_system_logs.png)
**UI 功能分解:**
- 🔹 **日志表**: 按时间顺序排列的操作记录、用户 ID 及精准时间戳。
- 🔹 **严重度筛选**: 允许快速在 INFO（信息）、WARNING（警告）与 ERROR（错误）日志之间切换过滤。
