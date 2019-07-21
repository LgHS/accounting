package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;
import static org.jooq.impl.DSL.extract;
import static org.jooq.impl.DSL.sum;

@Repository
@RequiredArgsConstructor
public class MovementRepository {

    private final DSLContext dsl;

    public Result<MovementCategoriesRecord> categories() {
        return dsl.selectFrom(MOVEMENT_CATEGORIES)
            .orderBy(MOVEMENT_CATEGORIES.NAME)
            .fetch();
    }

    private Result<MovementsRecord> find(Condition... conditions) {
    // public Result<MovementsRecord> findAll() {
    //     return dsl
    //        .select(
    //            MOVEMENTS.ID,
    //            MOVEMENTS.ENTRY_DATE,
    //            MOVEMENTS.COUNTER_PARTY_NAME,
    //            MOVEMENTS.COUNTER_PARTY_ACCOUNT_NUMBER,
    //            MOVEMENTS.AMOUNT,
    //            MOVEMENTS.COMMUNICATION,
    //            MOVEMENTS.CATEGORY_ID,
    //            MOVEMENT_CATEGORIES.NAME
    //        )
    //        .from(
    //            MOVEMENTS
    //                .leftJoin(MOVEMENT_CATEGORIES).onKey()
    //        )
    //        .orderBy(MOVEMENTS.ENTRY_DATE.desc())
    //        .fetch();
        return dsl.selectFrom(MOVEMENTS)
            .where(conditions)
            .orderBy(MOVEMENTS.ENTRY_DATE.desc())
            .fetch();
    }

    public Result<MovementsRecord> findAll() {
        return find();
    }

    public void createOne(MovementsRecord movement) {
        dsl.insertInto(MOVEMENTS)
            .set(movement)
            .returning()
            .execute();
    }

    public void setCategory(UUID movementId, UUID categoryId) {
        dsl.update(MOVEMENTS)
            .set(MOVEMENTS.CATEGORY_ID, categoryId)
            .where(MOVEMENTS.ID.eq(movementId))
            .execute();
    }

    public Result<Record5<Integer, String, String, String, BigDecimal>> legalSummary() {
        Field<Integer> entryDate = extract(MOVEMENTS.ENTRY_DATE, DatePart.YEAR);
        return dsl
            .select(
                entryDate.as("year"),
                MOVEMENT_CATEGORIES.TYPE,
                MOVEMENT_CATEGORIES.NAME.as("category"),
                ACCOUNTS.NAME,
                sum(MOVEMENTS.AMOUNT).as("amount")
            )
            .from(
                MOVEMENTS
                    .leftJoin(MOVEMENT_CATEGORIES).onKey()
                    .leftJoin(ACCOUNTS).onKey()
            )
            .groupBy(
                entryDate,
                MOVEMENT_CATEGORIES.TYPE,
                MOVEMENT_CATEGORIES.NAME,
                ACCOUNTS.NAME
            )
            .orderBy(
                entryDate.desc(),
                MOVEMENT_CATEGORIES.TYPE,
                MOVEMENT_CATEGORIES.NAME
            )
            .fetch();
    }

    public Result<MovementsRecord> findByAccount(UUID accountId) {
        return find(MOVEMENTS.ACCOUNT_ID.eq(accountId));
    }
}
