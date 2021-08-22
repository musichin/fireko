package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.FirekoAdapter
import de.musichin.fireko.annotations.NullValues
import javax.lang.model.type.DeclaredType

internal class TargetParameter(
    classElement: ClassElement,
    parameterSpec: ParameterSpec,
) {
    val name = parameterSpec.name

    val type = parameterSpec.type

    val annotations: List<AnnotationSpec> =
        parameterSpec.propertyAnnotations(classElement.typeSpec)

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    val propertyName: String = annotations.propertyName() ?: name

    val hasDefaultValue: Boolean = parameterSpec.defaultValue != null

    val usingAdapter = annotations
        .get(FirekoAdapter::class.asClassName())
        ?.value("using")
        ?.let { it as DeclaredType }
        ?.asElement()

    private val targetNullValues: NullValues? =
        classElement.element.getAnnotation(NullValues::class.java)

    private val propertyNullValues: NullValues? by lazy {
        val nullValues = annotations.get(NullValues::class.asClassName())
        if (nullValues == null) {
            null
        } else {
            val omit = nullValues.value("omit") as Boolean? ?: false
            val preset = nullValues.value("preset") as Boolean? ?: false
            val constructor = NullValues::class.constructors.first()
            constructor.call(omit, preset)
        }
    }

    val omitNullValue = type.isNullable &&
            (propertyNullValues ?: targetNullValues)?.omit == true

    val presetNullValue = hasDefaultValue &&
            (propertyNullValues ?: targetNullValues)?.preset == true

    val embedded: Boolean = hasAnnotation(Embedded::class.asClassName())

    val documentId: Boolean = hasAnnotation(FIREBASE_DOCUMENT_ID)

    val serverTimestamp: Boolean = hasAnnotation(FIREBASE_SERVER_TIMESTAMP)

    fun hasAnnotation(typeName: TypeName): Boolean = annotation(typeName) != null

    fun annotation(typeName: TypeName): AnnotationSpec? = annotations.get(typeName)

    companion object {
        private fun ParameterSpec.propertyAnnotations(typeSpec: TypeSpec): List<AnnotationSpec> {
            val fieldAnnotations = typeSpec.propertySpecs
                .filter { it.name == this.name }
                .flatMap { it.annotations }
            return annotations + fieldAnnotations
        }

        internal fun List<AnnotationSpec>.propertyName(): String? =
            annotationValue(FIREBASE_PROPERTY_NAME, "value")?.toString()

        internal fun List<AnnotationSpec>.annotationValue(
            annotation: TypeName,
            property: String
        ): Any? = get(annotation)?.value(property)

        @KotlinPoetMetadataPreview
        fun create(element: ClassElement, parameter: ParameterSpec): TargetParameter {
            return TargetParameter(element, parameter)
        }
    }
}
