-- M9: typed integration failure codes for operator assistibility
ALTER TABLE processing_jobs
    ADD COLUMN IF NOT EXISTS last_error_code TEXT;
