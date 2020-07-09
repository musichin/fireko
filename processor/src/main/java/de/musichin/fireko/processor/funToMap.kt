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
    if (param.embedded) {
        return CodeBlock.of("putAll(%L)\n", getter(param))
    }

    return CodeBlock.of("put(%S, %L)\n", param.propertyName, getter(param))
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

    // FIXME
    return CodeBlock.of(".let{TODO()}")
}