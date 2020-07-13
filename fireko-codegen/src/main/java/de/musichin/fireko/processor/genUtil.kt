package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

internal fun invokeToType(type: ClassName, vararg arguments: String): CodeBlock =
    invokeToType(type.isNullable, type.simpleName, *arguments)

fun invokeToType(nullable: Boolean, type: String, vararg arguments: String): CodeBlock =
    CodeBlock.Builder().invokeToType(nullable, type, *arguments).build()

fun CodeBlock.Builder.invokeToType(
    nullable: Boolean,
    type: String,
    vararg arguments: String
): CodeBlock.Builder = apply {
    call(nullable)
    add(toType(type))
    add("(${arguments.joinToString(",")})")
}

internal fun CodeBlock.Builder.call(safe: Boolean = false) = apply {
    if (safe) {
        add("?")
    }
    add(".")
}

internal fun CodeBlock.Builder.call(type: TypeName) = call(type.isNullable)

internal fun toType(type: String) = "to${type.capitalize()}"
internal fun toType(type: ClassName) = toType(type.simpleName)
