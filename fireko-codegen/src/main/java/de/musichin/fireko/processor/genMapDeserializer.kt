package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun genMapDeserializer(context: Context, target: TargetClass) = FunSpec
    .builder(toType(target.type))
    .addKdoc(
        "Converts %T obtained from %T from to %T.",
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
        FIREBASE_DOCUMENT_SNAPSHOT,
        target.type
    )
    .receiver(MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
    .returns(target.type)
    .apply {
        val params = target.params
        val documentIdParams = target.documentIdParams
        val localParams = params - documentIdParams

        val docIdParam = target.documentIdParamSpec

        if (docIdParam != null) {
            addParameter(docIdParam)

            val coveredIdParam = documentIdParams.find {
                it.name == docIdParam.name && it.type.isAssignable(it.type)
            }

            val remainingParams = documentIdParams - listOfNotNull(coveredIdParam)

            remainingParams.forEach { param ->
                val initializer = CodeBlock.builder().convert(docIdParam.type, param.type).build()
                val propertySpec = PropertySpec.builder(param.name, param.type)
                    .initializer("%L%L", docIdParam.name, initializer)
                    .build()
                addCode("%L", propertySpec)
            }
        }

        localParams.forEach { parameter ->
            addCode("%L", generateLocalProperty(context, target, parameter))
        }

        when {
            params.none(TargetParameter::hasDefaultValue) ->
                addCode(CodeBlock.builder().genSimpleInitializer(target).build())
            target.typeSpec.isData ->
                addCode(CodeBlock.builder().getDataInitializer(target).build())
            else ->
                addCode(CodeBlock.builder().getClassInitializer(target).build())
        }
    }
    .build()

@KotlinPoetMetadataPreview
private fun generateLocalProperty(
    context: Context,
    target: TargetClass,
    param: TargetParameter
): PropertySpec = PropertySpec
    .builder(param.name, param.propertyType)
    .initializer(generateInitializer(context, target, param))
    .build()

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
private fun CodeBlock.Builder.assertNullability(param: TargetParameter, force: Boolean = false) = apply {
    if (!param.type.isNullable && !param.hasDefaultValue || force) {
        add(" ?: throw NullPointerException(%S)", "Property ${param.propertyName} is absent or null.")
    }
}

@KotlinPoetMetadataPreview
private fun generateInitializer(
    context: Context,
    target: TargetClass,
    param: TargetParameter
): CodeBlock {
    val type = param.type
    val name = param.propertyName

    if (param.documentId) {
        throw IllegalArgumentException("${param.name} is not allowed to be DocumentId")
    }

    if (param.embedded) {
        type as ClassName
        // id needed?
        val paramTargetClass = context.targetClass(type)
        val paramNames = if (paramTargetClass?.needsDocumentId == true) {
            val paramName = target.documentIdParamSpec?.name
            listOfNotNull(paramName)
        } else {
            emptyList()
        }
        return CodeBlock.builder()
            .add("this%L", invokeToType(type.asNotNullable(), *paramNames.toTypedArray()))
            .build()
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, param.type.asNullable()))
        .deserialize(context, param.type.asNullable(), true)
        .assertNullability(param)
        .build()
}

@KotlinPoetMetadataPreview
private fun getBaseInitializer(context: Context, name: String, type: TypeName): CodeBlock {
    val source = ValueType.typeOf(context, type)

    return CodeBlock.of("(get(%S) as %T)", name, source.copy(nullable = type.isNullable))
}

@KotlinPoetMetadataPreview
private val TargetParameter.propertyType: TypeName
    get() = type.copy(nullable = type.isNullable || hasDefaultValue)

@KotlinPoetMetadataPreview
private val TargetClass.resultName: String
    get() = findFreeParamName("result")
