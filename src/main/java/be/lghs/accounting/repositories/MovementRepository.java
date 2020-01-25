package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class MovementRepository {

    private final DSLContext dsl;

    public Result<MovementCategoriesRecord> categories() {
        return dsl.selectFrom(MOVEMENT_CATEGORIES)
            .orderBy(MOVEMENT_CATEGORIES.NAME)
            .fetch();
    }

    public Map<UUID, String> categoryNamesById() {
        return dsl.selectFrom(MOVEMENT_CATEGORIES)
            .orderBy(MOVEMENT_CATEGORIES.NAME)
            .fetchMap(MOVEMENT_CATEGORIES.ID, MOVEMENT_CATEGORIES.NAME);
    }

    private Result<MovementsRecord> find(Condition... conditions) {
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
            .orderBy(
                MOVEMENTS.ENTRY_DATE.desc(),
                MOVEMENTS.CODA_SEQUENCE_NUMBER.desc(),
                MOVEMENTS.AMOUNT.desc(),
                MOVEMENTS.COMMUNICATION)
            .fetch();
    }

    public Result<MovementsRecord> findAll() {
        return find(
            MOVEMENTS.ENTRY_DATE.greaterOrEqual(LocalDate.now().withDayOfYear(1).minusYears(1))
        );
    }

    public void createOne(MovementsRecord movement) {
        dsl.insertInto(MOVEMENTS)
            .set(movement)
            .returning()
            .execute();
    }

    public MovementsRecord addMovement(UUID accountId, BigDecimal amount, String communication, LocalDate date) {
        return dsl.insertInto(MOVEMENTS)
                .columns(MOVEMENTS.AMOUNT, MOVEMENTS.ENTRY_DATE, MOVEMENTS.ACCOUNT_ID, MOVEMENTS.COMMUNICATION)
                .values(amount, date, accountId, communication)
                .returning(MOVEMENTS.ID)
                .fetchOne();
    }

    public void setCategory(UUID movementId, UUID categoryId) {
        dsl.update(MOVEMENTS)
            .set(MOVEMENTS.CATEGORY_ID, categoryId)
            .where(MOVEMENTS.ID.eq(movementId))
            .execute();
    }

    public Result<Record4<Integer, String, String, BigDecimal>> legalSummary() {
        Field<Integer> entryDate = extract(MOVEMENTS.ENTRY_DATE, DatePart.YEAR);
        return dsl
            .select(
                entryDate.as("year"),
                MOVEMENT_CATEGORIES.TYPE,
                MOVEMENT_CATEGORIES.NAME.as("category"),
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
                MOVEMENT_CATEGORIES.NAME
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

    public MovementsRecord getOne(UUID movementId) {
        return dsl.selectFrom(MOVEMENTS)
            .where(MOVEMENTS.ID.eq(movementId))
            .fetchOne();
    }

    public Result<MovementCategoriesRecord> credits() {
        return dsl.selectFrom(MOVEMENT_CATEGORIES)
            .where(MOVEMENT_CATEGORIES.TYPE.eq("CREDIT"))
            .fetch();
    }

    public Result<MovementCategoriesRecord> debits() {
        return dsl.selectFrom(MOVEMENT_CATEGORIES)
            .where(MOVEMENT_CATEGORIES.TYPE.eq("DEBIT"))
            .fetch();
    }

    public void insertFromTemplate(MovementsRecord originMovement,
                                   String communication,
                                   BigDecimal amount,
                                   UUID categoryId) {
        MovementsRecord copy = originMovement.copy();
        copy.setCommunication(communication);
        copy.setAmount(amount);
        copy.setCategoryId(categoryId);

        copy.insert();
    }

    public Result<MovementsRecord> findFromCounterParty(String iban) {
        return find(MOVEMENTS.COUNTER_PARTY_ACCOUNT_NUMBER.eq(iban));
    }

    public Result<MovementsRecord> findByCategory(UUID categoryId) {
        return find(MOVEMENTS.CATEGORY_ID.eq(categoryId));
    }

    public Result<Record2<LocalDate, BigDecimal>> amountsPerMonth() {
        var firstMonth = LocalDate.now()
            .withDayOfMonth(1)
            .minusMonths(6);
        var date_trunc = function("date_trunc", LocalDate.class, val("months"), MOVEMENTS.ENTRY_DATE);
        return dsl
            .select(
                date_trunc.as("date"),
                sum(MOVEMENTS.AMOUNT)
            )
            .from(MOVEMENTS)
            .where(MOVEMENTS.ENTRY_DATE.greaterOrEqual(firstMonth))
            .groupBy(field("date"))
            .orderBy(field("date").desc())
            .fetch()
            ;
    }
}
