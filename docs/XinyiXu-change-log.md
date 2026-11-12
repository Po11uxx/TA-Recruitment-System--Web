# XinyiXu 本次改动记录

更新日期：2026-10-28。工作分支：`XinyiXu`，基线：`d8f1835`（Version 1）。

状态：用户已确认提交与推送本记录涵盖的累计改动。本记录汇总 Interview 移除、用户角色检查中的第 1–5 项修复，以及后续确认的 Teacher 直选、admin 专属 CSV 和角色名称统一。此前积累的前端改动仍保留在同一工作区，见文末。

## 1. 移除 Interview 功能

- 删除教师 Interviews 页面、导航、排期/编辑操作及对应 GET/POST/PUT/DELETE API；原 API 返回 404。
- 删除申请流程、详情、仪表盘中的面试按钮、阶段、统计和事件；隐藏历史面试通知。
- Pipeline 使用 Applied → Review → Shortlisted → Offer / Rejected。正式录用审批继续在 Review Workspace 完成；Section 分配与学生回复继续使用 TA Assignments / My Assignments。
- Data Editor 不再提供 interviews 集合，直接访问返回 410；禁止新增 interview 状态。
- 保留历史数据、模型和数据库映射。旧 interview 状态在活动界面投影为 review，不批量改写历史记录。
- 更新首页介绍及中英文 README、使用手册。旧 PDF、截图和生成的 Javadoc 未重新生成。

详细兼容说明见 [interview-retirement.md](interview-retirement.md)。

## 2. 修复角色检查第 1–5 项

| 原问题 | 本次行为 | 主要代码 |
| --- | --- | --- |
| 1. 消息资料接口可查询无关学生资料 | 查询前检查请求人、目标账号状态及授权关系；无权返回 403，目标不存在返回 404。教师仅看到当前负责且有审核权限的课程对应的学生申请摘要；学生间不能借该入口读取彼此资料。联系人、聊天记录和未读计数也遵守权限边界。 | MessageService、MessageServlet |
| 2. 撤销审核权限后仍可通过旧 Pipeline 操作 | Dashboard、Pipeline、申请详情及关联分配候选入口检查 review.read。Shortlist 等操作要求 recommend 或 approve；正式拒绝要求 approve。接口拒绝未授权调用，页面按服务端能力标记显示按钮，缺少标记时默认不显示操作。 | ReviewAccess、OrganiserService / Servlet、SectionAssignmentService、教师页面 |
| 3. 课程转交后新旧教师职责错位 | 以当前 Module 所属教师为课程访问依据。管理员转交课程时先验证接收人为有效教师，再同步该课程所有职位的负责人及姓名，包括已关闭职位。新教师可维护职位、条件、审核和分配；旧教师失去当前课程访问。正常课程编辑与 Data Editor 课程编辑使用同一逻辑。 | AdminService、ReviewAccess、CriteriaServlet、SectionMatchingService、SectionAssignmentService |
| 4. 旧 Reject 路径绕过正式审批、造成状态冲突 | Reject 必须有 review.approve 并填写 10–4000 字符理由，通过 ReviewService 记录 not_selected、操作者及依据。已有有效分配时先取消分配；已决策申请不能从旧 Pipeline 倒退到待审状态。Offer 仍要求已有正式批准。Data Editor 禁止改变已有申请状态，指向 Pipeline / Review Workspace；允许保持原状态编辑其他字段。 | OrganiserService / Servlet、ReviewService、AdminService、Pipeline / 详情页面 |
| 5. Data Editor users 可绕过账号业务规则 | 删除 users 编辑入口；通用 users 新增/更新/删除在服务及 HTTP 层返回 403，提示使用 Users。保留其他集合必要的安全用户引用，不返回密码字段。Users 业务接口验证角色及状态，禁止管理员禁用本人，并保留本人角色修改/删除保护。 | AdminService、AdminServlet、admin/data-editor.jsp |

### 课程交接与历史记录

课程和职位的当前责任人会变更；既有申请内容、审核记录、分配记录、历史操作者及快照不因交接重写。匹配与新操作使用当前课程责任人。即使旧数据的 Job 负责人字段不同步，也不能作为旧教师继续访问的依据。

### 页面与接口配合

- Pipeline 和申请详情接收 canApprove、canRecommend、canNote、selectionApproved 等能力标记。
- 无批准权限时隐藏 Reject；有权限时要求输入理由，取消输入不发送请求。
- 前端隐藏按钮之外，后端仍独立校验权限、课程范围及状态前置条件。

## 3. 验证记录

上一轮结果：377 项 Java 测试通过，失败 / 错误 / 跳过均为 0（明确排除 PostgresStoreTest）；24 项前端测试通过，失败 / 跳过均为 0。Data Editor 内联 JavaScript 语法检查及 git diff --check 通过。

复核命令：

```sh
mvn -o -f backend/pom.xml '-Dtest=*,!PostgresStoreTest' test
node --test tests/frontend/*.test.cjs
git diff --check
```

前端 Chromium 用例需设置 PLAYWRIGHT_MODULE 和 CHROMIUM_EXECUTABLE 指向已安装的 Playwright 模块与 Chromium。

验证覆盖无关用户资料查询、撤销权限、课程交接、新旧教师访问、审批理由与记录一致性、通用账号写入拦截、自账号保护、Interview 退役及前端交互。审查另外发现 Data Editor 仍能直接改申请状态，已先复现，再新增保护与回归测试。

测试主要使用合成内存数据；浏览器检查使用页面脚本和替换 JSP 指令后的页面体，并拦截合成 API 响应。它们不证明真实 JSP 编译、运行中的服务已更新或真实 PostgreSQL 数据保存成功。独立的 PostgresStoreTest 本次未运行；本次没有修改本地真实账号、课程归属或权限数据。

## 4. 同一工作区中保留的此前改动

- 登录页输入邮箱后进入 Create account，带入已填邮箱。
- 收紧 applicant 的页面和功能入口，包括此前的 CSV 访问限制。
- 学生 TA Application Profile 分区展示、校验与编辑交互；保存后为只读展示，点击 Edit 才进入编辑，可取消恢复。

这些是此前积累的改动，不表示本次重新实现；已连同本次修复一并获得用户提交授权。

## 5. Teacher 直选 TA（后续批准范围）

入口：Teacher 的申请详情页，以及 Review Workspace 中的 **Select as TA**。

1. applicant 正常提交课程职位申请及资格 / availability 快照。
2. 当前课程 Teacher 打开申请，选择职位涵盖的有效 Section，填写 10–4000 字符录用理由。availability 待确认时，需要勾选明确确认并填写 10–1000 字符说明；有明确时间冲突时不能分配。
3. 提交成功会记录带操作者、Section 与匹配证据的正式 approve 决策，把申请置为 offer，并建立 Section 分配草稿；此时尚不向 applicant 发布分配。
4. Teacher 通过链接进入 TA Assignments，检查草稿、填写回复截止时间并发布。
5. applicant 在 My Assignments 查看已发布分配并接受或拒绝；admin 可查看及管理所有课程的分配。

Teacher 的默认权限现在包含 review.approve；已显式配置 permissions 的账号保持原配置，因此被撤销的权限不会被新默认值覆盖。原有 reviewer / decision_maker / viewer 权限不因这个默认值变更而升级。

直选先在隔离数据副本上执行完整审批和分配校验，通过后再在当前请求事务和共享锁内应用。名额不足、重复分配、课程 / 周期 / 职位 Section 不符、排期冲突、无效账号或缺少权限时，不留下半条批准记录。既有正式决定不会被直选静默覆盖，重复请求返回冲突，不重复创建草稿。匹配继续作为人工判断依据。

## 6. CSV 与角色显示名称

- 完整 CSV Import & Export 仅供有效 admin 使用。删除 Teacher CSV 页面及侧栏菜单，并在 AuthFilter、CSV Servlet 和业务服务独立校验；Teacher 和高级审核角色直接调用导入、导出、模板或 options 接口也会被拒绝。
- 主角色显示名称统一为 `applicant`、`Teacher`、`admin`，覆盖登录、导航、角色徽章、账号管理、课程负责人及当前使用说明。使用 RoleLabels 统一转换。
- 保留内部 `organiser` 角色值、路径和数据库兼容性；未自动迁移或删除真实账号。既有 reviewer、decision_maker、viewer 明确显示为高级审核角色。
- 已更新 README、使用手册和 Sponsor CSV 指南。历史审查记录保留当时发现，不作为当前行为说明。

### 本轮验证

后端全量非 PostgreSQL 回归：408 项通过，失败 / 错误 / 跳过均为 0。其后补充成功 HTTP 路径与会话操作者测试，直选服务 / 接口定向复核 22 项通过（其中服务 19 项、接口 3 项）。前端 30 项全部通过，失败 / 跳过均为 0；20 段改动 JSP 内联脚本语法检查及 git diff --check 通过。

代码审查发现并修正两处界面问题：待确认 availability 的说明上限与后端统一为 1000 字符；已有分配使用中性状态提示，避免把已发布或已接受分配误称为草稿。新增回归先复现失败再修复，复核未发现剩余具体问题。

上述测试使用合成数据及页面夹具。本轮未运行独立 PostgresStoreTest，也未验证运行中的服务已部署、真实 JSP 编译或真实数据库保存。没有修改本地真实账号权限或课程数据。

## 7. 范围与提交确认

- 初轮完成角色检查第 1–5 项；本轮进一步实现第 6–8 项涉及的 CSV 管理收口和 Teacher 直选，并统一主角色名称。
- Review Workspace / Pipeline 合并、精细字段可见性及高级角色迁移不在此次实现范围。
- 用户已确认将上述累计改动提交并推送至 `XinyiXu`；最终提交哈希及远程状态以 Git 记录为准。


## 8. 登录与品牌文案调整（2026-11-10）

- 删除登录页的三角色选择卡片、高级审核角色下拉框及相关状态脚本；客户端登录仅发送邮箱、密码。
- 登录身份及跳转使用服务端已存储的账号角色，支持现有高级审核账号。后端忽略客户端传入的 role，不依赖它判断身份；未知或缺少返回角色时前端提示错误，不默认进入学生页面。
- 公开注册只展示学生账号申请，去掉 Teacher / admin 的说明和任何角色状态。共享注册方法固定提交 applicant，后端拒绝伪造的 Teacher、admin 或高级审核角色；职员账号仍由 admin 在后台创建。
- 首页、所有角色侧栏、登录 / 注册及公共页面标题统一为 TA Application System；流程与统计使用 Application Pipeline / application。同步更新 API 提示、当前 README / 使用手册、演示脚本及可编辑图示文字。
- 内部兼容类名 RecruitmentStatus、现有仓库地址与 Git 目录名称不属于界面文案，保持不变；历史 PDF 和截图不批量改写。
- 回归覆盖六类账号登录跳转、无角色选择、注册固定学生身份、无效返回角色和伪造职员注册限制。最终结果：410 项 Java 测试通过（排除独立 PostgresStoreTest），39 项前端测试通过，失败 / 错误 / 跳过均为 0；6 段改动 JSP 内联脚本语法检查与 git diff --check 通过。测试为合成数据 / 浏览器页面夹具，不代表真实数据库或已运行服务验证。
- 本轮工作区修改尚未提交或推送。

- 2026-11-10 补充：登录与注册页右上角学院图标链接到 https://www.bio.uci.edu/，保持图标样式并支持键盘访问。

- 2026-11-10 补充：首页说明段落与标题间距增至 48px，改为左对齐并扩大桌面最大宽度；标题取消固定高度，避免两行大字溢出后挤压说明段落。

- 2026-11-10 补充：首页三张图替换为显微镜实验、学生图书馆学习及共同学习场景，移除图表和商务团队素材。同步修改替代文字、尺寸及下方图片懒加载；图片来源记录在 static/images/HOMEPAGE_PHOTO_SOURCES.md。外链均验证返回 200 image/jpeg。


## 9. Assignment 页面显示优化（2026-11-10）

- 主角色的显示名称由 applicant 改为 Applicant，内部账号键和路由仍为 applicant。更新公共角色转换及管理角色选项。
- 单选下拉框使用统一细线箭头，右侧保留 14px 内边距、文字预留 44px 空间并垂直居中；保留原生选择器的键盘操作，多选列表不受影响，高对比模式恢复原生箭头。
- Assignment 页的准备、发布、回复截止、回复、过期和取消时间按登录设备 / 浏览器时区格式化，并包含日期和时区。日期选择器继续使用本机时区。
- 每周课程上课时间属于无具体日期的课程时区规则，保留其原始时区并明确标注，不直接改写成设备时间；服务器仍决定截止、过期及分配规则。
- My Applications 用于申请审核进度；My Assignments 用于录用后的 Section 安排、学生接受 / 拒绝、回复截止与每周工时。它不是教学待办清单。
- 新增跨日期、夏令时和无效时间回归，并在洛杉矶 / 东京时区运行真实 Chromium 页面夹具，检查日期、下拉框及筛选。最终结果：44 项前端回归通过，RoleLabelsTest 2 项 Java 回归通过，脚本语法检查及 git diff --check 通过。浏览器测试使用合成 API，不代表运行中服务或真实数据库验证。
- 本轮累计改动未提交或推送。


## 10. 按 Proposal 简化分配流程（2026-11-10）

- 用户确认保留学生在线接受 / 拒绝，移除非必需的回复截止时间、倒计时和自动超时释放名额。
- 保留推荐 / 正式选择、分配草稿、确认发布、课程与 Section、预计工时、名额、重复 / 排期冲突检查、学生回复、授权人员取消和最终名单导出。
- 学生入口更名 **My TA Placement**，内部 assignments 路径保持兼容。申请进度继续在 My Applications；分配页面不是日常教学任务管理。
- 发布只需确认，不要求日期。仍有效且待回复的旧记录不受历史截止时间影响；无回复不会自动取消。
- 移除应用启动后的定时过期线程、API 请求前的过期维护和业务服务中的过期处理。V15 移除数据库强制截止约束，保留旧字段及历史已取消记录，不自动恢复过去已释放的名额。
- 列表及学生 / 职员操作响应不再返回截止字段。旧 expired 仅作为取消历史展示。
- 验证：隔离测试数据库中 23 项 PostgreSQL 集成测试通过，包括旧约束移除、无截止时间发布、重新连接后回复及历史字段保留；未操作真实应用库数据。最终非 PostgreSQL Java 回归 410 项通过，前端 48 项通过，失败 / 错误 / 跳过均为 0；git diff --check 和脚本语法检查通过。审查发现的职员操作响应残留截止字段问题已先复现再修复，最终复核无剩余具体问题。临时测试数据库已清理。
- 运行中的旧 Java 服务尚未重启；需要重启项目，以加载简化后的后端并在启动时自动执行 V15 迁移。
- 本轮累计工作区改动尚未提交或推送。


## 11. 暂时移除 Messages 界面（2026-11-10）

- 移除 Applicant、Teacher、admin 的 Messages 页面与侧栏入口，以及共享聊天内容 / 初始化 JSP，旧页面路径不再提供聊天界面。
- 移除 Data Editor 中的 Messages 标签、消息编辑 schema / 提示及消息集合预加载，避免其他界面仍暴露消息编辑入口。
- 后端 MessageService、MessageServlet、消息 API 映射、权限检查、数据模型、数据库历史及附件保持不变。共享 API 客户端定义保留，但没有聊天页面调用它们。
- 审核备注、系统通知及分配发布 / 回复 / 取消流程保留。application 内提交 issue 的想法仍待讨论，本次未实现。
- 验证：43 项后端消息 / 权限回归、48 项前端回归通过；Data Editor 脚本语法与 git diff --check 通过。源码检查确认没有 Messages JSP 页面引用或聊天入口，消息后端文件及 API 映射未修改。
- 本轮累计改动尚未提交或推送。


## 12. 学生忙碌时段周历（2026-11-11）

- Weekly Availability 改为周一至周日、每格 30 分钟的周历，按 UCI 校园时区显示。支持点击、鼠标拖选、键盘操作和手机横向滚动。
- 学生仅标记没空的时段；保存前须确认未标记时间可用。后端生成忙碌区间的可用补集，沿用现有存储与 Section 匹配；空白且确认代表无冲突，全周标记代表无可用时间。
- 未编辑的旧分钟边界保留，部分忙碌格标为 Partial。未确认的 CSV 记录、已提交申请快照与既有分配冲突检查不受影响。
- 验证：60 项后端排期 / 匹配 / 分配测试及 2 项 Chromium 周历测试通过；浏览器保存使用 API 夹具，未验证运行中服务。

- 2026-11-11：按 Fall 2026 BIO SCI WebSoc 中有明确时间的课节，将每小时边界改为 :00、:20、:30、:50；TBA 不生成占用。保留旧分钟边界、确认规则和后端冲突逻辑。更新脚本版本以避开旧缓存。2 项 Chromium 周历测试通过，覆盖精确保存 09:00–09:50 和保留旧 09:15–09:45 记录。

## 13. 明确 admin 和 Teacher 的课程权限（2026-11-12）

- Teacher 继续在自己负责的课程维护职位、Section 和匹配条件，并按审核权限处理申请及发布 TA 分配。Applicant 继续在 My TA Placement 查看并回复已发布的分配。
- admin 保留用户、课程归属、申请周期、权限和 CSV 管理。Review Workspace、Matching Criteria、Course Sections 和 TA Assignments 默认提供全局只读视图。
- 需要代办时，由另一名 admin 在 Review Access 为指定账号选择课程、操作权限和授权理由。可分别授予审核操作、课程配置、分配管理。直接选 TA 同时需要审批与分配权限。撤销在后续请求生效。
- 授权记录包含授权人、课程、权限和理由。代办分配草稿与发布须填写操作理由，审核决定与取消继续记录各自理由。
- CSV 导出和学生导入保留在 admin。导入 Section 前在 Modules 创建课程并指定 Teacher，再授权课程配置。导入选择结果遵循同一套课程审核权限。Data Editor 的课程、职位、申请、工时分配和日志集合改为只读，课程管理通过 Modules 进行。
- 验证：415 项非 PostgreSQL 后端测试和 53 项前端测试通过，包括授权、撤销、课程隔离、CSV 权限与分配页面。Chromium 使用合成数据验证，未连接运行中的应用数据库。
