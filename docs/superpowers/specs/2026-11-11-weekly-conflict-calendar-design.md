# Applicant weekly conflict calendar

Status: confirmed by the user and implemented. Changes are uncommitted.

- Replace the Applicant's separate available-time and conflict forms with a Monday–Sunday weekly grid. Show UCI campus time, 30-minute slots, with the full day reachable by scrolling. Click or drag to mark busy slots; support keyboard toggling and a clear selection action. Keep notes and period selection.
- Before saving, explicitly confirm that unmarked times are available. An empty confirmed grid means no conflicts; a wholly marked grid means no availability. An unsaved grid means no submitted schedule.
- Save merged busy intervals. Derive their complement on the server into the existing availability record, so confirmed student schedules are compatible with the current Section matcher and persistence schema. A meeting conflicts when it overlaps any busy interval. Keep existing cross-assignment overlap checks.
- Keep imported conflict-only records pending when no conflict is found, because students have not confirmed their full week. Do not rewrite existing submitted snapshots. For previous editable records, present their existing availability and conflicts before conversion; require confirmation for any change in the meaning of unmarked time.
- Validate adjacent and overlapping intervals, no conflicts, all week busy, prior/imported records, save/reload, and keyboard/mobile interaction.

Alternative: keep unmarked times unknown. This is safer for unconfirmed imported data but does not meet the requested conflict-only student workflow, because many meetings would still require manual confirmation.
