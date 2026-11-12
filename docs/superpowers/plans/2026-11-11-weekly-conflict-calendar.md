# Weekly conflict calendar implementation plan

- Add service tests for confirmed busy intervals, their complement, empty/full weeks, and unchanged snapshots.
- Derive the available interval complement in AvailabilityService; preserve legacy payload behavior and imported matching semantics.
- Replace availability.jsp and availability.js with a keyboard-accessible weekly grid, exact legacy interval preservation, and confirmation before save.
- Verify the service, Section matching, and browser save/reload; update the change log. No commit or push.
