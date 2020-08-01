package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun genFunDocumentSnapshotToObject(context: Context, target: TargetClass) = FunSpec
    .builder(toType(target.type))
    .addKdoc("Converts %T to %T.", FIREBASE_DOCUMENT_SNAPSHOT, target.type)
    .receiver(FIREBASE_DOCUMENT_SNAPSHOT)
    .returns(target.type.nullable())
    .beginControlFlow("if (!exists())")
    .addStatement("return null")
    .endControlFlow()
    .apply {
        val params = target.params
        params.forEach { parameter ->
            addCode("%L", generateLocalProperty(context, parameter))
        }

        val paramNames = params.map { it.name }.joinToString(", ") { "$it = $it" }

        addCode("return ${target.simpleName}($paramNames)")
    }
    .build()

@KotlinPoetMetadataPreview
private fun generateLocalProperty(context: Context, param: TargetParameter): PropertySpec = PropertySpec
    .builder(param.name, param.type)
    .initializer(generateInitializer(context, param))
    .build()

@KotlinPoetMetadataPreview
private fun generateInitializer(context: Context, param: TargetParameter): CodeBlock {
    val name = param.propertyName
    val type = param.type

    if (param.documentId) {
        return CodeBlock.builder().add("getId()").convert(STRING, type).build()
    }

    if (param.embedded) {
        type as ClassName
        return CodeBlock.builder()
            .add("this%L", invokeToType(type.notNullable()))
            .convert(type.nullable(), type)
            .build()
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, param.type))
        .deserialize(context, param.type, true)
        .build()
}

@KotlinPoetMetadataPreview
private fun getBaseInitializer(context: Context, name: String, type: TypeName) =
    when (ValueType.valueOf(context, type)) {
        ValueType.BOOLEAN -> CodeBlock.of("getBoolean(%S)", name)
        ValueType.INTEGER -> CodeBlock.of("getLong(%S)", name)
        ValueType.DOUBLE -> CodeBlock.of("getDouble(%S)", name)
        ValueType.STRING -> CodeBlock.of("getString(%S)", name)
        ValueType.BYTES -> CodeBlock.of("getBlob(%S)", name)
        ValueType.GEO_POINT -> CodeBlock.of("getGeoPoint(%S)", name)
        ValueType.REFERENCE -> CodeBlock.of("getDocumentReference(%S)", name)
        ValueType.TIMESTAMP -> CodeBlock.of("getTimestamp(%S)", name)
        ValueType.MAP,
        ValueType.ARRAY ->
            CodeBlock.of("(get(%S) as %T)", name, ValueType.typeOf(context, type).nullable())
    }

