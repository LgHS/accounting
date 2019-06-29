package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<AccountsRecord> findOne(UUID id) {
        return dsl.selectFrom(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(id))
                .fetchOptional();
    }

    public int update(UUID accountId, String name, String description) {
        return dsl.update(ACCOUNTS)
                .set(ACCOUNTS.NAME, name)
                .set(ACCOUNTS.DESCRIPTION, description)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public void updateBalance(UUID accountId, BigDecimal added) {
        dsl.update(ACCOUNTS)
            .set(ACCOUNTS.CURRENT_AMOUNT, ACCOUNTS.CURRENT_AMOUNT.plus(added))
            .where(ACCOUNTS.ID.eq(accountId))
            .execute();
    }
}
