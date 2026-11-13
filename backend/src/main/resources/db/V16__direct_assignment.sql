ALTER TABLE section_assignments
    ALTER COLUMN application_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS direct_reason TEXT;

ALTER TABLE section_assignments DROP CONSTRAINT IF EXISTS section_assignments_source_check;
ALTER TABLE section_assignments
    ADD CONSTRAINT section_assignments_source_check CHECK (
        (application_id IS NOT NULL AND direct_reason IS NULL)
        OR (application_id IS NULL AND direct_reason IS NOT NULL AND length(btrim(direct_reason)) > 0)
    );
