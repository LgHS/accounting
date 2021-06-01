package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.Tables;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.model.enums.UserRole;
import be.lghs.accounting.model.tables.records.UsersRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Record7;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;
import static be.lghs.accounting.model.tables.Users.USERS;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;

    public UsersRecord ensureUserExists(int id, UUID uuid, String name, String username, String email) {
        return dsl.insertInto(Tables.USERS)
            .columns(USERS.ID, USERS.UUID, USERS.NAME, USERS.USERNAME, USERS.EMAIL)
            .values(id, uuid, name, username, email)
            .onDuplicateKeyUpdate()
            .set(USERS.NAME, name)
            .set(USERS.USERNAME, username)
            .set(USERS.EMAIL, email)
            .returning(USERS.asterisk())
            .fetchOne();
    }

    // public Optional<UsersRecord> findByUsername(String username) {
    //     return dsl.selectFrom(Tables.USERS)
    //         .where(USERS.USERNAME.eq(username))
    //         .fetchOptional();
    // }

    public Optional<UsersRecord> findById(UUID id) {
        return dsl.selectFrom(Tables.USERS)
            .where(USERS.UUID.eq(id))
            .fetchOptional();
    }

    public Result<UsersRecord> findAll() {
        return dsl.selectFrom(Tables.USERS)
            .orderBy(
                USERS.ROLES,
                USERS.NAME
            )
            .fetch();
    }

    public Record7<UUID, LocalDate, LocalDate, Integer, BigDecimal, LocalDate, LocalDate> statistics(UUID userId) {
        return dsl
            .select(
                USERS.UUID,
                DSL.min(MOVEMENTS.ENTRY_DATE).as("firstSeen"),
                DSL.max(MOVEMENTS.ENTRY_DATE).as("lastSeen"),
                DSL.count(MOVEMENTS.ID).as("movementCount"),
                DSL.sum(MOVEMENTS.AMOUNT).as("totalAmount"),
                DSL.max(SUBSCRIPTIONS.END_DATE).filterWhere(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.MONTHLY)).as("endMonthly"),
                DSL.max(SUBSCRIPTIONS.END_DATE).filterWhere(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.YEARLY)).as("endYearly")
            )
            .from(USERS)
            .leftJoin(USER_ACCOUNT_NUMBERS)
                .on(USER_ACCOUNT_NUMBERS.USER_ID.eq(USERS.UUID).and(USER_ACCOUNT_NUMBERS.VALIDATED))
            .leftJoin(MOVEMENTS)
                .on(USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER.eq(MOVEMENTS.COUNTER_PARTY_ACCOUNT_NUMBER))
            .leftJoin(SUBSCRIPTIONS)
                .onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MOVEMENT_ID_FKEY)
            .where(
                USERS.UUID.eq(userId)
            )
            .groupBy(USERS.UUID)
            .fetchOne();
    }

    public List<String> findAdminEmails() {
        return dsl
            .select(USERS.EMAIL)
            .from(USERS)
            .where(
                DSL.val(UserRole.ROLE_ADMIN).eq(DSL.any(USERS.ROLES))
            )
            .fetch(USERS.EMAIL);
    }

    public Result<Record4<UUID, String, LocalDate, LocalDate>> findUsersWithLastSubscriptions() {
        return dsl
            .select(
                USERS.UUID,
                USERS.USERNAME,
                DSL.max(SUBSCRIPTIONS.END_DATE).filterWhere(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.MONTHLY)).as("last_monthly"),
                DSL.max(SUBSCRIPTIONS.END_DATE).filterWhere(SUBSCRIPTIONS.TYPE.eq(SubscriptionType.YEARLY)).as("last_yearly")
            )
            .from(USERS)
            .leftJoin(SUBSCRIPTIONS)
                .onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .groupBy(USERS.UUID, USERS.USERNAME)
            .orderBy(USERS.USERNAME)
            .fetch()
            ;
    }
}
