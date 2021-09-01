package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.NUMBER
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.Converter
import de.musichin.fireko.processor.ValueType
import de.musichin.fireko.processor.call
import de.musichin.fireko.processor.invokeToType
import de.musichin.fireko.processor.isOneOf
import de.musichin.fireko.processor.isString

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

        if (source.isNumber() && target.isOneOf(CHAR)) {
            return invokeToType(source.isNullable, INT.simpleName)
                .invokeToType(source.isNullable, CHAR.simpleName)
        }

        if (source.isOneOf(CHAR) && target.isNumber()) {
            return call().add("code")
                .invokeToType(source.isNullable, (target as ClassName).simpleName)
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
