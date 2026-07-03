-- Prevents the daily reminder job from re-sending the same reminder every day
-- between the reminder window opening and the payment being settled.
ALTER TABLE payments ADD COLUMN reminder_sent_at TIMESTAMPTZ;
