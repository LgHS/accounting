package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import static be.lghs.accounting.model.Tables.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dsl;

    public Result<AccountsRecord> findAll() {
        return dsl.selectFrom(ACCOUNTS)
            .fetch();
    }

    public void createOne(String name, String description) {
        dsl.insertInto(
                ACCOUNTS,
                ACCOUNTS.NAME,
                ACCOUNTS.DESCRIPTION
            )
            .values(name, description)
            .execute();

    }
}
