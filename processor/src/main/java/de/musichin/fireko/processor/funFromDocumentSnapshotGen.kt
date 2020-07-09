package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun generateFunReceiverDocumentSnapshot(target: TargetClass) = FunSpec
    .builder(toType(target.type))
    .addKdoc("Converts %T to %T.", FIREBASE_DOCUMENT_SNAPSHOT, target.type)
    .receiver(FIREBASE_DOCUMENT_SNAPSHOT)
    .returns(target.type)
    .apply {
        val params = target.params
        params.forEach { parameter ->
            addCode("%L", generateLocalProperty(parameter))
        }

        val paramNames = params.map { it.name }.joinToString(", ") { "$it = $it" }

        addCode("return ${target.simpleName}($paramNames)")
    }
    .build()

private fun generateLocalProperty(param: TargetParameter): PropertySpec = PropertySpec
    .builder(param.name, param.type)
    .initializer(generateInitializer(param))
    .build()

private fun generateInitializer(param: TargetParameter): CodeBlock {
    val name = param.propertyName
    val type = param.type

    if (param.documentId) {
        return CodeBlock.of("getId()") + param.convertFrom(STRING)
    }

    if (param.embedded) {
        return CodeBlock.of("this%L", param.convertFrom(FIREBASE_DOCUMENT_SNAPSHOT))
    }

    val supportedSources = FIREBASE_SUPPORTED_TYPES intersect param.supportedSources
    if (supportedSources.isNotEmpty()) {
        val source = param.selectSource(supportedSources)
        return initializerByType(source, param)
    }

    return getBaseInitializer(name, type) + param.convertFrom(type)
}

private fun initializerByType(type: TypeName, param: TargetParameter): CodeBlock {
    val source = type.copy(nullable = param.type.isNullable)
    return getBaseInitializer(param.propertyName, source) + param.convertFrom(source)
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
    UTIL_DATE -> {
        val init = CodeBlock.of("get%L(%S)", (type as ClassName).simpleName, name)
        if (type.isNullable) {
            init
        } else {
            CodeBlock.of("requireNotNull(%L)", init)
        }
    }
    MAP,
    MAP.parameterizedBy(STRING, ANY),
    MAP.parameterizedBy(STRING, ANY),
    MAP.parameterizedBy(STRING, ANY.copy(nullable = false)),
    MAP.parameterizedBy(ANY, ANY),
    MAP.parameterizedBy(ANY, ANY.copy(nullable = true)) -> {
        val init = CodeBlock.of("get(%S) as Map<String, Any?>", name)
        if (type.isNullable) {
            CodeBlock.of("(%L)", init)
        } else {
            CodeBlock.of("requireNotNull(%L)", init)
        }
    }
    else -> throw IllegalArgumentException("Type $type is unsupported.")
}
