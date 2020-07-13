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
        return CodeBlock.builder().add("getId()").convert(STRING, type).build()
    }

    if (param.embedded) {
        return CodeBlock.of("this%L", param.convertFrom(FIREBASE_DOCUMENT_SNAPSHOT))
    }

    val source = param.selectSource(FIREBASE_SUPPORTED_TYPES)
    return CodeBlock.builder()
        .add(getBaseInitializer(name, source))
        .add(param.convertFrom(source))
        .build()
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
    UTIL_DATE ->
        CodeBlock.of("get%L(%S)", (type as ClassName).simpleName, name)
    MAP,
    MAP.parameterizedBy(STRING, ANY),
    MAP.parameterizedBy(STRING, ANY),
    MAP.parameterizedBy(STRING, ANY.copy(nullable = false)),
    MAP.parameterizedBy(ANY, ANY),
    MAP.parameterizedBy(ANY, ANY.copy(nullable = true)) ->
        CodeBlock.of("(get(%S) as %T)", name, type)
    else -> throw IllegalArgumentException("Type $type is unsupported.")
}
