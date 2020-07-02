package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.util.Elements

internal sealed class TargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) {
    val name = parameterSpec.name

    val type = parameterSpec.type

    val annotations: List<AnnotationSpec> = parameterSpec.propertyAnnotations(targetTypeSpec)

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    val hasDefault: Boolean = parameterSpec.defaultValue != null

    val propertyName: String = annotations.propertyName() ?: name

    fun hasAnnotation(typeName: TypeName): Boolean = annotation(typeName) != null

    fun annotation(typeName: TypeName): AnnotationSpec? = annotations.get(typeName)

    abstract fun convert(source: TypeName): CodeBlock

    abstract val supportedSources: List<TypeName>

    fun isSourceSupported(source: TypeName): Boolean =
        supportedSources.contains(source.copy(nullable = false))

    open fun selectSource(sources: Collection<TypeName>): TypeName {
        if (sources.contains(type.copy(nullable = false))) {
            return type.copy(nullable = false)
        }

        return sources.first()
    }

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
            typeSpec: TypeSpec,
            parameter: ParameterSpec,
            elements: Elements,
            classInspector: ClassInspector
        ): TargetParameter {
            val paramClassName = (parameter.type as? ClassName)
            val paramTypeSpec = paramClassName
                ?.canonicalName
                ?.let(elements::getTypeElement)
                ?.getAnnotation(Metadata::class.java)
                ?.toImmutableKmClass()
                ?.toTypeSpec(classInspector, paramClassName)

            if (paramTypeSpec?.isEnum == true) {
                return EnumTargetParameter(typeSpec, parameter, paramTypeSpec)
            }

            return when (parameter.type.copy(nullable = false)) {
                STRING, CHAR_SEQUENCE ->
                    StringTargetParameter(typeSpec, parameter)
                NUMBER, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR ->
                    NumberTargetParameter(typeSpec, parameter)
                TIME_INSTANT, BP_INSTANT ->
                    InstantTargetParameter(typeSpec, parameter)
                UTIL_DATE ->
                    DateTargetParameter(typeSpec, parameter)
                FIREBASE_TIMESTAMP, FIREBASE_BLOB, FIREBASE_DOCUMENT_REFERENCE, FIREBASE_GEO_POINT ->
                    IdentityTargetParameter(typeSpec, parameter)
                else -> ClassTargetParameter(typeSpec, parameter, paramTypeSpec)
            }
        }
    }
}

private class StringTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> = listOf(STRING, CHAR_SEQUENCE)

    override fun convert(source: TypeName): CodeBlock = when (source.copy(nullable = false)) {
        STRING -> CodeBlock.of("")
        else -> throw IllegalArgumentException("Cannot convert $source to String")
    }
}

private class NumberTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> =
        listOf(BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, NUMBER)

    override fun selectSource(sources: Collection<TypeName>): TypeName {
        val type = type.copy(nullable = false)

        if (sources.contains(type)) {
            return type
        }

        return when {
            sources.contains(DOUBLE) && type == NUMBER -> DOUBLE
            sources.contains(DOUBLE) && isFloating(type) -> DOUBLE
            sources.contains(FLOAT) && isFloating(type) -> FLOAT
            sources.contains(LONG) && !isFloating(type) -> LONG
            sources.contains(INT) && !isFloating(type) -> INT
            sources.contains(SHORT) && !isFloating(type) -> SHORT
            sources.contains(BYTE) && !isFloating(type) -> BYTE
            sources.contains(CHAR) && !isFloating(type) -> CHAR
            else -> sources.first()
        }
    }

    override fun convert(source: TypeName): CodeBlock {
        if (source == type) {
            return CodeBlock.of("")
        }

        if (isNumber(source) && type.copy(nullable = false) == NUMBER) {
            return CodeBlock.of("")
        }

        if (isNumber(source)) {
            return invokeToType(type as ClassName)
        }

        if (source.copy(nullable = false) == STRING) {
            return invokeToType(type as ClassName)
        }

        // TODO Rational
        // TODO BigDecimal

        throw IllegalArgumentException("Cannot convert $source to $type")
    }

    private fun isNumber(typeName: TypeName): Boolean = when (typeName.copy(nullable = false)) {
        BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR -> true
        else -> false
    }

    private fun isFloating(typeName: TypeName): Boolean = when (typeName.copy(nullable = false)) {
        FLOAT, DOUBLE -> true
        else -> false
    }
}

private class InstantTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> =
        listOf(FIREBASE_TIMESTAMP, BP_INSTANT, TIME_INSTANT, UTIL_DATE, LONG)

    override fun convert(source: TypeName): CodeBlock = when (source.copy(nullable = false)) {
        BP_INSTANT, TIME_INSTANT ->
            CodeBlock.of("")
        FIREBASE_TIMESTAMP ->
            CodeBlock.Builder().call(source)
                .beginControlFlow("let")
                .addStatement(
                    "%T.ofEpochSecond(it.seconds, it.nanoseconds.toLong())",
                    type.copy(nullable = false)
                )
                .endControlFlow()
                .build()
        UTIL_DATE ->
            CodeBlock.Builder().call(source)
                .beginControlFlow("let")
                .addStatement(
                    "%T.ofEpochMilli(it.time)",
                    type.copy(nullable = false)
                )
                .endControlFlow()
                .build()
        LONG -> CodeBlock.Builder().call(source)
            .beginControlFlow("let")
            .addStatement(
                "%T.ofEpochMilli(it)",
                type.copy(nullable = false)
            )
            .endControlFlow()
            .build()
        else -> throw IllegalArgumentException("Cannot convert $source to $type")
    }
}

private class DateTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> = listOf(FIREBASE_TIMESTAMP, UTIL_DATE, LONG)

    override fun convert(source: TypeName): CodeBlock = when (source.copy(nullable = false)) {
        UTIL_DATE ->
            CodeBlock.of("")
        FIREBASE_TIMESTAMP ->
            invokeToType(UTIL_DATE)
        LONG ->
            CodeBlock.Builder().call(source)
                .beginControlFlow("let")
                .addStatement("%T(it)", UTIL_DATE)
                .endControlFlow()
                .build()
        else -> throw IllegalArgumentException("Cannot convert $source to $type")
    }
}

private class IdentityTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> = listOf(type.copy(nullable = false))

    override fun convert(source: TypeName): CodeBlock {
        if (source.copy(nullable = false) == type.copy(nullable = false)) {
            return CodeBlock.of("")
        }
        throw IllegalArgumentException("Cannot convert $source to $type")
    }
}

internal class ClassTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec,
    typeSpec: TypeSpec?
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> = listOf(FIREBASE_DOCUMENT_SNAPSHOT, MAP)

    override fun convert(source: TypeName): CodeBlock {
        return CodeBlock.Builder().add(invokeToType(type as ClassName)).build()
    }
}

internal class EnumTargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec,
    typeSpec: TypeSpec
) : TargetParameter(targetTypeSpec, parameterSpec) {
    override val supportedSources: List<TypeName> = listOf(STRING)

    private val propertyNames: Map<String, String> = typeSpec.enumConstants
        .mapNotNull { (name, spec) -> spec.annotationSpecs.propertyName()?.let { name to it } }
        .toMap()

    override fun convert(source: TypeName): CodeBlock {
        if (propertyNames.isEmpty()) {
            return useValueOf()
        }

        return CodeBlock.Builder()
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

    private fun useValueOf(): CodeBlock {
        return CodeBlock.builder()
            .call(type)
            .beginControlFlow("let")
            .addStatement(
                "%T.valueOf(it)",
                type.copy(nullable = false)
            )
            .endControlFlow()
            .build()
    }
}
