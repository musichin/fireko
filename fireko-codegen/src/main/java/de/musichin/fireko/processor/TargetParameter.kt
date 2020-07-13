package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import de.musichin.fireko.annotations.Fireko
import javax.lang.model.element.AnnotationMirror

internal sealed class TargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) {
    val name = parameterSpec.name

    val type = parameterSpec.type

    val annotations: List<AnnotationSpec> = parameterSpec.propertyAnnotations(targetTypeSpec)

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    val propertyName: String = annotations.propertyName() ?: name

    val embedded: Boolean = hasAnnotation(EMBEDDED)

    val documentId: Boolean = hasAnnotation(FIREBASE_DOCUMENT_ID)

    val serverTimestamp: Boolean = hasAnnotation(FIREBASE_SERVER_TIMESTAMP)

    fun hasAnnotation(typeName: TypeName): Boolean = annotation(typeName) != null

    fun annotation(typeName: TypeName): AnnotationSpec? = annotations.get(typeName)

    open fun convertFrom(source: TypeName): CodeBlock {
        if (isIdentityType(source)) {
            throw IllegalArgumentException("Source $source is unsupported.")
        }

        return CodeBlock.of("")
    }

    open fun convertTo(target: TypeName): CodeBlock {
        if (isIdentityType(target)) {
            throw IllegalArgumentException("Target $target is unsupported.")
        }

        return CodeBlock.of("")
    }

    private fun isIdentityType(typeName: TypeName) =
        typeName.copy(nullable = false) != type.copy(nullable = false)

    open fun selectSource(sources: Collection<TypeName>): TypeName = select(sources, type)

    open fun selectTarget(targets: Collection<TypeName>): TypeName = select(type, targets)

    companion object {
        private fun ParameterSpec.propertyAnnotations(typeSpec: TypeSpec): List<AnnotationSpec> {
            val fieldAnnotations = typeSpec.propertySpecs
                .filter { it.name == this.name }
                .flatMap { it.annotations }
            return annotations + fieldAnnotations
        }

        internal fun List<AnnotationSpec>.propertyName(): String? = get(FIREBASE_PROPERTY_NAME)
            ?.tag<AnnotationMirror>()
            ?.elementValues
            ?.entries
            ?.single { it.key.simpleName.contentEquals("value") }
            ?.value
            ?.value
            ?.toString()

        @KotlinPoetMetadataPreview
        fun create(
            context: Context,
            element: TargetElement,
            parameter: ParameterSpec
        ): TargetParameter {
            val paramClassName = (parameter.type as? ClassName)
            val paramTargetClass = paramClassName?.let { context.targetClass(paramClassName) }
            val paramTargetElement = paramClassName?.let { context.targetElement(paramClassName) }
            val targetTypeSpec = element.typeSpec

            if (paramTargetClass?.typeSpec?.isEnum == true) {
                return EnumTargetParameter(targetTypeSpec, parameter, paramTargetClass.typeSpec)
            }

            if (paramTargetElement?.element?.getAnnotation(Fireko::class.java) != null) {
                return TargetClassTargetParameter(context, element, parameter)
            }

            return when (parameter.type.copy(nullable = false)) {
                STRING, CHAR_SEQUENCE,
                NUMBER, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR,
                TIME_INSTANT, BP_INSTANT, UTIL_DATE,
                FIREBASE_TIMESTAMP, FIREBASE_BLOB, FIREBASE_DOCUMENT_REFERENCE, FIREBASE_GEO_POINT ->
                    StaticTargetParameter(targetTypeSpec, parameter)
                else -> throw IllegalArgumentException("Unsupported type $targetTypeSpec")
            }
        }
    }
}

private class StaticTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override fun selectSource(sources: Collection<TypeName>): TypeName = select(sources, type)
    override fun selectTarget(targets: Collection<TypeName>): TypeName = select(type, targets)

    override fun convertFrom(source: TypeName): CodeBlock {
        return CodeBlock.builder().convert(source, type).build()
    }

    override fun convertTo(target: TypeName): CodeBlock {
        return CodeBlock.builder().convert(type, target).build()
    }
}

@KotlinPoetMetadataPreview
internal class TargetClassTargetParameter(
    private val context: Context,
    private val element: TargetElement,
    val parameterSpec: ParameterSpec
) : TargetParameter(element.typeSpec, parameterSpec) {

    private val supportedTypes = listOf(
        FIREBASE_DOCUMENT_SNAPSHOT,
        MAP.parameterizedBy(STRING, ANY),
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
        MAP.parameterizedBy(ANY, ANY),
        MAP.parameterizedBy(ANY, ANY.copy(nullable = true))
    )

    override fun selectSource(sources: Collection<TypeName>): TypeName {
        return sources.find { it.notNullable in supportedTypes } ?: super.selectSource(sources)
    }

    override fun selectTarget(targets: Collection<TypeName>): TypeName {
        return targets.find { it.notNullable in supportedTypes } ?: super.selectTarget(targets)
    }

    override fun convertFrom(source: TypeName): CodeBlock = when (source.notNullable) {
        FIREBASE_DOCUMENT_SNAPSHOT ->
            CodeBlock.Builder().add(invokeToType(type as ClassName)).build()
        MAP.parameterizedBy(STRING, ANY),
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
        MAP.parameterizedBy(ANY, ANY),
        MAP.parameterizedBy(ANY, ANY.copy(nullable = true)),
        MAP -> {
            // do I need id?
            val paramTargetClass = context.targetClass(parameterSpec.type as ClassName)
            val paramNames = if (paramTargetClass?.needsDocumentId == true) {
                val paramName = context.targetClass(element)?.documentIdParamSpec?.name
                listOfNotNull(paramName)
            } else {
                emptyList()
            }
            CodeBlock.Builder()
                .convert(source, source.copy(nullable = type.isNullable))
                .add(invokeToType(type as ClassName, *paramNames.toTypedArray()))
                .build()
        }
        else -> super.convertFrom(source)
    }

    override fun convertTo(target: TypeName): CodeBlock = when (target.notNullable) {
        MAP.parameterizedBy(STRING, ANY),
        MAP.parameterizedBy(STRING, ANY.copy(nullable = true)),
        MAP.parameterizedBy(ANY, ANY),
        MAP.parameterizedBy(ANY, ANY.copy(nullable = true)),
        MAP -> {
            CodeBlock.Builder()
                .call(type.isNullable)
                .add("toMap()")
                .build()
        }
        else -> super.convertTo(target)
    }
}

internal class EnumTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec,
    typeSpec: TypeSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {

    override fun selectSource(sources: Collection<TypeName>): TypeName {
        return sources.find { it.notNullable == STRING } ?: super.selectSource(sources)
    }

    override fun selectTarget(targets: Collection<TypeName>): TypeName {
        return targets.find { it.notNullable == STRING } ?: super.selectTarget(targets)
    }

    private val propertyNames: Map<String, String> = typeSpec.enumConstants
        .mapNotNull { (name, spec) -> spec.annotationSpecs.propertyName()?.let { name to it } }
        .toMap()

    override fun convertFrom(source: TypeName): CodeBlock {
        if (propertyNames.isEmpty()) {
            return useValueOf(source)
        }

        return CodeBlock.Builder()
            .convert(source, source.copy(nullable = type.isNullable))
            .call(type)
            .beginControlFlow("let")
            .beginControlFlow("when (it)")
            .apply {
                propertyNames.forEach { (name, propertyName) ->
                    addStatement("%S -> %T.%L", propertyName, type, name)
                }
            }
            .addStatement("else -> %T.valueOf(it)", type.copy(nullable = false))
            .endControlFlow()
            .endControlFlow()
            .build()
    }

    private fun useValueOf(source: TypeName): CodeBlock {
        return CodeBlock.builder()
            .convert(source, source.copy(nullable = type.isNullable))
            .call(type)
            .beginControlFlow("let")
            .addStatement(
                "%T.valueOf(it)",
                type.copy(nullable = false)
            )
            .endControlFlow()
            .build()
    }

    override fun convertTo(target: TypeName): CodeBlock {
        if (propertyNames.isEmpty()) {
            return useName()
        }

        return CodeBlock.Builder()
            .call(type)
            .beginControlFlow("let")
            .beginControlFlow("when (it)")
            .apply {
                propertyNames.forEach { (name, propertyName) ->
                    addStatement("%T.%L -> %S", type, name, propertyName)
                }
            }
            .addStatement("else -> it.name")
            .endControlFlow()
            .endControlFlow()
            .build()
    }

    private fun useName(): CodeBlock {
        return CodeBlock.builder()
            .call(type)
            .add("name")
            .build()
    }
}
