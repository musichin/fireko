package de.musichin.fireko.processor

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import de.musichin.fireko.processor.converters.*
import de.musichin.fireko.processor.converters.NumberConverter

internal abstract class Converter {
    fun convert(
        source: TypeName,
        target: TypeName,
        b: CodeBlock.Builder
    ): CodeBlock.Builder = b.convert(source, target)

    open fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = throwConversionError(source, target)

    val valueType: ValueType get() = valueTypes.first()
    abstract val valueTypes: List<ValueType>

    protected fun throwConversionError(source: TypeName, target: TypeName): Nothing =
        throw IllegalArgumentException("Cannot convert from $source to $target")

    interface Factory {
        fun create(type: TypeName): Converter?
    }
}

private val factories = listOf(
    NumberConverter.Factory,
    CharSequenceConverter,
    Jsr310Converter.Factory,
    DateConverter,
    CurrencyConverter,
    JavaNetUriConverter,
    AndroidNetUriConverter
)

private val converters = mutableMapOf<TypeName, Converter?>()

private fun createConverter(target: TypeName): Converter? {
    for (factory in factories) {
        return factory.create(target) ?: continue
    }
    return null
}

internal fun getConverter(target: TypeName): Converter? =
    converters.getOrPut(target) { createConverter(target) }

internal fun CodeBlock.Builder.convert(
    source: TypeName,
    target: TypeName
): CodeBlock.Builder = when {
    target.isAssignable(source) -> this

    source.isNullable && !target.isNullable -> {
        add(".let(::requireNotNull)")
        convert(source.asNotNullable(), target)
    }

    target.isAny() -> this

    getConverter(target) != null -> getConverter(target)!!.convert(source, target, this)
    getConverter(source) != null -> getConverter(source)!!.convert(source, target, this)

    else -> throwConversionError(source, target)
}

private fun throwConversionError(source: TypeName, target: TypeName): Nothing =
    throw IllegalArgumentException("Cannot convert from $source to $target")
