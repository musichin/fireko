package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import de.musichin.fireko.processor.TargetParameter.Companion.propertyName

@KotlinPoetMetadataPreview
internal fun CodeBlock.Builder.deserialize(
    context: Context,
    type: TypeName,
    nullable: Boolean
): CodeBlock.Builder = apply {
    when (ValueType.valueOf(context, type)) {
        ValueType.STRING -> {
            if (context.isEnum(type)) {
                type as ClassName

                convert(type.copy(nullable = nullable), type)

                deserializeEnum(context, type)
            } else {
                convert(ValueType.typeOf(context, type).copy(nullable = nullable), type)
            }
        }
        ValueType.ARRAY -> {
            type as ParameterizedTypeName

            convert(type.copy(nullable = nullable), type)

            val parametrizedType = type.typeArguments.first()
            if (peek { deserialize(context, parametrizedType, false) }.isNotEmpty()) {
                call(type)
                add("map { it")
                deserialize(context, parametrizedType, false)
                add(" }")
            }
        }
        ValueType.MAP -> {
            convert(type.copy(nullable = nullable), type)

            if (context.isPojo(type)) {
                type as ClassName

                add(invokeToType(type.nullable(nullable)))
            } else {
                type as ParameterizedTypeName

                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]

                if (!keyType.isString()) {
                    call(type)
                    add("mapKeys { (key, _) -> key")
                    deserialize(context, keyType, false)
                    add(" }")
                }

                if (peek { deserialize(context, valueType, false) }.isNotEmpty()) {
                    call(type)
                    add("mapValues { (_, value) -> value")
                    deserialize(context, valueType, false)
                    add(" }")
                }
            }
        }
        else -> convert(ValueType.typeOf(context, type).copy(nullable = nullable), type)
    }
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.deserializeEnum(context: Context, target: ClassName) = apply {
    val typeSpec = context.typeSpec(target)
        ?: throw IllegalArgumentException("Could not get type spec for $target")

    val propertyNames = enumPropertyNames(typeSpec)

    if (propertyNames.isEmpty()) {
        return call(target)
            .add("let { %T.valueOf(it) }", target.asNotNullable())
    }

    call(target)
    beginControlFlow("let")
    beginControlFlow("when (it)")
    propertyNames.forEach { (name, propertyName) ->
        addStatement("%S -> %T.%L", propertyName, target.asNotNullable(), name)
    }
    addStatement("else -> %T.valueOf(it)", target.asNotNullable())
    endControlFlow()
    endControlFlow()
}

@KotlinPoetMetadataPreview
internal fun CodeBlock.Builder.serialize(
    context: Context,
    type: TypeName
): CodeBlock.Builder = apply {
    when (ValueType.valueOf(context, type)) {
        ValueType.STRING -> {
            if (context.isEnum(type)) {
                type as ClassName

                serializeEnum(context, type)
            } else {
                convert(type, ValueType.typeOf(context, type).copy(nullable = type.isNullable))
            }
        }
        ValueType.ARRAY -> {
            type as ParameterizedTypeName

            val parametrizedType = type.typeArguments.first()
            if (peek { serialize(context, parametrizedType.asNotNullable()) }.isNotEmpty()) {
                call(type)
                add("map { it")
                serialize(context, parametrizedType.asNotNullable())
                add(" }")
            }
        }
        ValueType.MAP -> {
            if (context.isPojo(type)) {
                type as ClassName

                call(type.isNullable)
                add("toMap()")
            } else {
                type as ParameterizedTypeName

                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]

                if (!keyType.isString()) {
                    call(type)
                    add("mapKeys { (key, _) -> key")
                    serialize(context, keyType.asNotNullable())
                    add(" }")
                }

                if (peek { serialize(context, valueType.asNotNullable()) }.isNotEmpty()) {
                    call(type)
                    add("mapValues { (_, value) -> value")
                    serialize(context, valueType.asNotNullable())
                    add(" }")
                }
            }
        }
        else -> convert(type, ValueType.typeOf(context, type).copy(nullable = type.isNullable))
    }
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.serializeEnum(context: Context, type: ClassName) = apply {
    val typeSpec = context.typeSpec(type)
        ?: throw IllegalArgumentException("Could not get type spec for $type")

    val propertyNames: Map<String, String> = enumPropertyNames(typeSpec)

    if (propertyNames.isEmpty()) {
        return call(type).add("name")
    }

    call(type)
    beginControlFlow("let")
    beginControlFlow("when (it)")
    propertyNames.forEach { (name, propertyName) ->
        addStatement("%T.%L -> %S", type, name, propertyName)
    }
    addStatement("else -> it.name")
    endControlFlow()
    endControlFlow()
}

@KotlinPoetMetadataPreview
private fun enumPropertyNames(typeSpec: TypeSpec): Map<String, String> {
    return typeSpec.enumConstants
        .mapNotNull { (name, spec) -> spec.annotationSpecs.propertyName()?.let { name to it } }
        .toMap()
}

private fun peek(block: CodeBlock.Builder.() -> CodeBlock.Builder): CodeBlock {
    return CodeBlock.builder().block().build()
}
