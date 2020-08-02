package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.*
import de.musichin.fireko.processor.*

internal object JavaNetUriConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isOneOf(STRING) && target.isOneOf(URI, URL) -> {
            call(source)
            add("let(::%T)", target.notNullable())
        }
        source.isOneOf(URI, URL) && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        source.isOneOf(URI) && target.isOneOf(URL) -> {
            call(source)
            add("toURL()")
        }

        source.isOneOf(URL) && target.isOneOf(URI) -> {
            call(source)
            add("toURI()")
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.STRING)

    override fun create(type: TypeName): Converter? {
        if (type.isOneOf(URI, URL)) return JavaNetUriConverter

        return null
    }

    private val URI = ClassName("java.net", "URI")
    private val URL = ClassName("java.net", "URL")
}
