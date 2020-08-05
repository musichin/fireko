package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
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

    when {
        params.none(TargetParameter::hasDefaultValue) ->
            genSimpleInitializer(target)
        target.typeSpec.isData ->
            getDataInitializer(target)
        else ->
            getClassInitializer(target)
    }
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.getClassInitializer(target: TargetClass) = apply {
    val params = target.params
    val requiredParams = target.params.filter { !it.hasDefaultValue }

    addStatement("var %L = %T(", target.resultName, target.type.asNotNullable())
    paramBlock(requiredParams) { param ->
        addStatement("%L = %L,", param.name, param.name)
    }
    addStatement(")")

    addStatement("%L = %T(", target.resultName, target.type.asNotNullable())
    paramBlock(params) { param ->
        val name = param.name

        if (param.hasDefaultValue) {
            paramValueOrDefault(param, target)
        } else {
            addStatement("%L = %L,", name, name)
        }
    }
    addStatement(")")

    addStatement("return %L", target.resultName)
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.getDataInitializer(target: TargetClass) = apply {
    val params = target.params
    val requiredParams = params.filter { !it.hasDefaultValue }
    val defaultParams = target.params.filter { it.hasDefaultValue }

    add("var %L = %T(\n", target.resultName, target.type.asNotNullable())
    paramBlock(requiredParams) { param ->
        addStatement("%L = %L,", param.name, param.name)
    }
    add(")\n")

    add("%L = %L.copy(\n", target.resultName, target.resultName)
    paramBlock(defaultParams) { param ->
        paramValueOrDefault(param, target)
    }
    add(")\n")

    addStatement("return %L", target.resultName)
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.paramValueOrDefault(
    param: TargetParameter,
    target: TargetClass
) {
    val name = param.name
    val th = CodeBlock.builder().assertNullability(param, true).build()
    addStatement(
        "%L = if (%L != null || contains(%S))\n⇥%L %L⇤\nelse\n⇥%L.%L,⇤",
        name, name, param.propertyName, name, th, target.resultName, name
    )
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.genSimpleInitializer(target: TargetClass) = apply {
    val params = target.params

    add("return %T(\n", target.type.asNotNullable())
    paramBlock(params) { param ->
        val name = param.name
        addStatement("%L = %L,", name, name)
    }
    add(")\n")
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
            .assertNullability(param)
            .build()
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, param.type))
        .deserialize(context, param.type.asNullable(), true)
        .assertNullability(param)
        .build()
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.assertNullability(param: TargetParameter, force: Boolean = false) = apply {
    if (!param.type.isNullable && !param.hasDefaultValue || force) {
        add(" ?: throw NullPointerException(%S)", "Property ${param.propertyName} is absent or null.")
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
