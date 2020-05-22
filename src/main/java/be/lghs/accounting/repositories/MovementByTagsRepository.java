package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import be.lghs.accounting.repositories.utils.DateTrunc;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Record4;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static be.lghs.accounting.model.Tables.MOVEMENTS;
import static be.lghs.accounting.model.Tables.MOVEMENT_TAGS;
import static org.jooq.impl.DSL.sum;

@Repository
@RequiredArgsConstructor
public class MovementByTagsRepository {

    private final DSLContext dsl;

    public Result<Record4<String, BigDecimal, BigDecimal, BigDecimal>> summary() {
        return dsl
            .select(
                MOVEMENT_TAGS.CONTENT,
                sum(MOVEMENTS.AMOUNT),
                sum(MOVEMENTS.AMOUNT)
                    .filterWhere(
                        MOVEMENTS.ENTRY_DATE.between(
                            LocalDate.now().withDayOfYear(1).minusYears(1),
                            LocalDate.now().withDayOfYear(1).minusDays(1)
                        )
                    ),
                sum(MOVEMENTS.AMOUNT)
                    .filterWhere(
                        MOVEMENTS.ENTRY_DATE.ge(LocalDate.now().withDayOfYear(1))
                    )
            )
            .from(MOVEMENT_TAGS)
            .innerJoin(MOVEMENTS).onKey(Keys.MOVEMENT_TAGS__MOVEMENT_TAGS_MOVEMENT_ID_FKEY)
            .groupBy(MOVEMENT_TAGS.CONTENT)
            .orderBy(MOVEMENT_TAGS.CONTENT)
            .fetch();
    }

    public Result<Record3<LocalDate, String, BigDecimal>> summary(DateTrunc.DateTruncUnit unit) {
        var dateTrunc = DateTrunc.dateTrunc(unit, MOVEMENTS.ENTRY_DATE);

        return dsl
            .select(
                dateTrunc,
                MOVEMENT_TAGS.CONTENT,
                sum(MOVEMENTS.AMOUNT)
            )
            .from(MOVEMENTS)
            .leftJoin(MOVEMENT_TAGS).onKey(Keys.MOVEMENT_TAGS__MOVEMENT_TAGS_MOVEMENT_ID_FKEY)
            .groupBy(
                dateTrunc,
                MOVEMENT_TAGS.CONTENT
            )
            .orderBy(
                dateTrunc.desc(),
                MOVEMENT_TAGS.CONTENT
            )
            .fetch()
            ;
    }
}
