ALTER TABLE payments.payments RENAME COLUMN razorpay_payment_id TO payment_id;
ALTER TABLE payments.payment_events RENAME COLUMN razorpay_event_id TO event_id;

DROP INDEX IF EXISTS idx_payments_razorpay_payment_id;
DROP INDEX IF EXISTS idx_payment_events_razorpay_event_id;

CREATE INDEX idx_payments_payment_id ON payments.payments(payment_id);
CREATE INDEX idx_payment_events_event_id ON payments.payment_events(event_id);
