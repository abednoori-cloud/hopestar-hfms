ALTER TABLE number_sequences
    CHANGE COLUMN sequence_last_value `last_value` BIGINT NOT NULL DEFAULT 0;
