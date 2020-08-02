package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.Converter
import de.musichin.fireko.processor.ValueType
import de.musichin.fireko.processor.call
import de.musichin.fireko.processor.isOneOf
import de.musichin.fireko.processor.isString

internal object CharSequenceConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isString() && target.isOneOf(CHAR_SEQUENCE, STRING) ->
            this

        source.isOneOf(CHAR_SEQUENCE, STRING) && target.isString() -> {
            call(source)
            add("toString()")
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.STRING)

    override fun create(type: TypeName): Converter? {
        if (type.isOneOf(CHAR_SEQUENCE)) return CharSequenceConverter

        return null
    }
}
