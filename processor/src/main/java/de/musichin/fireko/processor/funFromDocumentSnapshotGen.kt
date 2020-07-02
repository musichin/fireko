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
): PropertySpec = PropertySpec
    .builder(param.name, param.type)
    .initializer(generateInitializer(param, targets))
    .build()

private fun generateInitializer(
    param: TargetParameter,
    targets: List<TargetClass> = emptyList()
): CodeBlock {
    if (param.hasAnnotation(FIREBASE_DOCUMENT_ID)) {
        return CodeBlock.of("getId()") + param.convert(STRING)
    }

    val name = param.propertyName
    val type = param.type

    if (type is ClassName && targets.map { it.type }.contains(type.copy(nullable = false))) {
        // FIXME unsafe
        return CodeBlock.of(
            "(get(%S) as %T?)?.to%L()",
            param.name, MAP.parameterizedBy(ANY, ANY), type.simpleName
        )
    }

    val supportedSources = FIREBASE_SUPPORTED_TYPES intersect param.supportedSources
    if (supportedSources.isNotEmpty()) {
        val source = param.selectSource(supportedSources)
        return initializerByType(source, param)
    }

    return getBaseInitializer(name, type) + param.convert(type)
}

private fun initializerByType(type: TypeName, param: TargetParameter): CodeBlock {
    val source = type.copy(nullable = param.type.isNullable)
    return getBaseInitializer(param.propertyName, source) + param.convert(source)
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
    else -> throw IllegalArgumentException("Type $type is unsupported.")
}.wrapRequireNotNull(type.isNullable)
