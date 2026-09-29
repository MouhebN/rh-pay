ALTER TABLE chat_action_drafts
    ADD COLUMN IF NOT EXISTS payload_json TEXT;
