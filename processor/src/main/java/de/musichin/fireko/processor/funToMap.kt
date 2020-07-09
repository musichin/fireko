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
//    .beginControlFlow("return HashMap<%T, %T>(%L).apply", STRING, ANY, target.params.size) // TODO take care of embedded
    .beginControlFlow("return mutableMapOf<%T, %T>().apply", STRING, ANY.copy(nullable = true))
    .apply {
        val params = target.params
        val documentedIdParams = target.documentIdParams
        val propertyParams = (params - documentedIdParams)

        propertyParams.forEach { param ->
            addCode(putter(param))
        }
    }
    .endControlFlow()
    .build()

@KotlinPoetMetadataPreview
private fun putter(param: TargetParameter): CodeBlock {
    return CodeBlock.builder()
        .add("%L", getter(param))
        .call(param.type)
        .beginControlFlow("let")
        .apply {
            if (param.embedded) {
                addStatement("putAll(it)")
            } else {
                addStatement("put(%S, it)", param.propertyName)
            }
        }
        .endControlFlow()
        .build()
}

@KotlinPoetMetadataPreview
private fun getter(param: TargetParameter): CodeBlock {
    return CodeBlock.of(param.name) + convertFrom(param)
}

private fun convertFrom(param: TargetParameter): CodeBlock {
    val supportedTargets = FIREBASE_SUPPORTED_TYPES intersect param.supportedTargets
    if (supportedTargets.isNotEmpty()) {
        val target = param.selectTarget(supportedTargets)
        return param.convertTo(target)
    }

    throw IllegalArgumentException("Could not generate converter for ${param.name}")
}
