package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.tag
import javax.lang.model.element.AnnotationMirror

@KotlinPoetMetadataPreview
internal class TargetParameter(
    targetTypeSpec: TypeSpec,
    parameterSpec: ParameterSpec
) {
    val name = parameterSpec.name

    val type = parameterSpec.type

    val annotations: List<AnnotationSpec> = parameterSpec.propertyAnnotations(targetTypeSpec)

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    val propertyName: String = annotations.propertyName() ?: name

    val hasDefaultValue: Boolean = parameterSpec.defaultValue != null

    val omitNullValue = annotations.annotationValue(NULL_VALUE, "omit") == true
            && type.isNullable

    val presetNullValue = annotations.annotationValue(NULL_VALUE, "preset") == true
            && hasDefaultValue

    val embedded: Boolean = hasAnnotation(EMBEDDED)

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
        ): Any? = get(annotation)
            ?.tag<AnnotationMirror>()
            ?.elementValues
            ?.entries
            ?.singleOrNull { it.key.simpleName.contentEquals(property) }
            ?.value
            ?.value

        @KotlinPoetMetadataPreview
        fun create(element: TargetElement, parameter: ParameterSpec): TargetParameter {
            val targetTypeSpec = element.typeSpec

            return TargetParameter(targetTypeSpec, parameter)
        }
    }
}
