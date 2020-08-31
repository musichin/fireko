package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun genFunObjectToMap(context: Context, target: TargetClass) = FunSpec
    .builder("toMap")
    .addKdoc("Converts %T to %T.", target.type, MAP.parameterizedBy(STRING, ANY))
    .receiver(target.type)
    .returns(MAP.parameterizedBy(STRING, ANY.asNullable()))
    .body(context, target)
    .build()

@KotlinPoetMetadataPreview
private fun FunSpec.Builder.body(context: Context, target: TargetClass) = apply {
    val params = target.params
    val documentedIdParams = target.documentIdParams
    val propertyParams = (params - documentedIdParams)

    if (propertyParams.isEmpty()) {
        addCode("return emptyMap()")
    } else {
        beginControlFlow("return mutableMapOf<%T, %T>().apply", STRING, ANY.asNullable())
        putParams(context, propertyParams)
        endControlFlow()
    }
}

@KotlinPoetMetadataPreview
private fun FunSpec.Builder.putParams(context: Context, params: List<TargetParameter>) = apply {
    params.forEach { param ->
        addCode(putter(context, param))
    }
}

@KotlinPoetMetadataPreview
private fun putter(context: Context, param: TargetParameter): CodeBlock =
    if (param.embedded) {
        if (param.type.isNullable) {
            CodeBlock.of("putAll(%L.orEmpty())\n", serialize(context, param))
        } else {
            CodeBlock.of("putAll(%L)\n", serialize(context, param))
        }
    } else if (!param.omitNullValue || !param.type.isNullable || param.serverTimestamp) {
        CodeBlock.of("put(%S, %L)\n", param.propertyName, serialize(context, param))
    } else {
        CodeBlock.builder()
            .beginControlFlow("if (%L != null)", param.name)
            .add("put(%S, %L)\n", param.propertyName, serialize(context, param, false))
            .endControlFlow()
            .build()
    }

@KotlinPoetMetadataPreview
private fun serialize(
    context: Context,
    param: TargetParameter,
    nullable: Boolean = param.type.isNullable
): CodeBlock {
    return CodeBlock.builder()
        .add("%L", param.name)
        .serialize(context, param.type.nullable(nullable))
        .apply {
            if (param.serverTimestamp && param.type.isNullable) {
                add("?:")
                serverTimestamp()
            }
        }
        .build()
}

private fun CodeBlock.Builder.serverTimestamp() = apply {
    add("%T.serverTimestamp()", FIREBASE_FIELD_VALUE)
}
