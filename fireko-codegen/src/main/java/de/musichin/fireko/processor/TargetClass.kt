package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
internal class TargetClass private constructor(
    private val context: Context,
    val typeSpec: TypeSpec,
    val type: ClassName,
    val allParams: List<TargetParameter>
) {
    val canonicalName: String get() = type.canonicalName
    val simpleName: String get() = type.simpleName
    val packageName: String get() = type.packageName

    val params = allParams.filter { it.include }

    val excludeParams = allParams.filter { it.exclude }

    val documentIdParams = params.filter { it.documentId }

    private val allDocumentIds: List<TargetParameter> by lazy {
        params.flatMap { param ->
            when {
                param.documentId -> listOf(param)
                param is TargetClassTargetParameter -> {
                    context.targetClass(param.parameterSpec.type as ClassName)!!.allDocumentIds
                }
                else -> emptyList()
            }
        }
    }

    private val preferredDocumentId = allDocumentIds.find {
        it.type.copy(nullable = false) == STRING
    }

    val needsDocumentId: Boolean = allDocumentIds.isNotEmpty()

    val documentIdNullable = allDocumentIds.all { it.type.isNullable }

    val documentIdParamSpec: ParameterSpec? by lazy {
        if (!needsDocumentId) {
            null
        } else {
            val type = STRING.copy(nullable = documentIdNullable)
            val name = preferredDocumentId?.name ?: "documentId"

            if (allParams.any { it.name == name && !it.type.isAssignable(type) }) {
                throw IllegalArgumentException("Could not find a name for document id")
            }

            ParameterSpec.builder(name, type).build()
        }
    }

    companion object {
        @KotlinPoetMetadataPreview
        fun create(context: Context, element: TargetElement): TargetClass {
            val constructor = element.typeSpec.primaryConstructor
                ?: throw IllegalArgumentException("${element.className} has no default constructor")

            val params = constructor.parameters.map { parameter ->
                TargetParameter.create(context, element, parameter)
            }

            return TargetClass(context, element.typeSpec, element.className, params)
        }
    }
}
