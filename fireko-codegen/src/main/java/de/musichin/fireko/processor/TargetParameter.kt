package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.tag
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.NullValues
import javax.lang.model.element.AnnotationMirror

@KotlinPoetMetadataPreview
internal class TargetParameter(
    targetElement: TargetElement,
    parameterSpec: ParameterSpec,
) {
    val name = parameterSpec.name

    val type = parameterSpec.type

    val annotations: List<AnnotationSpec> = parameterSpec.propertyAnnotations(targetElement.typeSpec)

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    val propertyName: String = annotations.propertyName() ?: name

    val hasDefaultValue: Boolean = parameterSpec.defaultValue != null

    private val targetNullValues: NullValues? =
        targetElement.element.getAnnotation(NullValues::class.java)

    val omitNullValue = type.isNullable &&
            (annotations.nullValuesOmit() || targetNullValues?.omit == true)

    val presetNullValue = hasDefaultValue &&
            (annotations.nullValuesPreset() || targetNullValues?.preset == true)

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

        internal fun List<AnnotationSpec>.nullValuesOmit(): Boolean =
            annotationValue(NullValues::class.asClassName(), "omit") == true

        internal fun List<AnnotationSpec>.nullValuesPreset(): Boolean =
            annotationValue(NullValues::class.asClassName(), "preset") == true

        internal fun List<AnnotationSpec>.annotationValue(
            annotation: TypeName,
            property: String
        ): Any? = get(annotation)
            ?.tag<AnnotationMirror>()
            ?.elementValues
            ?.entries
            ?.singleOrNull { it.key.simpleName.contentEquals(property) }
            ?.value
            ?.value

        @KotlinPoetMetadataPreview
        fun create(element: TargetElement, parameter: ParameterSpec): TargetParameter {
            return TargetParameter(element, parameter)
        }
    }
}
