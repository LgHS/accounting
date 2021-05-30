package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.utils.DateTrunc;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

    public Result<MovementsRecord> findForMonth(YearMonth yearMonth) {
        var dateTrunc = DateTrunc.dateTrunc(DateTrunc.DateTruncUnit.MONTHS, MOVEMENTS.ENTRY_DATE);

        return find(dateTrunc.eq(yearMonth.atDay(1)));
    }

    public MovementsRecord getOne(UUID movementId) {
        return dsl.selectFrom(MOVEMENTS)
            .where(MOVEMENTS.ID.eq(movementId))
            .fetchOne();
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

        var dateTrunc = DateTrunc.dateTrunc(DateTrunc.DateTruncUnit.MONTHS, MOVEMENTS.ENTRY_DATE);
        return dsl
            .select(
                dateTrunc.as("date"),
                sum(MOVEMENTS.AMOUNT)
            )
            .from(MOVEMENTS)
            .leftJoin(MOVEMENT_CATEGORIES).onKey(Keys.MOVEMENTS__MOVEMENTS_CATEGORY_ID_FKEY)
            .where(
                MOVEMENTS.ENTRY_DATE.greaterOrEqual(firstMonth)
                    .and(MOVEMENT_CATEGORIES.NAME.notIn("Crédit interne", "Débit interne"))
            )
            .groupBy(field("date"))
            .orderBy(field("date").desc())
            .fetch()
            ;
    }

    public Result<Record5<LocalDate, Integer, Integer, Integer, BigDecimal>> monthsSummaries() {
        var month = DateTrunc.dateTrunc(DateTrunc.DateTruncUnit.MONTHS, MOVEMENTS.ENTRY_DATE);

        return dsl
            .select(
                month,
                count(asterisk()),
                count(asterisk()).filterWhere(MOVEMENTS.CATEGORY_ID.isNull()),
                count(asterisk()).filterWhere(SUBSCRIPTIONS.ID.isNull().and(MOVEMENT_CATEGORIES.NAME.eq("Cotisations"))),
                sum(MOVEMENTS.AMOUNT)
            )
            .from(MOVEMENTS)
            .leftJoin(MOVEMENT_CATEGORIES).onKey(Keys.MOVEMENTS__MOVEMENTS_CATEGORY_ID_FKEY)
            .leftJoin(SUBSCRIPTIONS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .groupBy(month)
            .orderBy(month.desc())
            .fetch();
    }

    public Result<MovementsRecord> missingCategories(YearMonth month) {
        return find(MOVEMENTS.CATEGORY_ID.isNull().and(MOVEMENTS.ENTRY_DATE.between(month.atDay(1), month.atEndOfMonth())));
    }

    public Result<MovementsRecord> missingSubscription(YearMonth month) {
        // in case you wonder, this is https://blog.jooq.org/2015/10/13/semi-join-and-anti-join-should-have-its-own-syntax-in-sql/
        // return dsl
        //     .selectFrom(
        //         MOVEMENTS
        //             .leftSemiJoin(MOVEMENT_CATEGORIES).on(
        //                 MOVEMENTS.CATEGORY_ID.eq(MOVEMENT_CATEGORIES.ID)
        //                     .and(MOVEMENT_CATEGORIES.NAME.eq("Cotisations"))
        //             )
        //             .leftAntiJoin(SUBSCRIPTIONS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
        //     )
        //     .where(
        //         MOVEMENTS.ENTRY_DATE.between(month.atDay(1), month.atEndOfMonth()))
        //     .fetch();

        return dsl
            .selectFrom(MOVEMENTS)
            .where(
                MOVEMENTS.CATEGORY_ID
                    .eq(
                        select(MOVEMENT_CATEGORIES.ID).from(MOVEMENT_CATEGORIES).where(MOVEMENT_CATEGORIES.NAME.eq("Cotisations"))
                    )
                    .and(MOVEMENTS.ENTRY_DATE.between(month.atDay(1), month.atEndOfMonth())))
            .and(notExists(
                select().from(SUBSCRIPTIONS).where(SUBSCRIPTIONS.MOVEMENT_ID.eq(MOVEMENTS.ID))
            ))
            .fetch();
    }

    public Result<MovementsRecord> unlinkedSubscriptions(UUID userId) {
        return dsl
            .select()
            .from(MOVEMENTS)
            .join(USER_ACCOUNT_NUMBERS)
                .on(USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER.eq(MOVEMENTS.COUNTER_PARTY_ACCOUNT_NUMBER))
            .where(USER_ACCOUNT_NUMBERS.VALIDATED)
            .fetchInto(MOVEMENTS);
    }
}
