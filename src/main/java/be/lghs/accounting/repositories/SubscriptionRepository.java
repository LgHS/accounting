package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.model.tables.records.SubscriptionsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Record8;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private final DSLContext dsl;

    public Result<Record8<UUID,
                          LocalDate,
                          LocalDate,
                          LocalDate,
                          String,
                          String,
                          UUID,
                          SubscriptionType>> findAll() {
        return dsl
            .select(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE,
                SUBSCRIPTIONS.END_DATE,
                USERS.USERNAME,
                USERS.NAME,
                USERS.UUID,
                SUBSCRIPTIONS.TYPE
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .orderBy(SUBSCRIPTIONS.START_DATE.desc())
            .fetch();
    }
    //
    // public Record9<UUID,
    //                LocalDate,
    //                LocalDate,
    //                SubscriptionType,
    //                String,
    //                UUID,
    //                LocalDate,
    //                BigDecimal,
    //                String> getForMovement(UUID movementId) {
    public SubscriptionsRecord getForMovement(UUID movementId) {
        return dsl
            .selectFrom(SUBSCRIPTIONS)
            // .select(
            //     SUBSCRIPTIONS.ID,
            //     SUBSCRIPTIONS.START_DATE,
            //     SUBSCRIPTIONS.END_DATE,
            //     SUBSCRIPTIONS.TYPE,
            //     SUBSCRIPTIONS.COMMENT,
            //     SUBSCRIPTIONS.MOVEMENT_ID,
            //     MOVEMENTS.ENTRY_DATE,
            //     MOVEMENTS.AMOUNT,
            //     USERS.USERNAME
            // )
            // .from(SUBSCRIPTIONS)
            // .innerJoin(MOVEMENTS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            // .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .where(SUBSCRIPTIONS.MOVEMENT_ID.eq(movementId))
            .fetchOne();
    }

    public Result<SubscriptionsRecord> findSubscriptionsForMonthlyGraph(UUID userId, int months) {
        return dsl.selectFrom(SUBSCRIPTIONS)
            .where(
                SUBSCRIPTIONS.MEMBER_ID.eq(userId)
                    .and(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.MONTHLY))
                    .and(SUBSCRIPTIONS.START_DATE.greaterOrEqual(LocalDate.now().withDayOfMonth(1).minusMonths(months)))
            )
            .orderBy(SUBSCRIPTIONS.START_DATE)
            .fetch();
    }

    public Result<SubscriptionsRecord> findSubscriptionsForYearlyGraph(UUID userId) {
        return dsl.selectFrom(SUBSCRIPTIONS)
            .where(
                SUBSCRIPTIONS.MEMBER_ID.eq(userId)
                    .and(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.YEARLY))
            )
            .orderBy(SUBSCRIPTIONS.START_DATE)
            .limit(2)
            .fetch();
    }

    public Result<Record4<LocalDate, BigDecimal, String, SubscriptionType>> findLastSubscriptionsForUser(UUID userId) {
        return dsl
            .select(
                MOVEMENTS.ENTRY_DATE,
                MOVEMENTS.AMOUNT,
                MOVEMENTS.COMMUNICATION,
                SUBSCRIPTIONS.TYPE
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(MOVEMENTS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .where(
                SUBSCRIPTIONS.MEMBER_ID.eq(userId)
            )
            .orderBy(MOVEMENTS.ENTRY_DATE)
            .limit(10)
            .fetch();
    }
}
