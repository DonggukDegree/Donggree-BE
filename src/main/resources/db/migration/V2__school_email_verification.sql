ALTER TABLE public.member
    ADD COLUMN school_email varchar(74),
    ADD COLUMN school_email_verified_at timestamp(6),
    ADD CONSTRAINT member_school_email_key UNIQUE (school_email),
    ADD CONSTRAINT member_school_email_verified_check
        CHECK ((school_email IS NULL) = (school_email_verified_at IS NULL));
