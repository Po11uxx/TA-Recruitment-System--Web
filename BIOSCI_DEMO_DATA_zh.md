# BioSci 默认演示课程

默认初始化文件：`backend/src/main/resources/data.json`。

本次将原有六门演示课程统一替换为 BioSci 主题，并保留原有课程、职位、账号和申请 ID，避免断开既有引用。

| 原课程 | 新演示课程 | 名称 |
| --- | --- | --- |
| CS201 | BIO 93 | DNA to Organisms |
| CS301 | BIO 98 | Biochemistry |
| CS101 | BIO E109 | Human Physiology |
| EBU6304 | BIO INF1 | Bioinformatics |
| MATH201 | BIO 94 | From Organisms to Ecosystems |
| EE301 | BIO 99 | Molecular Biology |

前四门采用仓库 `Test_Data/Courses & Sections Dataset.csv` 的代码和命名约定，其中 **BIO INF1 是 sponsor 样本中的演示标识，不宣称是 UCI 正式课程代码**。BIO 94、BIO 99 的名称参考 [UCI Biological Sciences 课程目录](https://catalogue.uci.edu/allcourses/bio_sci/)。这里统一使用项目样本的 `BIO` 前缀；正式目录使用 `BIO SCI`。

相关职位的标题、说明、标签、要求和职责已同步为生物学内容。示例学生的课程记录、申请信、遗留说明、通知、消息、日志及旧工作量记录中的课程引用同步替换。历史演示状态、人数、日期、工时、教师姓名和账号凭据保持原样；学生保留原有跨学科背景。教师所属部门改为 Biological Sciences，课程部门清单统一为 Biological Sciences。

这些是合成演示数据。职位要求、教师归属、学期、工时和日期并非学校的真实招聘公告或真实开课安排；遗留匹配分数和 `aiSummary` 字段不代表重新计算或执行了 AI 评估。旧面试数据只用于兼容，不恢复已退役面试功能。原始 sponsor CSV 保持不变。

## 已有数据库与初始化文件的区别

新数据库第一次启动才导入 `data.json`。**更改文件或重启应用不会覆盖已初始化的 PostgreSQL 数据库**。已有数据库的同步需要另外执行，并应先备份，保留用户自行建立的数据与历史快照。不要通过删除数据库卷来刷新演示课程。

### 2026-10-29 本地数据库同步记录

按用户选择，已另行同步当前本地演示数据库。同步前备份至 `backups/backup-20261029-203204-73832/`，包含 PostgreSQL dump 和私有附件归档；数据库备份目录可正常读取。

同步在事务和应用共用 advisory lock 内完成，只更新仍与原始样本相同的目标字段。六门课程和关联职位已显示 BioSci 数据；非目标字段及记录 ID 的事务内比对通过，账号密码、权限、业务状态和关联保持不变，未修改提交/审核/分配快照。自定义或已编辑的字段不会被种子值覆盖。

已有数据库保留其他部门记录，只刷新课程数量，避免删除原有部门；新库初始化的部门清单则为 Biological Sciences。这次同步未重置数据库，也没有导入 sponsor CSV 或新建 Section。
