package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.tables.records.UserAccountNumbersRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record7;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;

@Repository
@RequiredArgsConstructor
public class UserAccountNumberRepository {

    private final DSLContext dsl;

    public Result<UserAccountNumbersRecord> list() {
        return dsl.selectFrom(USER_ACCOUNT_NUMBERS)
                .fetch();
    }

    public Result<Record7<String, String, String, BigDecimal, UUID, String, LocalDateTime>> listWaitingValidation() {
        return dsl
            .select(
                USERS.USERNAME,
                MOVEMENTS.COUNTER_PARTY_NAME,
                MOVEMENTS.COMMUNICATION,
                MOVEMENTS.AMOUNT,
                USER_ACCOUNT_NUMBERS.USER_ID,
                USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER,
                USER_ACCOUNT_NUMBERS.ENCODING_DATE
            )
            .distinctOn(USERS.USERNAME, USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER)
            .from(USER_ACCOUNT_NUMBERS)
            .join(USERS)
                .onKey(Keys.USER_ACCOUNT_NUMBERS__USER_ACCOUNT_NUMBERS_USER_ID_FKEY)
            .leftJoin(MOVEMENTS)
                .on(USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER.eq(MOVEMENTS.COUNTER_PARTY_ACCOUNT_NUMBER))
            .where(USER_ACCOUNT_NUMBERS.VALIDATED.isFalse())
            .orderBy(USERS.USERNAME, USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER, USER_ACCOUNT_NUMBERS.ENCODING_DATE.desc())
            .fetch();
    }

    public void validate(List<String> numbers) {
        dsl.update(USER_ACCOUNT_NUMBERS)
            .set(USER_ACCOUNT_NUMBERS.VALIDATED, true)
            .where(USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER.in(numbers))
            .execute();
    }

    public Result<UserAccountNumbersRecord> forUser(UUID userId) {
        return dsl
            .selectFrom(USER_ACCOUNT_NUMBERS)
            .where(USER_ACCOUNT_NUMBERS.USER_ID.eq(userId))
            .fetch();
    }

    public void addAccountNumber(UUID userId, String iban) {
        dsl
            .insertInto(USER_ACCOUNT_NUMBERS)
            .columns(USER_ACCOUNT_NUMBERS.USER_ID, USER_ACCOUNT_NUMBERS.ACCOUNT_NUMBER)
            .values(userId, iban)
            .execute();
    }

    public int countPendingAccountNumbersForUser(UUID userId) {
        return dsl
            .selectCount()
            .from(USER_ACCOUNT_NUMBERS)
            .where(
                USER_ACCOUNT_NUMBERS.USER_ID.eq(userId)
                    .and(USER_ACCOUNT_NUMBERS.VALIDATED.isFalse())
            )
            .fetchOne()
            .value1();
    }
}
