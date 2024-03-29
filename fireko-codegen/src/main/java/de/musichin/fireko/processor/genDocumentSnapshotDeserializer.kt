package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun genDocumentSnapshotDeserializer(context: Context, target: TargetClass) = FunSpec
    .builder(toType(target.type))
    .addKdoc("Converts %T to %T.", FIREBASE_DOCUMENT_SNAPSHOT, target.type)
    .receiver(FIREBASE_DOCUMENT_SNAPSHOT)
    .returns(target.type.asNullable())
    .beginControlFlow("if (!exists())")
    .addStatement("return null")
    .endControlFlow()
    .addCode(CodeBlock.builder().generateBody(context, target).build())
    .build()

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.generateBody(
    context: Context,
    target: TargetClass
): CodeBlock.Builder = apply {
    val params = target.params
    genProperties(context, params)

    genTargetClassInitializer(target)
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.genProperties(
    context: Context,
    params: List<TargetParameter>
) {
    params.forEach { param ->
        add("%L", generateLocalProperty(context, param))
    }
}

@KotlinPoetMetadataPreview
private fun generateLocalProperty(context: Context, param: TargetParameter): PropertySpec =
    PropertySpec
        .builder(param.name, param.propertyType)
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
            .add("this%L", invokeToType(type.asNotNullable()))
            .convert(type.asNullable(), type.asNullable())
            .assertNotNull(param)
            .build()
    }

    val adapter = param.usingAdapter?.let { context.adapterElement(it) }
        ?: context.getAnnotatedAdapter(type)
    if (adapter != null) {
        return if (adapter.readFunSpec != null) {
            val sourceType = adapter.readFunSpec.parameters.first().type
            CodeBlock.builder()
                .add(getBaseInitializer(context, name, sourceType))
                .deserialize(context, sourceType.asNullable(), true)
                .add("?.let(%L::%L)", adapter.className, adapter.readFunSpec.name)
                .assertNotNull(param)
                .build()
        } else {
            CodeBlock.of("Adapter should contain read method.")
        }
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, type))
        .deserialize(context, type.asNullable(), true)
        .assertNotNull(param)
        .build()
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.assertNotNull(param: TargetParameter) = apply {
    if (!param.type.isNullable && !param.hasDefaultValue) {
        add(
            "?: throw NullPointerException(%S)",
            "Property ${param.propertyName} is absent or null."
        )
    }
}

@KotlinPoetMetadataPreview
private fun getBaseInitializer(context: Context, name: String, type: TypeName) =
    when (ValueType.valueOf(context, type)) {
        null -> CodeBlock.of("get(%S)", name)
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
            CodeBlock.of("(get(%S) as %T)", name, ValueType.typeOf(context, type).asNullable())
    }

@KotlinPoetMetadataPreview
private val TargetParameter.propertyType: TypeName
    get() = type.copy(nullable = type.isNullable || hasDefaultValue)

@KotlinPoetMetadataPreview
private val TargetClass.resultName: String
    get() = findFreeParamName("result")
