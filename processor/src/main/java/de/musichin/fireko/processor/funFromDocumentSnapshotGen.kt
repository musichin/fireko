package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun generateFunReceiverDocumentSnapshot(
    target: TargetType,
    targets: List<TargetType>
) = generateFun(target, targets, FIREBASE_DOCUMENT_SNAPSHOT, ::generateLocalProperty)

private fun generateLocalProperty(
    target: TargetType,
    property: ParameterSpec,
    targets: List<TargetType>
): PropertySpec {
    var initializer = generateInitializer(target, property, targets)
    if (!property.type.isNullable) {
        initializer = CodeBlock.of("requireNotNull(%L)", initializer)
    }

    return PropertySpec
        .builder(property.name, property.type)
        .initializer(initializer)
        .build()
}

private fun generateInitializer(
    target: TargetType,
    property: ParameterSpec,
    targets: List<TargetType>
): CodeBlock {
    if (property.propertyAnnotations(target.typeSpec).any { it.typeName == FIREBASE_DOCUMENT_ID }) {
        return CodeBlock.of("getId()")
    }

    val name = propertyName(target, property)
    val type = property.type

    val base = getBaseInitializer(name, type)
    if (base != null) {
        return base
    }

    if (type is ClassName && targets.map { it.type }.contains(type.copy(nullable = false))) {
        // FIXME unsafe
        return CodeBlock.builder()
            .add(
                "(get(%S) as %T?)?.to%L()",
                property.name, MAP.parameterizedBy(ANY, ANY), type.simpleName
            )
            .build()
    }

    return when (property.type.copy(nullable = false)) {
        BYTE -> CodeBlock.of("(%L)?.toByte()", getBaseInitializer(name, LONG))
        SHORT -> CodeBlock.of("(%L)?.toShort()", getBaseInitializer(name, LONG))
        INT -> CodeBlock.of("(%L)?.toInt()", getBaseInitializer(name, LONG))
        FLOAT -> CodeBlock.of("(%L)?.toFloat()", getBaseInitializer(name, DOUBLE))
        TIME_INSTANT,
        BP_INSTANT -> CodeBlock.builder()
            .beginControlFlow("(%L)?.let", getBaseInitializer(name, FIREBASE_TIMESTAMP))
            .add("%T.ofEpochSecond(it.seconds, it.nanoseconds.toLong())", type.copy(nullable = false))
            .endControlFlow()
            .build()
        BYTE_ARRAY -> CodeBlock.of("(%L)?.toBytes()", getBaseInitializer(name, FIREBASE_BLOB))
        else -> CodeBlock.of("get(%S, %T::class.java)", property.name, property.type)
        // LIST
        // MAP
    }
}

private fun getBaseInitializer(name: String, type: TypeName) = when (type.copy(nullable = false)) {
    BOOLEAN,
    STRING,
    LONG,
    DOUBLE,
    FIREBASE_TIMESTAMP,
    FIREBASE_BLOB,
    FIREBASE_GEO_POINT,
    FIREBASE_DOCUMENT_REFERENCE,
    UTIL_DATE -> CodeBlock.of("get%L(%S)", (type as ClassName).simpleName, name)
    else -> null
}
