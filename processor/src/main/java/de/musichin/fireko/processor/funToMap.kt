package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun generateFunTargetMap(target: TargetClass) = FunSpec
    .builder("toMap")
    .addKdoc("Converts %T to %T.", target.type, MAP.parameterizedBy(STRING, ANY))
    .receiver(target.type)
    .returns(MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
    .body(target)
    .build()

@KotlinPoetMetadataPreview
private fun FunSpec.Builder.body(target: TargetClass) = apply {
    val params = target.params
    val documentedIdParams = target.documentIdParams
    val propertyParams = (params - documentedIdParams)

    if (propertyParams.isEmpty()) {
        addCode("return emptyMap()")
    } else {
        beginControlFlow("return mutableMapOf<%T, %T>().apply", STRING, ANY.copy(nullable = true))
        putParams(propertyParams)
        endControlFlow()
    }
}

@KotlinPoetMetadataPreview
private fun FunSpec.Builder.putParams(params: List<TargetParameter>) = apply {
    params.forEach { param ->
        addCode(putter(param))
    }
}

@KotlinPoetMetadataPreview
private fun putter(param: TargetParameter): CodeBlock = CodeBlock.builder()
    .add("\n// %L\n", param.name)
    .add("%L", getter(param))
    .call(param.type)
    .beginControlFlow("also")
    .putParam(param)
    .endControlFlow()
    .apply {
        if (param.serverTimestamp && param.type.isNullable) {
            addStatement(
                "?: put(%S, %T.serverTimestamp())",
                param.propertyName,
                FIREBASE_FIELD_VALUE
            )
        }
    }
    .build()

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.putParam(param: TargetParameter): CodeBlock.Builder =
    if (param.embedded) {
        add("putAll(%L)\n", getter(param))
    } else {
        add("put(%S, it)\n", param.propertyName)
    }

@KotlinPoetMetadataPreview
private fun getter(param: TargetParameter): CodeBlock =
    CodeBlock.of(param.name) + convertFrom(param)

private fun convertFrom(param: TargetParameter): CodeBlock {
    val target = param.selectTarget(FIREBASE_SUPPORTED_TYPES)
    return param.convertTo(target)
}
