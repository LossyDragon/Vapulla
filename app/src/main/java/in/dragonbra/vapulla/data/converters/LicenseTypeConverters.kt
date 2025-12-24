package `in`.dragonbra.vapulla.data.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.ELicenseType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import java.util.Date
import java.util.EnumSet

class LicenseTypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromLicenseType(value: ELicenseType?): Int? = value?.code()

    @TypeConverter
    fun toLicenseType(code: Int?): ELicenseType? = code?.let { ELicenseType.from(it) }

    @TypeConverter
    fun fromPaymentMethod(value: EPaymentMethod?): Int? = value?.code()

    @TypeConverter
    fun toPaymentMethod(code: Int?): EPaymentMethod? = code?.let { EPaymentMethod.from(it) }

    @TypeConverter
    fun fromLicenseFlags(value: EnumSet<ELicenseFlags>?): Int? =
        value?.let { ELicenseFlags.code(it) }

    @TypeConverter
    fun toLicenseFlags(code: Int?): EnumSet<ELicenseFlags>? = code?.let { ELicenseFlags.from(it) }
}
