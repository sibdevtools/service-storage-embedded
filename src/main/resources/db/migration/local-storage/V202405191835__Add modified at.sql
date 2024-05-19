ALTER TABLE bucket
    ADD modified_at timestamp NOT NULL DEFAULT created_at;