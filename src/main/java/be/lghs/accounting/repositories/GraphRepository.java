package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static be.lghs.accounting.model.Tables.MOVEMENTS;
import static be.lghs.accounting.model.Tables.MOVEMENT_CATEGORIES;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class GraphRepository {

    private final DSLContext dsl;

    public Result<Record2<LocalDate, BigDecimal>> rollingSum() {
        var innerSelect = dsl
            .select(
                MOVEMENTS.ENTRY_DATE,
                sum(MOVEMENTS.AMOUNT)
                    .over(
                        orderBy(MOVEMENTS.ENTRY_DATE)
                            .rangeBetweenUnboundedPreceding()
                            .andCurrentRow()
                    )
            )
            .from(MOVEMENTS)
            .asTable("x", "date", "amount");

        var startDate = LocalDate.now()
            .minusYears(2);

        return dsl
            .select(
                innerSelect.field("date", LocalDate.class),
                min(innerSelect.field("amount", BigDecimal.class))
            )
            .from(innerSelect)
            .where(innerSelect.field("date", LocalDate.class).greaterOrEqual(startDate))
            .groupBy(innerSelect.field("date"))
            .orderBy(1)
            .fetch()
            ;
    }

    public Result<Record3<LocalDate, BigDecimal, BigDecimal>> creditsPerDay() {
        var startDate = LocalDate.now()
            .withDayOfMonth(1)
            .minusMonths(2);

        return dsl
            .select(
                MOVEMENTS.ENTRY_DATE,
                sum(MOVEMENTS.AMOUNT)
                    .filterWhere(MOVEMENTS.AMOUNT.greaterThan(BigDecimal.ZERO)),
                sum(MOVEMENTS.AMOUNT)
                    .filterWhere(MOVEMENTS.AMOUNT.lessThan(BigDecimal.ZERO))
            )
            .from(MOVEMENTS)
            .leftJoin(MOVEMENT_CATEGORIES).onKey(Keys.MOVEMENTS__MOVEMENTS_CATEGORY_ID_FKEY)
            .where(
                MOVEMENTS.ENTRY_DATE.greaterOrEqual(startDate)
                    .and(
                        MOVEMENT_CATEGORIES.NAME.isNull()
                            .or(MOVEMENT_CATEGORIES.NAME.notIn("Crédit interne", "Débit interne"))
                    )
            )
            .groupBy(MOVEMENTS.ENTRY_DATE)
            .orderBy(MOVEMENTS.ENTRY_DATE)
            .fetch();
    }
}
