# 学生每周可用时间

## 学生填写

1. 登录申请人账号，打开侧栏 **Weekly Availability**，或从 Profile 页进入。
2. 选择申请周期。只有已开放且在起止时间范围内的周期允许修改。
3. 周历显示周一至周五、每天 08:00–20:00（UCI 校园时间）。每半小时内有两条浅灰线，标出 10 分钟刻度。点击同一天的开始和结束时刻，形成一个忙碌区间。键盘可用方向键和空格选取，Escape 取消未完成的选取。
4. 确认工作日 08:00–20:00 内未标记的时段均可用于 TA 工作（其余时间不计入可用时间），填写可选备注后保存。可从周历下方移除已选区间。
5. 已保存的周末和范围外冲突会保留；清空按钮只清空可见时段。旧记录的原始分钟边界保持不变，已提交申请的快照不受影响。

草稿周期不向学生提供填写入口；关闭、尚未开始或已截止的周期可查看已保存记录，但不能修改。
每个周期分别保存，不会从另一个周期自动复制。

## 提交与快照

提交职位申请前，必须先保存该周期的安排（也可以明确声明无可用时间）。
申请弹窗会展示已保存的可用时间、冲突和备注，并提供编辑入口。
真正提交时，后端从数据库复制当前学生、当前周期的安排作为该申请的独立快照。
客户端不能通过提交伪造的快照来替换服务器记录。

之后修改 Weekly Availability，只影响未来提交的申请，不会更改先前申请。
学生可在 **Application Tracker → Submitted availability** 查看自己提交时的安排。
本轮没有新增修改已提交快照的流程。

## 工作人员查看

- Organiser：在 Pipeline 中打开 **View Full Application**，查看 **Weekly availability · submitted snapshot**。
- 管理员：在 **Application Periods → View applications → Submitted schedule** 查看。

工作人员始终查看申请自身的快照，不会用学生最新填写的内容替换历史快照。
Organiser 仍然只能查看自己有权审核的职位申请。
旧申请不会补造时间安排，页面显示 **No weekly availability was recorded for this application.**

## 数据与 API

启动时自动执行 V3 数据库迁移，增加 `weekly_availabilities` 表和
`applications.availability_snapshot` JSONB 字段。已有申请 ID、周期和审核状态保留。
升级前备份位于 `backups/tamo-before-weekly-availability.dump`。

接口前缀为 `/tamo/api`，均需要 Applicant 会话：

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/applicant/availability-periods` | 获取已发布的非历史周期及是否可编辑 |
| GET | `/applicant/availability/{periodId}` | 读取当前登录学生在指定周期的安排 |
| PUT | `/applicant/availability/{periodId}` | 保存当前登录学生的安排 |

保存请求示例：

```json
{
  "timeZone": "America/Los_Angeles",
  "availableSlots": [
    { "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" },
    { "dayOfWeek": 1, "startTime": "13:00", "endTime": "17:00" }
  ],
  "conflictSlots": [
    { "dayOfWeek": 1, "startTime": "12:00", "endTime": "13:00", "reason": "Class" }
  ],
  "noAvailability": false,
  "notes": "Other restrictions"
}
```

`dayOfWeek` 为 1（周一）至 7（周日）。每类最多 70 个时段，冲突原因最多 200 字符，备注最多 2000 字符。
学生身份取自登录会话，接口不接受另一位学生的身份作为修改目标。
本轮提供时间采集和快照。[课程 Section 时间表](COURSE_SECTIONS_zh.md) 已可维护；现已支持[基于提交快照的 Section 时间匹配](SECTION_MATCHING_zh.md)。
