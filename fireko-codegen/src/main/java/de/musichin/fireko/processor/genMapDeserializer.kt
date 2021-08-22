package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
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

        addCode(genTargetClassInitializer(target))
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
private fun CodeBlock.Builder.assertNotNull(param: TargetParameter) = apply {
    if (!param.type.isNullable && !param.hasDefaultValue) {
        add("?: throw NullPointerException(%S)", "Property ${param.propertyName} is absent or null.")
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

    val adapter = param.usingAdapter?.let { context.adapterElement(it) }
        ?: context.getAnnotatedAdapter(type)
    if (adapter != null) {
        return if (adapter.readFunSpec != null) {
            val sourceType = adapter.readFunSpec.parameters.first().type
            CodeBlock.builder()
                .add(getBaseInitializer(context, name, sourceType.asNullable()))
                .deserialize(context, sourceType.asNullable(), true)
                .add("?.let(%L::%L)", adapter.className, adapter.readFunSpec.name)
                .assertNotNull(param)
                .build()
        } else {
            CodeBlock.of("Adapter should contain read method.")
        }
    }

    return CodeBlock.builder()
        .add(getBaseInitializer(context, name, type.asNullable()))
        .deserialize(context, type.asNullable(), true)
        .assertNotNull(param)
        .build()
}

@KotlinPoetMetadataPreview
private fun getBaseInitializer(context: Context, name: String, type: TypeName): CodeBlock {
    val source = ValueType.typeOf(context, type)

    if (source.isOneOf(ANY.asNullable())) {
        return CodeBlock.of("get(%S)", name)
    }

    return CodeBlock.of("(get(%S) as %T)", name, source.copy(nullable = type.isNullable))
}

@KotlinPoetMetadataPreview
private val TargetParameter.propertyType: TypeName
    get() = type.copy(nullable = type.isNullable || hasDefaultValue)

@KotlinPoetMetadataPreview
private val TargetClass.resultName: String
    get() = findFreeParamName("result")
