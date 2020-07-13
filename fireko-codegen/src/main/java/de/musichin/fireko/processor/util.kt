package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

operator fun CodeBlock.plus(other: CodeBlock): CodeBlock {
    return CodeBlock.Builder().add(this).add(other).build()
}

fun List<AnnotationSpec>.get(typeName: TypeName): AnnotationSpec? =
    find { it.typeName == typeName }

fun TypeName.isAssignable(initializer: TypeName): Boolean {
    if (this == initializer) return true
    if (this.notNullable != initializer.notNullable) return false
    return this.isNullable
}

val TypeName.notNullable: TypeName get() = copy(nullable = false)
val TypeName.nullable: TypeName get() = copy(nullable = true)