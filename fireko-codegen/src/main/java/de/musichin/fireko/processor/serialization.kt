package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
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
            type as ClassName

            convert(type.copy(nullable = nullable), type)

            if (context.isEnum(type)) {
                deserializeEnum(context, type)
            }
        }
        ValueType.ARRAY -> {
            type as ParameterizedTypeName

            convert(type.copy(nullable = nullable), type)

            // TODO optimizations
            val parametrizedType = type.typeArguments.first()
            if (!parametrizedType.isAny()) {
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

                add(invokeToType(type.notNullable()))
            } else {
                type as ParameterizedTypeName

                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]

                // TODO optimizations
                if (!keyType.isString()) {
                    call(type)
                    add("mapKeys { (key, _) -> key")
                    deserialize(context, keyType, false)
                    add(" }")
                }

                call(type)
                add("mapValues { (_, value) -> value")
                deserialize(context, valueType, false)
                add(" }")
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
            .add("let { %T.valueOf(it) }", target.notNullable())
    }

    call(target)
    beginControlFlow("let")
    beginControlFlow("when (it)")
    propertyNames.forEach { (name, propertyName) ->
        addStatement("%S -> %T.%L", propertyName, target, name)
    }
    addStatement("else -> %T.valueOf(it)", target.notNullable())
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
            type as ClassName

            if (context.isEnum(type)) {
                serializeEnum(context, type)
            }
        }
        ValueType.ARRAY -> {
            type as ParameterizedTypeName

            // TODO optimizations
            val parametrizedType = type.typeArguments.first()
            if (!parametrizedType.isAny()) {
                call(type)
                add("map { it")
                serialize(context, parametrizedType.notNullable())
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

                // TODO optimizations
                if (!keyType.isString()) {
                    call(type)
                    add("mapKeys { (key, _) -> key")
                    serialize(context, keyType.notNullable())
                    add(" }")
                }

                call(type)
                add("mapValues { (_, value) -> value")
                serialize(context, valueType.notNullable())
                add(" }")
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
