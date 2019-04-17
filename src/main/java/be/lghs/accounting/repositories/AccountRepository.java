package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Tables;
import be.lghs.accounting.model.tables.records.AccountsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dsl;

    public Result<AccountsRecord> findAll() {
        return dsl.selectFrom(Tables.ACCOUNTS)
            .fetch();
    }
}
