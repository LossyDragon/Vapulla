package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.ELicenseType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import java.util.EnumSet

/**
 * JavaSteam's license enums, stored as Steam's own numbers. A number this version of
 * JavaSteam does not know reads as the enum's "none" value.
 */
class LicenseTypeConverters {
    @TypeConverter
    fun fromLicenseType(value: ELicenseType): Int = value.code()

    @TypeConverter
    fun toLicenseType(code: Int): ELicenseType = ELicenseType.from(code) ?: ELicenseType.NoLicense

    @TypeConverter
    fun fromPaymentMethod(value: EPaymentMethod): Int = value.code()

    @TypeConverter
    fun toPaymentMethod(code: Int): EPaymentMethod =
        EPaymentMethod.from(code) ?: EPaymentMethod.None

    @TypeConverter
    fun fromLicenseFlags(value: EnumSet<ELicenseFlags>): Int = ELicenseFlags.code(value)

    @TypeConverter
    fun toLicenseFlags(code: Int): EnumSet<ELicenseFlags> = ELicenseFlags.from(code)
}
