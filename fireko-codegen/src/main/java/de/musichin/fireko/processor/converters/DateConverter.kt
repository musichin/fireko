package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.Converter
import de.musichin.fireko.processor.FIREBASE_TIMESTAMP
import de.musichin.fireko.processor.ValueType
import de.musichin.fireko.processor.call
import de.musichin.fireko.processor.isFirebaseTimestamp
import de.musichin.fireko.processor.isOneOf

internal object DateConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isFirebaseTimestamp() && target.isOneOf(DATE) -> {
            call(source)
            add("toDate()")
        }

        source.isOneOf(DATE) && target.isFirebaseTimestamp() -> {
            call(source)
            add("let(::%T)", FIREBASE_TIMESTAMP)
        }

        source.isOneOf(DATE) && target.isOneOf(LONG) -> {
            call(source)
            add("time")
        }

        source.isOneOf(LONG) && target.isOneOf(DATE) -> {
            call(source)
            add("let(::%T)", DATE)
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.TIMESTAMP, ValueType.INTEGER)

    override fun create(type: TypeName): Converter? {
        if (type.isOneOf(DATE)) return DateConverter

        return null
    }

    private val DATE = ClassName("java.util", "Date")
}
