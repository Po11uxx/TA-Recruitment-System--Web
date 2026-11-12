# Section 时间匹配

Organiser 登录后，在 **Application Pipeline** 中使用课程、Section、Time matching 和申请周期筛选。点击申请人查看摘要，或进入完整申请页面查看每个 Section 的上课时间及具体冲突。

- **Available**：申请时提交的可用时间覆盖该 Section 的全部上课时段，且没有明确冲突。
- **Time conflict**：标出不在可用时间内或与明确冲突重叠的星期、起止时间。学生声明完全不可用时，也显示冲突。
- **Needs confirmation**：缺少提交快照、身份/周期/时区无法确认、时间数据不完整，或课程尚无有效 Section。

未选定 Section 时，卡片的 Available 表示至少一个 Section 可安排；若没有可安排的 Section，但存在待确认项，则显示待确认，否则显示冲突。选定 Section 后，筛选和卡片状态只针对该 Section。课程尚无申请时，不会出现在申请筛选列表中。

## 计算规则

- 使用申请的 `availabilitySnapshot`，不会读取学生后来修改的 Weekly Availability。旧申请没有快照时保持待确认。
- 对照课程**当前未归档**的 Section；修改 Section 时间、归档或恢复后，刷新页面会重新计算。
- 相邻可用时段可以共同覆盖一个上课时段。结束时间与另一时段的开始时间相同不算重叠；支持周日及结束时间 `24:00`。
- 明确冲突优先于可用时间。未填入可用范围的时间也不能判为可安排。
- 固定使用校园时区 `America/Los_Angeles`，按每周重复时间比较。
- 所有有效 Section 均显示，包括 TA 需求为零的课堂参考时段。
- 只显示申请所属课程的 Section。课程当前负责人和职位负责人必须都与当前 Organiser 一致；课程负责人变化时，原负责人不会看到该课程的 Section 数据。

匹配是审核提示，不会自动分配 TA、占用名额、更改申请状态或发送通知；也不代表多个 Section 的组合排课或总工时已被核验。本轮不增加数据库表或收费服务。
