ALTER TABLE chat_action_drafts
    ADD COLUMN IF NOT EXISTS proposal_id VARCHAR(36);
