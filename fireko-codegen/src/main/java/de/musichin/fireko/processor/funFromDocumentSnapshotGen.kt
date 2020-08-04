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
    .addCode(CodeBlock.builder().generateBody(context, target).build())
    .build()

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.generateBody(
    context: Context,
    target: TargetClass
): CodeBlock.Builder = apply {
    val params = target.params
    genProperties(context, params)

    genReturns(target)
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.genReturns(
    target: TargetClass,
    index: Int = 0,
    excludedParams: List<TargetParameter> = emptyList()
) {
    val paramsWithDefault = target.params.filter(TargetParameter::hasDefaultValue)
    val param = paramsWithDefault.getOrNull(index)

    if (param == null) {
        val includedParams = paramsWithDefault - excludedParams
        includedParams.forEach { includedParam ->
            addStatement("requireNotNull(%L)", includedParam.name)
        }
        genReturn(target.params - excludedParams, target)
        return
    }

    beginControlFlow("if (%L != null || contains(%S))", param.name, param.propertyName)
    genReturns(target, index + 1, excludedParams)
    nextControlFlow("else")
    genReturns(target, index + 1, excludedParams + param)
    endControlFlow()
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.genReturn(params: List<TargetParameter>, target: TargetClass) {
    val paramNames = params.map(TargetParameter::name).joinToString(", ") { name ->
        "$name = $name"
    }

    addStatement("return %T($paramNames)", target.type.notNullable())
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
            .add("this%L", invokeToType(type.notNullable()))
            .convert(type.nullable(), type)
            .build()
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, param.type))
        .deserialize(context, param.propertyType, true)
        .build()
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
            CodeBlock.of("(get(%S) as %T)", name, ValueType.typeOf(context, type).nullable())
    }

@KotlinPoetMetadataPreview
private val TargetParameter.propertyType: TypeName
    get() = type.copy(nullable = type.isNullable || hasDefaultValue)