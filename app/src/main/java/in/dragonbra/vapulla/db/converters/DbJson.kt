package `in`.dragonbra.vapulla.db.converters

import kotlinx.serialization.json.Json

/**
 * For the JSON columns. A row written by an older version of the app may hold fields that
 * have since been removed, so unknown ones are skipped instead of failing the read.
 */
internal val DbJson = Json { ignoreUnknownKeys = true }
