package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
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

        internal fun List<AnnotationSpec>.propertyName(): String? = get(FIREBASE_PROPERTY_NAME)
            ?.tag<AnnotationMirror>()
            ?.elementValues
            ?.entries
            ?.single { it.key.simpleName.contentEquals("value") }
            ?.value
            ?.value
            ?.toString()

        @KotlinPoetMetadataPreview
        fun create(element: TargetElement, parameter: ParameterSpec): TargetParameter {
            val targetTypeSpec = element.typeSpec

            return TargetParameter(targetTypeSpec, parameter)
        }
    }
}
