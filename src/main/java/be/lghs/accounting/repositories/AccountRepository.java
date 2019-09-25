package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.ACCOUNTS;


//
// Should probably run this every once in a while to notify treasurers when current_balance is wrong
//
// update accounting.accounts updated_table
// set current_balance = (
//     select coalesce(sum(amount), 0)
//     from accounting.movements
//     where account_id = updated_table.id
// )
// from accounting.accounts old_values
// where updated_table.id = old_values.id
// returning old_values.current_balance, updated_table.current_balance;
// ;
//

@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dsl;

    public Result<AccountsRecord> findAll() {
        return dsl
            .selectFrom(ACCOUNTS)
            .orderBy(ACCOUNTS.NAME)
            .fetch();
    }

    public void createOne(String name, String description) {
        dsl
            .insertInto(
                ACCOUNTS,
                ACCOUNTS.NAME,
                ACCOUNTS.DESCRIPTION
            )
            .values(name, description)
            .execute();

    }

    public Optional<AccountsRecord> findOne(UUID id) {
        return dsl
            .selectFrom(ACCOUNTS)
            .where(ACCOUNTS.ID.eq(id))
            .fetchOptional();
    }

    public int update(UUID accountId, String name, String description) {
        return dsl
            .update(ACCOUNTS)
            .set(ACCOUNTS.NAME, name)
            .set(ACCOUNTS.DESCRIPTION, description)
            .where(ACCOUNTS.ID.eq(accountId))
            .execute();
    }

    public void updateBalance(UUID accountId, BigDecimal added) {
        dsl
            .update(ACCOUNTS)
            .set(ACCOUNTS.CURRENT_BALANCE, ACCOUNTS.CURRENT_BALANCE.plus(added))
            .where(ACCOUNTS.ID.eq(accountId))
            .execute();
    }

    public BigDecimal globalBalance() {
        return dsl
            .select(DSL.sum(
                ACCOUNTS.CURRENT_BALANCE
            ))
            .from(ACCOUNTS)
            .fetchOne()
            .component1();
    }
}
