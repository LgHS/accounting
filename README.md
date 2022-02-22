# Accounting application for the LgHS


## Devenv, compile and run

1. create an alias for docker-compose
```bash
alias dcacc='docker-compose -p lghs-accounting -f path/to/accounting/dev-env/docker-compose.yml'
```

2. launch db and keycloak
```bash
dcacc up -d db keycloak
```

3. init db (only the first time you launch the db)
```bash
cat prepare_db.sql | dcacc exec -T db psql -Upostgres
```

4. generate jook tables
```bash
dcacc run --rm app gradle --stacktrace jooq
```

5. launch app
```bash
dcacc up -d
```

7. add entry to host `127.0.0.1   accounting.local` and connect to https://accounting.local or connect to http://localhost:8080




## Dependencies

You'll need a postgresql database and a compiled version of [coda-rs](https://github.com/bendem/coda-rs/tree/develop).

You can start a transient database using postgresql, you'll have to insert an account and a first movement manually
before being able to access the dashboard:
```shell
# works with docker too
podman run --rm -it -e POSTGRES_HOST_AUTH_METHOD=trust -p 127.0.0.1:5432:5432 docker.io/postgres:13
```

Make sure you provide the path to the coda-rs executable through the config param `lghs.accounting.coda-rs`.

## Setup

Create yourself a nice cosy space from your `psql` prompt:

```sql
create user lghs_accounting_root with password 'lghs_accounting_root_password'; -- change it really
create database lghs_accounting owner lghs_accounting_root;

\c lghs_accounting
revoke all on schema public from public;

create user lghs_accounting_app;
grant connect on database lghs_accounting to lghs_accounting_app;

grant usage on schema public to lghs_accounting_root;
grant usage on schema public to lghs_accounting_app;

create extension "uuid-ossp";
create extension pgcrypto;
create extension btree_gist;
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
