ALTER TABLE course_classification
    ADD COLUMN grading_system varchar(20) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE course_classification
    ADD CONSTRAINT course_classification_grading_system_check
    CHECK (grading_system IN ('UNKNOWN', 'GRADED', 'PASS_FAIL'));
