package de.musichin.fireko.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.tag
import javax.lang.model.element.AnnotationMirror


internal fun propertyName(target: TargetType, property: ParameterSpec): String = property
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

internal fun ParameterSpec.propertyAnnotations(typeSpec: TypeSpec): List<AnnotationSpec> {
    val fieldAnnotations = typeSpec.propertySpecs
        .filter { it.name == this.name }
        .flatMap { it.annotations }
    return annotations + fieldAnnotations
}