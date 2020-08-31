package de.musichin.fireko.processor

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun CodeBlock.Builder.genTargetClassInitializer(target: TargetClass) = apply {
    val params = target.params
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
internal fun genTargetClassInitializer(target: TargetClass) =
    CodeBlock.builder().genTargetClassInitializer(target).build()

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
    val th = CodeBlock.builder().assertNotNull(param).build()

    if (param.presetNullValue) {
        addStatement("%L = %L ?: %L.%L,", name, name, target.resultName, name)
    } else {
        addStatement(
            "%L = if (%L != null || contains(%S))\n⇥%L %L⇤\nelse\n⇥%L.%L,⇤",
            name, name, param.propertyName, name, th, target.resultName, name
        )
    }
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
private fun CodeBlock.Builder.assertNotNull(param: TargetParameter) = apply {
    if (!param.type.isNullable && !param.presetNullValue) {
        add(" ?: throw NullPointerException(%S)", "Property ${param.propertyName} is null.")
    }
}

@KotlinPoetMetadataPreview
private val TargetParameter.propertyType: TypeName
    get() = type.copy(nullable = type.isNullable || hasDefaultValue)

@KotlinPoetMetadataPreview
private val TargetClass.resultName: String
    get() = findFreeParamName("result")
