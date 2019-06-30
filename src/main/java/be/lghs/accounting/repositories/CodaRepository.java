package be.lghs.accounting.repositories;

import be.lghs.accounting.model.tables.records.CodasRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static be.lghs.accounting.model.Tables.CODAS;

@Repository
@RequiredArgsConstructor
public class CodaRepository {

    private final DSLContext dsl;

    public Result<CodasRecord> findAll() {
        return dsl.selectFrom(CODAS)
            .orderBy(CODAS.FILENAME.desc())
            .fetch();
    }

    public void createOne(String name, String description) {
        dsl.insertInto(
                CODAS//,
                // CODAS.NAME,
                // CODAS.DESCRIPTION
            )
            .values(name, description)
            .execute();

    }

    public UUID createOne(CodasRecord coda) {
        return dsl.insertInto(CODAS)
            .set(coda)
            .returning(CODAS.ID)
            .fetchOne()
            .getId();
    }
}
