# Accounting application for the LgHS

## Setup

Create yourself a nice cosy space from your `psql` prompt:

```sql
create user lghs_accounting_root with password 'lghs_account_root_password'; -- change it really
create database lghs_accounting owner lghs_accounting_root;

\c lghs_accounting
revoke all on schema public from public;

create user lghs_accounting_app;
grant connect on database lghs_accounting to lghs_accounting_app;

create extension "uuid-ossp";
create extension pgcrypto;
```


### Jooq

Classes describing the database are generated using jooq. You can modify the `gradle.properties` file to allow 
the build script to connect to your database and generate those files using the `jooq` task.

The `jooq` task is a compound task that will make sure your database matches the latest flyway state before running
jooq's codegen. The generated code is located in `build/generated/sources/jooq/java`


### Local configuration

I recommend creating a folder in the root of the repository named `run` and adding a file named `application-dev.yml`
in it that will contain the local configuration of the application (keys, passwords and the likes can go there).


### OAuth2 configuration

The configuration for the members oauth endpoint need to be manually provided in your local configuration. You can ask
an admin to get the dev credentials.

```yaml
spring.security.oauth2.client.registration.members:
  client-id: x
  client-secret: azertyuiopqsdfghjklm
```
