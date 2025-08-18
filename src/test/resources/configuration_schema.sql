CREATE ROLE new_tenant_mod_configuration PASSWORD 'new_tenant' NOSUPERUSER NOCREATEDB INHERIT LOGIN;
GRANT new_tenant_mod_configuration TO CURRENT_USER;
CREATE SCHEMA new_tenant_mod_configuration AUTHORIZATION new_tenant_mod_configuration;


CREATE TABLE IF NOT EXISTS new_tenant_mod_configuration.config_data (
  id UUID PRIMARY KEY,
  jsonb JSONB NOT NULL
);
