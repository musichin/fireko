package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.Converter
import de.musichin.fireko.processor.ValueType
import de.musichin.fireko.processor.call
import de.musichin.fireko.processor.isOneOf

internal object AndroidNetUriConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isOneOf(STRING) && target.isOneOf(URI) -> {
            call(source)
            add("let(%T::parse)", URI)
        }
        source.isOneOf(URI) && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.STRING)

    override fun create(type: TypeName): Converter? {
        if (type.isOneOf(URI)) return AndroidNetUriConverter

        return null
    }

    private val URI = ClassName("android.net", "Uri")
}
