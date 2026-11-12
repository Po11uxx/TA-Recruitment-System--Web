> Version 1 更新：创建职位前先配置 Course Sections，再选择 Course 和所覆盖的 Section；版本改动及限制见 [Version 1 版本说明](VERSION_1_zh.md)。

> 更新：CSV 与 sponsor 新字段已完成，详见 [Sponsor 表单与 CSV 说明](SPONSOR_CSV_GUIDE_zh.md)。

# Proposal 功能交接与验收

本文件描述当前本地版本，优先于旧手册中的演示流程。CSV 导入导出见上述新版说明。运行不依赖付费云数据库或付费 AI 服务。

## 启动

启动 Docker Desktop，在项目根目录运行 `./run.sh`，访问 `http://localhost:8080/tamo/auth/login.jsp`。停止 Java 服务使用 Ctrl+C；`docker compose stop db` 只停止数据库，不删除数据。不要执行 `docker compose down -v`，该命令会删除数据卷。

数据库为 PostgreSQL 17，默认本机端口 5433。`data.json` 仅首次初始化导入；日常读写发生在数据库。重启不清空账号、申请、审批或分配。现有明文演示密码在启动时自动转换为带随机盐的 PBKDF2 哈希；源 JSON 仍含原有演示凭据，不应作为正式部署数据。

## 一轮招聘的操作顺序

1. Admin 在 **Application Periods** 创建周期，设置开始与截止时间，开放申请。
2. Organiser 先在 **Course Sections** 为自己的课程选择周期，填写 Faculty、lecture/lab/discussion、每周上课时间、TA 名额和每名 TA 的周工时。再在 **TA Positions → Create Posting** 选择 Course 和同一周期的 Section 并发布职位。在 **Matching Criteria** 选择职位和周期，设置必需/优先技能、学生层级、兴趣及培训要求。
3. 学生填写 **Profile**（学术资料、附件）、**TA Application Profile**（学号、UCI 邮箱、层级、项目、技能、教学/科研经历、培训状态及真实性确认），并保存本周期的 **Weekly Availability**。申请时选择提交附件。
4. 提交后，系统保存当时的资格、学术资料、选中附件元数据及可用时间快照。学生后续修改资料不会改变既有申请。历史申请缺少快照时明确显示需要确认，不自动补造。
5. 授权人员进入 **Review Workspace**，按周期、课程、姓名、层级、技能、兴趣、经历、匹配结果和选拔状态组合筛选，并按姓名、日期或匹配分排序。
6. Reviewer 记录内部说明、标记、推荐或候补。Decision-maker 审批或记录不选用理由；最终决定至少填写 10 字符。记录包含操作者、时间与当时匹配证据。
7. 审批通过后，Organiser 在 **TA Assignments** 分配本周期 Section，确认后发布。学生在 **My TA Placement** 接受或拒绝；未回复的安排保持待确认，由授权人员根据实际情况取消。
8. 学生 Dashboard 和管理员 Workload Intel 按同一分配台账统计。**已接受工时**仅计正式发布且学生接受的有效安排；**预留工时**还包含待回复安排，管理员同时看见草稿预留。不同周期独立统计。

## 默认角色与课程范围

| 角色 | 默认能力 | 可查看课程 |
| --- | --- | --- |
| Applicant | 维护个人资料、申请、查看并回复自己的安排 | 自己申请的资料 |
| Organiser | 维护课程职位、Section 与匹配条件；查看、记录、推荐及执行已批准的分配 | 当前归属自己的课程 |
| Reviewer | 查看、内部记录、推荐或候补 | Admin 明确授予的课程 |
| Decision-maker | 查看、内部记录、审批或不选用 | Admin 明确授予的课程 |
| Viewer | 只读申请与匹配说明；默认不显示内部审核记录 | Admin 明确授予的课程 |
| Admin | 管理周期、账号、课程授权和全部审核能力 | 全部课程 |

Admin 在 **User Management** 创建 staff 账号，并在 **Review Access** 配置课程及动作权限。未授权任何课程的新 Reviewer/Decision-maker/Viewer 看不到申请。自定义权限会覆盖角色默认动作；修改角色会清除旧的自定义动作权限，回到新角色默认值。停用账号或撤销权限后，下次 API 请求即生效。

推荐与批准分开保存。推荐不等于录用，未经批准不能创建或发布正式分配。已分配的申请要改为不选用，必须先取消有效安排。存在审核或分配历史的父记录保留，避免删除历史依据。

## 匹配和跨周期规则

匹配为可解释规则，没有随机分，也不会自动选择学生。必需项和优先项分别列出命中与缺失；培训状态、技能与教学经历基于申请人的提交，不代表学校已核验。审批人可结合理由作出决定；明确的上课时间冲突仍阻止分配，缺失时间数据需人工确认。

同一课程不同周期可有同名 Section、不同 Faculty 和时间安排；职位的匹配条件也按周期保存。旧版未归属周期的 Section 保留为历史，使用 **Copy to cycle** 创建新周期副本；已存在的分配继续使用冻结的 Section 快照。

## 附件与安全

上传接口只接受实际 multipart 文件，限制类型及 10 MB 大小，拒绝目录路径文件名。新文件保存在 `backend/storage/uploads/`；旧公开上传目录首次启动迁到 `backend/storage/legacy-uploads/`。原静态附件地址统一返回 404。下载走 `/api/documents/{id}`，要求本人或具有该已提交申请所在课程的查看权限；申请上下文用 `applicationId` 指定。已被提交引用的附件禁止删除。

API 响应不包含密码或内部文件键。登录验证账号状态并更换会话 ID；敏感资料采用安全文本渲染，DOCX 预览清理 HTML；跨来源写入被拒绝。新增表单有可见标签、状态提示，审核详情可用键盘展开，日历周切换按钮有可访问名称。这些是基础改进，未声称完成独立渗透测试或全面 WCAG 审计。

## 备份与恢复演练

在项目根目录运行：

```bash
./backup.sh
./verify-backup.sh backups/backup-YYYYMMDD-HHMMSS-PID
```

第一个命令生成 PostgreSQL 自定义格式 `database.dump`，并在存在附件时生成 `uploads.tar.gz`。二者应一起保存；备份含私人资料，不放入 Git 或公开分享。在线备份测试适用；正式运维应在暂停上传和写入的维护窗口备份，保证文件和数据库的一致性。

验证脚本把备份恢复到独立的临时数据库，检查 schema、申请和分配记录，并测试解压附件；完成后只删除该临时环境，原数据库不变。

实际恢复前先停应用，并另做当前状态备份。建议恢复到新的命名数据库，不直接覆盖原库：

```bash
docker compose exec -T db createdb -U tamo tamo_recovered
docker compose exec -T db pg_restore -U tamo -d tamo_recovered --exit-on-error --no-owner --no-privileges < backups/你的备份目录/database.dump
# 在独立暂存目录解压并检查后，将备份的 storage 目录放到 backend/storage
# 设置 TAMO_DB_URL=jdbc:postgresql://127.0.0.1:5433/tamo_recovered 后运行 ./run.sh
```

恢复后检查登录、周期、审批记录、已接受工时及附件下载。旧库和旧 storage 保留到验收完成。`verify-backup.sh` 用于演练，不是替换生产数据库的命令。

## 正式使用的边界

当前为本地可运行 MVP，使用本地账号和站内通知，没有接入 UCI SSO、学校培训核验、真实学生信息、外发邮件、HR 或工资系统。生产部署需学校确认数据保存政策、账号接入与授权负责人，配置 HTTPS、受限网络和非演示凭据，并进行学校要求的安全与可访问性审查。数据库默认只监听本机；Java 服务对外部署应由受控反向代理提供访问。

本文件原有验收后的 CSV 扩展已完成，见上述新版说明。
