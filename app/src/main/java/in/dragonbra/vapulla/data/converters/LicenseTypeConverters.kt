package `in`.dragonbra.vapulla.data.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.ELicenseType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import java.util.Date
import java.util.EnumSet

class LicenseTypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromLicenseType(value: ELicenseType?): Int? {
        return value?.code()
    }

    @TypeConverter
    fun toLicenseType(code: Int?): ELicenseType? {
        return code?.let { ELicenseType.from(it) }
    }

    @TypeConverter
    fun fromPaymentMethod(value: EPaymentMethod?): Int? {
        return value?.code()
    }

    @TypeConverter
    fun toPaymentMethod(code: Int?): EPaymentMethod? {
        return code?.let { EPaymentMethod.from(it) }
    }

    @TypeConverter
    fun fromLicenseFlags(value: EnumSet<ELicenseFlags>?): Int? {
        return value?.let { ELicenseFlags.code(it) }
    }

    @TypeConverter
    fun toLicenseFlags(code: Int?): EnumSet<ELicenseFlags>? {
        return code?.let { ELicenseFlags.from(it) }
    }
}