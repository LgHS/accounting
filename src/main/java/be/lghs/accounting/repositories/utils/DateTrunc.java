package be.lghs.accounting.repositories.utils;

import org.jooq.Field;
import org.jooq.impl.DSL;

import java.time.LocalDate;

public class DateTrunc {

    public enum DateTruncUnit {
        DAYS,
        MONTHS,
        YEARS,
    }

    public static Field<LocalDate> dateTrunc(DateTruncUnit unit, Field<LocalDate> field) {
        return DSL.function(
            "date_trunc",
            LocalDate.class,
            DSL.inline(unit.name().toLowerCase()),
            field
        );
    }
}
