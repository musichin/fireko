package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun generateFunReceiverDocumentSnapshot(
    target: TargetClass,
    targets: List<TargetClass>
) = generateFun(target, targets, FIREBASE_DOCUMENT_SNAPSHOT, ::generateLocalProperty)

private fun generateLocalProperty(
    param: TargetParameter,
    targets: List<TargetClass>
): PropertySpec {
    var initializer = generateInitializer(param, targets)
    if (!param.type.isNullable) {
        initializer = CodeBlock.of("requireNotNull(%L)", initializer)
    }

    return PropertySpec
        .builder(param.name, param.type)
        .initializer(initializer)
        .build()
}

private fun generateInitializer(
    param: TargetParameter,
    targets: List<TargetClass>
): CodeBlock {
    if (param.hasAnnotation(FIREBASE_DOCUMENT_ID)) {
        return CodeBlock.of("getId()")
    }

    val name = param.propertyName
    val type = param.type

    val base = getBaseInitializer(name, type)
    if (base != null) {
        return base
    }

    if (type is ClassName && targets.map { it.type }.contains(type.copy(nullable = false))) {
        // FIXME unsafe
        return CodeBlock.builder()
            .add(
                "(get(%S) as %T?)?.to%L()",
                param.name, MAP.parameterizedBy(ANY, ANY), type.simpleName
            )
            .build()
    }

    return when (param.type.copy(nullable = false)) {
        BYTE -> CodeBlock.of("(%L)?.toByte()", getBaseInitializer(name, LONG))
        SHORT -> CodeBlock.of("(%L)?.toShort()", getBaseInitializer(name, LONG))
        INT -> CodeBlock.of("(%L)?.toInt()", getBaseInitializer(name, LONG))
        FLOAT -> CodeBlock.of("(%L)?.toFloat()", getBaseInitializer(name, DOUBLE))
        TIME_INSTANT,
        BP_INSTANT -> CodeBlock.builder()
            .beginControlFlow("(%L)?.let", getBaseInitializer(name, FIREBASE_TIMESTAMP))
            .add(
                "%T.ofEpochSecond(it.seconds, it.nanoseconds.toLong())",
                type.copy(nullable = false)
            )
            .endControlFlow()
            .build()
        BYTE_ARRAY -> CodeBlock.of("(%L)?.toBytes()", getBaseInitializer(name, FIREBASE_BLOB))
        else -> CodeBlock.of("get(%S, %T::class.java)", param.name, param.type)
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
