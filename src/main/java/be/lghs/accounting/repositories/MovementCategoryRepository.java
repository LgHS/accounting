package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import static be.lghs.accounting.model.Tables.MOVEMENT_CATEGORIES;

@Repository
@RequiredArgsConstructor
public class MovementCategoryRepository {

    private final DSLContext dsl;

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
}
