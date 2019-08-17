package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Keys;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;

import static be.lghs.accounting.model.Tables.ACCOUNTS;
import static be.lghs.accounting.model.Tables.MOVEMENTS;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class GraphRepository {

    private final DSLContext dsl;

    public Result<Record2<Date, BigDecimal>> rollingSum() {
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
            .asTable("x", "date", "amount")
            ;

        return dsl
            .select(
                innerSelect.field("date", Date.class),
                min(innerSelect.field("amount", BigDecimal.class))
            )
            .from(innerSelect)
            .groupBy(innerSelect.field("date"))
            .orderBy(1)
            .fetch()
            ;
    }
}
