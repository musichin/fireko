package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun generateFunReceiverMap(
    target: TargetClass,
    targets: List<TargetClass>
) = generateFun(
    target,
    targets,
    MAP.parameterizedBy(ANY, ANY.copy(nullable = true)),
    ::generateLocalProperty
)

private fun generateLocalProperty(
    param: TargetParameter,
    targets: List<TargetClass>
): PropertySpec {
    var initializer = generateInitializer(param, targets)
    if (!param.type.isNullable) {
        initializer = CodeBlock.builder().add("requireNotNull(%L)", initializer).build()
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
    val name = param.propertyName
    val type = param.type

    val base = getBaseInitializer(name, type)
    if (base != null) {
        return base
    }

    if (type is ClassName && targets.map { it.type }.contains(type.copy(nullable = false))) {
        // FIXME unsafe
        return CodeBlock.of(
            "(%L)?.to%L()",
            getBaseInitializer(name, MAP.parameterizedBy(ANY, ANY)),
            (type).simpleName
        )
    }

    return when (type.copy(nullable = false)) {
        BYTE -> CodeBlock.of("(%L)?.toByte()", getBaseInitializer(name, LONG))
        SHORT -> CodeBlock.of("(%L)?.toShort()", getBaseInitializer(name, LONG))
        INT -> CodeBlock.of("(%L)?.toInt()", getBaseInitializer(name, LONG))
        FLOAT -> CodeBlock.of("(%L)?.toFloat()", getBaseInitializer(name, DOUBLE))
        UTIL_DATE -> CodeBlock.of("(%L)?.toDate()", getBaseInitializer(name, FIREBASE_TIMESTAMP))
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
        else -> CodeBlock.of("TODO()")
        // FIXME LIST
        // FIXME MAP
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
    MAP,
    MAP.parameterizedBy(STRING, ANY),
    MAP.parameterizedBy(STRING, ANY.copy(nullable = false)),
    MAP.parameterizedBy(ANY, ANY),
    MAP.parameterizedBy(ANY, ANY.copy(nullable = true)),
    LIST,
    LIST.parameterizedBy(ANY) -> CodeBlock.of("get(%S) as %T", name, type.copy(nullable = true))
    else -> null
}
