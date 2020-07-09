package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal fun generateFunReceiverMap(target: TargetClass) = FunSpec
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
                val propertySpec = PropertySpec.builder(param.name, param.type)
                    .initializer("%L%L", docIdParam.name, param.convertFrom(docIdParam.type))
                    .build()
                addCode("%L", propertySpec)
            }
        }

        localParams.forEach { parameter ->
            addCode("%L", generateLocalProperty(parameter))
        }

        val paramNames = params.map { it.name }.joinToString(", ") { "$it = $it" }

        addCode("return ${target.simpleName}($paramNames)")
    }
    .build()

private fun generateLocalProperty(param: TargetParameter): PropertySpec = PropertySpec
    .builder(param.name, param.type)
    .initializer(generateInitializer(param))
    .build()

private fun generateInitializer(param: TargetParameter): CodeBlock {
    val name = param.propertyName
    val type = param.type

    if (param.documentId) {
        return CodeBlock.of("TODO()")
    }

    if (param.embedded) {
        return CodeBlock.of(
            "this%L",
            param.convertFrom(MAP.parameterizedBy(STRING, ANY.copy(nullable = true)))
        )
    }

    val supportedSources = FIREBASE_SUPPORTED_TYPES intersect param.supportedSources
    if (supportedSources.isNotEmpty()) {
        val source = param.selectSource(supportedSources)
        return initializerByType(source, param)
    }

    return getBaseInitializer(name, type)
}

private fun initializerByType(type: TypeName, param: TargetParameter): CodeBlock {
    val source = type.copy(nullable = param.type.isNullable)
    return getBaseInitializer(param.propertyName, source) + param.convertFrom(source)
}

private fun getBaseInitializer(name: String, type: TypeName): CodeBlock =
    when (type.copy(nullable = false)) {
        BOOLEAN,
        STRING,
        LONG,
        DOUBLE,
        FIREBASE_TIMESTAMP,
        FIREBASE_BLOB,
        FIREBASE_GEO_POINT,
        FIREBASE_DOCUMENT_REFERENCE,
        MAP,
        MAP.parameterizedBy(STRING, ANY),
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
        MAP.parameterizedBy(ANY, ANY),
        MAP.parameterizedBy(ANY, ANY.copy(nullable = true)),
        LIST,
        LIST.parameterizedBy(ANY) -> CodeBlock.of("(get(%S) as %T)", name, type)
        else -> throw IllegalArgumentException("Unsupported type $type")
    }