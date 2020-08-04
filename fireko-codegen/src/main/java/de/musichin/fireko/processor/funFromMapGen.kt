package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun genFunMapToObject(context: Context, target: TargetClass) = FunSpec
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

        addCode(CodeBlock.builder().genReturns(target).build())
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
private fun CodeBlock.Builder.genReturns(
    target: TargetClass,
    index: Int = 0,
    excludedParams: List<TargetParameter> = emptyList()
): CodeBlock.Builder {
    val paramsWithDefault = target.params.filter(TargetParameter::hasDefaultValue)
    val param = paramsWithDefault.getOrNull(index)

    if (param == null) {
        val includedParams = paramsWithDefault - excludedParams
        includedParams.forEach { includedParam ->
            addStatement("requireNotNull(%L)", includedParam.name)
        }
        genReturn(target.params - excludedParams, target)
        return this
    }

    beginControlFlow("if (%L != null || contains(%S))", param.name, param.propertyName)
    genReturns(target, index + 1, excludedParams)
    nextControlFlow("else")
    genReturns(target, index + 1, excludedParams + param)
    endControlFlow()
    return this
}

@KotlinPoetMetadataPreview
private fun CodeBlock.Builder.genReturn(params: List<TargetParameter>, target: TargetClass) {
    val paramNames = params.map(TargetParameter::name).joinToString(", ") { name ->
        "$name = $name"
    }

    addStatement("return %T($paramNames)", target.type.notNullable())
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
            .add("this%L", invokeToType(type.notNullable(), *paramNames.toTypedArray()))
            .build()
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, param.propertyType))
        .deserialize(context, param.propertyType, param.propertyType.isNullable)
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
