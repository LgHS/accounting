package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.MovementsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import static be.lghs.accounting.model.Tables.MOVEMENTS;

@Repository
@RequiredArgsConstructor
public class MovementRepository {

    private final DSLContext dsl;

    public Result<MovementsRecord> findAll() {
        return dsl.selectFrom(MOVEMENTS)
            .orderBy(MOVEMENTS.ENTRY_DATE.desc())
            .fetch();
    }

    public void createOne(MovementsRecord movement) {
        dsl.insertInto(MOVEMENTS)
            .set(movement)
            .returning()
            .execute();
    }
}
