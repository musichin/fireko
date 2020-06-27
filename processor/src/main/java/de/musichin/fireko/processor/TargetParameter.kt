package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.AnnotationMirror

data class TargetParameter(
    val targetClass: TargetClass,
    val parameterSpec: ParameterSpec
) {
    val name = parameterSpec.name

    val propertyName = propertyName(targetClass, parameterSpec)

    val annotations: List<AnnotationSpec> = parameterSpec.propertyAnnotations(targetClass.typeSpec)

    val type = parameterSpec.type

    fun hasAnnotation(typeName: TypeName): Boolean = annotations.any { it.typeName == typeName }

    val exclude: Boolean = hasAnnotation(FIREBASE_EXCLUDE)

    val include = !exclude

    companion object {
        private fun propertyName(target: TargetClass, property: ParameterSpec): String = property
            .propertyAnnotations(target.typeSpec)
            .find { it.typeName == FIREBASE_PROPERTY_NAME }
            ?.tag<AnnotationMirror>()
            ?.elementValues
            ?.entries
            ?.single { it.key.simpleName.contentEquals("value") }
            ?.value
            ?.value
            ?.toString()
            ?: property.name

        private fun ParameterSpec.propertyAnnotations(typeSpec: TypeSpec): List<AnnotationSpec> {
            val fieldAnnotations = typeSpec.propertySpecs
                .filter { it.name == this.name }
                .flatMap { it.annotations }
            return annotations + fieldAnnotations
        }
    }
}
