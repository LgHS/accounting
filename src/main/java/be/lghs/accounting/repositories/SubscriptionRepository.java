package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.model.tables.records.SubscriptionsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private static final Condition ONE_EQUALS_ONE = DSL.val(1).eq(1);

    private final DSLContext dsl;

    public Result<Record10<UUID,
                          LocalDate,
                          LocalDate,
                          String,
                          String,
                          UUID,
                          SubscriptionType,
                          LocalDate,
                          BigDecimal,
                          Boolean>> findAll(SubscriptionType type) {
        var query = dsl
            .select(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE,
                USERS.USERNAME,
                USERS.NAME,
                USERS.UUID,
                SUBSCRIPTIONS.TYPE,
                MOVEMENTS.ENTRY_DATE,
                MOVEMENTS.AMOUNT,
                field(MOVEMENTS.COUNTER_PARTY_NAME.likeIgnoreCase("sumup %")).as("sumup")
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(MOVEMENTS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY);


        SelectOrderByStep<Record10<UUID, LocalDate, LocalDate, String, String, UUID, SubscriptionType, LocalDate, BigDecimal, Boolean>> where = query;
        if (type != null) {
            where = query.where(SUBSCRIPTIONS.TYPE.eq(type));
        }

        return where
            .orderBy(SUBSCRIPTIONS.START_DATE.desc())
            .fetch();
    }

     public Record9<UUID,
                    LocalDate,
                    LocalDate,
                    SubscriptionType,
                    String,
                    UUID,
                    LocalDate,
                    BigDecimal,
                    String> getForMovement(UUID movementId) {
    // public SubscriptionsRecord getForMovement(UUID movementId) {
        return dsl
            // .selectFrom(SUBSCRIPTIONS)
            .select(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE,
                SUBSCRIPTIONS.TYPE,
                SUBSCRIPTIONS.COMMENT,
                SUBSCRIPTIONS.MOVEMENT_ID,
                MOVEMENTS.ENTRY_DATE,
                MOVEMENTS.AMOUNT,
                USERS.USERNAME
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(MOVEMENTS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
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

    public Result<SubscriptionsRecord> findSubscriptionsForYearlyGraph(UUID userId, LocalDate activeAfterThisDate) {
        return dsl.selectFrom(SUBSCRIPTIONS)
            .where(
                SUBSCRIPTIONS.MEMBER_ID.eq(userId)
                    .and(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.YEARLY))
                    .and(SUBSCRIPTIONS.END_DATE.ge(activeAfterThisDate))
            )
            .orderBy(SUBSCRIPTIONS.START_DATE)
            .fetch();
    }

    public Result<Record6<LocalDate, BigDecimal, String, SubscriptionType, LocalDate, LocalDate>> findLastSubscriptionsForUser(UUID userId,
                                                                                                         boolean loadAllPayments) {
        var query = dsl
            .select(
                MOVEMENTS.ENTRY_DATE,
                MOVEMENTS.AMOUNT,
                MOVEMENTS.COMMUNICATION,
                SUBSCRIPTIONS.TYPE,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(MOVEMENTS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .where(
                SUBSCRIPTIONS.MEMBER_ID.eq(userId)
            )
            .orderBy(MOVEMENTS.ENTRY_DATE.desc());

        ResultQuery<Record6<LocalDate, BigDecimal, String, SubscriptionType, LocalDate, LocalDate>> result = query;
        if (!loadAllPayments) {
            result = query.limit(10);
        }

        return result.fetch();
    }

    public LocalDate getLastSubscription(UUID userId, SubscriptionType type) {
        AggregateFunction<LocalDate> max = max(SUBSCRIPTIONS.END_DATE);

        return dsl
            .select(max)
            .from(SUBSCRIPTIONS)
            .where(
                SUBSCRIPTIONS.TYPE.eq(type)
                    .and(SUBSCRIPTIONS.MEMBER_ID.eq(userId))
            )
            .fetchOne(max);
    }

    public void linkMovement(UUID encoderId, UUID movementId, String username, SubscriptionType type, LocalDate startDate, LocalDate endDate, String comment) {
        dsl.insertInto(SUBSCRIPTIONS)
            .columns(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.MOVEMENT_ID,
                SUBSCRIPTIONS.MEMBER_ID,
                SUBSCRIPTIONS.ENCODER_ID,
                SUBSCRIPTIONS.TYPE,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE,
                SUBSCRIPTIONS.COMMENT
            )
            .select(
                select(
                        inline(UUID.randomUUID()), // todo, default id to uuid_generate_v4(), this is ugly
                        inline(movementId),
                        USERS.UUID,
                        inline(encoderId),
                        inline(type),
                        inline(startDate),
                        inline(endDate),
                        inline(comment)
                    )
                    .from(USERS)
                    .where(USERS.USERNAME.eq(username))
            )
            // .values(
            //     movementId,
            //     select(USERS.UUID)
            //         .from(USERS)
            //         .where(USERS.USERNAME.eq("username")),
            //     encoderId,
            //     type,
            //     startDate,
            //     endDate
            // )
            .execute();
    }
}
