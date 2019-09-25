package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.model.enums.SubscriptionType;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.*;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepository {

    private final DSLContext dsl;

    public Result<Record6<UUID,
                          LocalDate,
                          LocalDate,
                          LocalDate,
                          String,
                          SubscriptionType>> findAll() {
        return dsl
            .select(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.START_DATE,
                SUBSCRIPTIONS.END_DATE,
                SUBSCRIPTIONS.END_DATE,
                USERS.USERNAME,
                SUBSCRIPTIONS.TYPE
            )
            .from(SUBSCRIPTIONS)
            .innerJoin(USERS).onKey(Keys.SUBSCRIPTIONS__SUBSCRIPTIONS_MEMBER_ID_FKEY)
            .orderBy(SUBSCRIPTIONS.START_DATE.desc())
            .fetch();
    }
}
