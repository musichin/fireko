package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun generateFunReceiverMap(target: TargetClass) = generateFun(
    target,
    MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
    ::generateLocalProperty
)

private fun generateLocalProperty(param: TargetParameter): PropertySpec {
    return PropertySpec
        .builder(param.name, param.type)
        .initializer(generateInitializer(param))
        .build()
}

private fun generateInitializer(param: TargetParameter): CodeBlock {
    val name = param.propertyName
    val type = param.type

    if (param.embedded) {
        return CodeBlock.of("this%L", invokeToType(param.type as ClassName))
    }

    val supportedSources = FIREBASE_SUPPORTED_TYPES intersect param.supportedSources
    if (supportedSources.isNotEmpty()) {
        val source = param.selectSource(supportedSources)
        return initializerByType(source, param)
    }

    return getBaseInitializer(name, type)
}

private fun initializerByType(type: TypeName, param: TargetParameter): CodeBlock {
    val source = type.copy(nullable = param.type.isNullable)
    return getBaseInitializer(param.propertyName, source) + param.convert(source)
}

private fun getBaseInitializer(name: String, type: TypeName): CodeBlock =
    when (type.copy(nullable = false)) {
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
        else -> throw IllegalArgumentException("Unsupported type $type")
    }.let {
        if (type.isNullable) {
            CodeBlock.of("(%L)", it)
        } else {
            CodeBlock.of("requireNotNull(%L)", it)
        }
    }
