package de.musichin.fireko.processor.converters

import com.squareup.kotlinpoet.*
import de.musichin.fireko.processor.*

internal object CurrencyConverter : Converter(), Converter.Factory {
    override fun CodeBlock.Builder.convert(
        source: TypeName,
        target: TypeName
    ): CodeBlock.Builder = when {
        source.isOneOf(STRING) && target.isCurrency() -> {
            call(source)
            add("let(%T::getInstance)", target.notNullable())
        }
        source.isCurrency() && target.isOneOf(CHAR_SEQUENCE, STRING) -> {
            call(source)
            add("toString()")
        }

        else -> throwConversionError(source, target)
    }

    override val valueTypes: List<ValueType> = listOf(ValueType.STRING)

    override fun create(type: TypeName): Converter? {
        if (type.isCurrency()) return CurrencyConverter

        return null
    }

    private fun TypeName.isCurrency(): Boolean {
        if (this !is ClassName) return false
        if (this.simpleName != "Currency") return false
        return this.packageName in listOf("java.util", "android.icu.util")
    }
}
