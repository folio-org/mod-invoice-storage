CREATE ROLE new_tenant_mod_finance_storage PASSWORD 'new_tenant' NOSUPERUSER NOCREATEDB INHERIT LOGIN;
GRANT new_tenant_mod_finance_storage TO CURRENT_USER;
CREATE SCHEMA new_tenant_mod_finance_storage AUTHORIZATION new_tenant_mod_finance_storage;


CREATE TABLE IF NOT EXISTS new_tenant_mod_finance_storage.fund (
  id UUID PRIMARY KEY,
  jsonb JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS new_tenant_mod_finance_storage.transaction (
  id UUID PRIMARY KEY,
  jsonb JSONB NOT NULL
);
