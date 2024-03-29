CREATE ROLE new_tenant_mod_orders_storage PASSWORD 'new_tenant' NOSUPERUSER NOCREATEDB INHERIT LOGIN;
GRANT new_tenant_mod_orders_storage TO CURRENT_USER;
CREATE SCHEMA new_tenant_mod_orders_storage AUTHORIZATION new_tenant_mod_orders_storage;


CREATE TABLE IF NOT EXISTS new_tenant_mod_orders_storage.purchase_order (
  id UUID PRIMARY KEY,
  jsonb JSONB NOT NULL
);
CREATE TABLE IF NOT EXISTS new_tenant_mod_orders_storage.order_invoice_relationship (
  id UUID PRIMARY KEY,
  jsonb JSONB NOT NULL
);
