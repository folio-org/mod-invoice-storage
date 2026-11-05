-- Supports the calendar trigger query, which matches the date part of paymentDue: left(jsonb->>'paymentDue', 10)
CREATE INDEX IF NOT EXISTS invoices_payment_due_date ON ${myuniversity}_${mymodule}.invoices
  (left(jsonb->>'paymentDue', 10));
