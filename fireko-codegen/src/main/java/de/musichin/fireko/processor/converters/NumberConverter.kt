package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.*
import de.musichin.fireko.processor.*
import de.musichin.fireko.processor.ValueType

internal abstract class NumberConverter : Converter() {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isNumber() && target.isNumber() ->
            convertNumber(source, target)

        source.isString() && target.isNumber() ->
            convertNumber(source, target)

        source.isNumber() && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        else -> throwConversionError(source, target)
    }

    private fun CodeBlock.Builder.convertNumber(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder {
        if (source.copy(nullable = false) == target.copy(nullable = false)) {
            return this
        }

        if (source.isNumber() && target.isOneOf(NUMBER)) {
            return this
        }

        if (source.isNumber()) {
            return invokeToType(source.isNullable, (target as ClassName).simpleName)
        }

        if (source.isOneOf(STRING)) {
            return invokeToType(source.isNullable, (target as ClassName).simpleName)
        }

        // TODO Rational
        // TODO BigDecimal

        throwConversionError(source, target)
    }

    private object DoubleNumberConverter : NumberConverter() {
        override val valueTypes: List<ValueType> = listOf(ValueType.DOUBLE, ValueType.STRING)
    }

    private object IntegerNumberConverter : NumberConverter() {
        override val valueTypes: List<ValueType> = listOf(ValueType.INTEGER, ValueType.STRING)
    }

    object Factory : Converter.Factory {
        override fun create(type: TypeName): Converter? {
            if (type.isFloating()) return DoubleNumberConverter
            if (type.isOneOf(NUMBER)) return DoubleNumberConverter
            else if (type.isNumber()) return IntegerNumberConverter

            return null
        }
    }

    companion object {
        fun TypeName.isNumber(): Boolean =
            isOneOf(NUMBER, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)

        fun TypeName.isFloating(): Boolean =
            isOneOf(FLOAT, DOUBLE)
    }
}
