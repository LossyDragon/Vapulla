package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import java.util.Date

/** Dates are stored as epoch milliseconds. */
class DateTypeConverter {
    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(value: Long): Date = Date(value)
}
