ALTER TABLE orders.orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders.orders
  ADD CONSTRAINT orders_status_check
    CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'CANCELLED'));
