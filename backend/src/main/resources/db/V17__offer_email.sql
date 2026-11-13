CREATE TABLE IF NOT EXISTS offer_emails (
    id TEXT PRIMARY KEY REFERENCES section_assignments(id) DEFERRABLE INITIALLY DEFERRED,
    recipient_email TEXT NOT NULL CHECK (length(btrim(recipient_email)) > 0),
    course_code TEXT NOT NULL,
    course_name TEXT NOT NULL,
    section_name TEXT NOT NULL,
    placement_path TEXT NOT NULL,
    created_at TEXT NOT NULL,
    row_order INTEGER NOT NULL
);
