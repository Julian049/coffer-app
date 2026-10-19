package com.dev.julian09.financeapp.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** data-model §5.1: instants as epoch milliseconds (UTC), UUIDs as text, dates as ISO text (it sorts). */
class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun uuidToText(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun textToUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
