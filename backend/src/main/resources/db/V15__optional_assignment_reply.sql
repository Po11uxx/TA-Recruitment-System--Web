-- Replies remain optional until the student responds or authorised staff cancel.
-- Preserve legacy deadline/expiry columns and cancelled history; remove only
-- the constraint that prevents publishing an assignment without a reply deadline.
ALTER TABLE section_assignments DROP CONSTRAINT IF EXISTS assignment_pending_deadline;
