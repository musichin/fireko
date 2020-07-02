package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

internal fun CodeBlock.wrapRequireNotNull(nullable: Boolean): CodeBlock =
    if (nullable) this
    else CodeBlock.of("requireNotNull(%L)", this)

internal fun invokeToType(type: ClassName): CodeBlock =
    invokeToType(type.isNullable, type.simpleName)

fun invokeToType(nullable: Boolean, type: String): CodeBlock =
    CodeBlock.Builder().call(nullable).add(toType(type)).add("()").build()

internal fun CodeBlock.Builder.call(safe: Boolean = false) = apply {
    if (safe) {
        add("?")
    }
    add(".")
}

internal fun CodeBlock.Builder.call(type: TypeName) = call(type.isNullable)

internal fun toType(type: String) = "to${type.capitalize()}"
internal fun toType(type: ClassName) = toType(type.simpleName)
