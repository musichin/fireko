package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

operator fun CodeBlock.plus(other: CodeBlock): CodeBlock {
    return CodeBlock.Builder().add(this).add(other).build()
}

fun List<AnnotationSpec>.get(typeName: TypeName): AnnotationSpec? =
    find { it.typeName == typeName }