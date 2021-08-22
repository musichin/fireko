package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.Converter
import de.musichin.fireko.processor.FIREBASE_TIMESTAMP
import de.musichin.fireko.processor.ValueType
import de.musichin.fireko.processor.asNotNullable
import de.musichin.fireko.processor.call
import de.musichin.fireko.processor.isFirebaseTimestamp
import de.musichin.fireko.processor.isOneOf

internal sealed class Jsr310Converter : Converter() {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isOneOf(CHAR_SEQUENCE, STRING) && target.isIso() -> {
            call(source)
            add("let(%T::parse)", target.asNotNullable())
        }
        source.isIso() && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        source.isFirebaseTimestamp() && target.isInstant() -> {
            call(source)
            add(
                "let { %T.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }",
                target.asNotNullable()
            )
        }
        source.isInstant() && target.isFirebaseTimestamp() -> {
            call(source)
            add("let { %T(it.epochSecond, it.nano) }", FIREBASE_TIMESTAMP)
        }

        source.isInstant() && target.isOneOf(LONG) -> {
            call(source)
            add("toEpochMilli")
        }

        source.isOneOf(LONG) && target.isInstant() -> {
            add("let(%T::ofEpochMilli)", target.asNotNullable())
        }

        else -> throwConversionError(source, target)
    }

    object Factory : Converter.Factory {
        override fun create(type: TypeName): Converter? {
            if (type.isInstant()) return InstantConverter
            if (type.isZoneId()) return ZoneConverter
            if (type.isZoneOffset()) return ZoneConverter
            if (type.isIso()) return IsoConverter

            return null
        }
    }

    object InstantConverter : Jsr310Converter() {
        override val valueTypes: List<ValueType> = listOf(
            ValueType.TIMESTAMP,
            ValueType.INTEGER,
            ValueType.STRING
        )
    }

    object IsoConverter : Jsr310Converter() {
        override val valueTypes: List<ValueType> = listOf(ValueType.STRING)
    }

    object ZoneConverter : Jsr310Converter() {
        override val valueTypes: List<ValueType> = listOf(
            ValueType.STRING
        )

        override fun CodeBlock.Builder.convert(
            source: TypeName,
            target: TypeName
        ): CodeBlock.Builder = when {
            source.isOneOf(STRING) && (target.isZoneId() || target.isZoneOffset()) -> {
                call(source)
                add("let(%T::of)", target.asNotNullable())
            }
            (source.isZoneId() || source.isZoneOffset()) && target.isOneOf(STRING) -> {
                call(source)
                add("toString()")
            }

            else -> throwConversionError(source, target)
        }
    }

    companion object {
        private fun TypeName.isSupportedPkg() =
            this is ClassName && this.packageName in listOf("java.time", "org.threeten.bp")

        private fun TypeName.isSupportedClass(simpleName: String) =
            this is ClassName && this.isSupportedPkg() && this.simpleName == simpleName

        private fun TypeName.isZoneId() = isSupportedClass("ZoneId")
        private fun TypeName.isZoneOffset() = isSupportedClass("ZoneOffset")

        private fun TypeName.isInstant() = isSupportedClass("Instant")

        private fun TypeName.isLocalDateTime() = isSupportedClass("LocalDateTime")
        private fun TypeName.isLocalDate() = isSupportedClass("LocalDate")
        private fun TypeName.isLocalTime() = isSupportedClass("LocalTime")

        private fun TypeName.isOffsetDateTime() = isSupportedClass("OffsetDateTime")
        private fun TypeName.isOffsetTime() = isSupportedClass("OffsetTime")

        private fun TypeName.isZonedDateTime() = isSupportedClass("ZonedDateTime")

        private fun TypeName.isPeriod() = isSupportedClass("Period")

        private fun TypeName.isDuration() = isSupportedClass("Duration")

        private fun TypeName.isIso() = isInstant() ||
            isLocalDateTime() || isLocalDate() || isLocalTime() ||
            isOffsetDateTime() || isOffsetTime() ||
            isZonedDateTime() ||
            isPeriod() ||
            isDuration()
    }
}
