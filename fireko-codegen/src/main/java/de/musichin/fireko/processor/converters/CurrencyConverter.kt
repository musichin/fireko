package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.*
import de.musichin.fireko.processor.*

internal object CurrencyConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isOneOf(STRING) && target.isOneOf(CURRENCY) -> {
            call(source)
            add("let(%T::getInstance)", target.notNullable())
        }
        source.isOneOf(CURRENCY) && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.STRING)

    override fun create(type: TypeName): Converter? {
        if (type.isOneOf(CURRENCY)
        ) return CurrencyConverter

        return null
    }

    private val CURRENCY =
        ClassName("java.util", "Currency")
}
