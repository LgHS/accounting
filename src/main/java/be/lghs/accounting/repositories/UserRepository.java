package be.lghs.accounting.repositories;

import be.lghs.accounting.model.Tables;
import be.lghs.accounting.model.tables.records.UsersRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
}
