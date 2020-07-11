package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.Element
import javax.lang.model.util.Elements

@KotlinPoetMetadataPreview
internal class Context(
    annotatedElement: Iterable<Element>,
    private val elements: Elements,
    private val classInspector: ClassInspector
) {
    val targetElements: List<TargetElement> = create(annotatedElement, classInspector)

    private val otherTargetElements = mutableMapOf<ClassName, TargetElement?>()
    private val targetClasses = mutableMapOf<TargetElement, TargetClass?>()

    fun targetElement(className: ClassName): TargetElement? =
        targetElements.find { it.className == className } ?: getOrCreateTargetElement(className)

    fun targetClass(typeName: ClassName): TargetClass? {
        val targetElement = targetElement(typeName) ?: return null
        return targetClasses.getOrPut(targetElement) {
            TargetClass.create(this, targetElement)
        }
    }

    fun targetClass(element: TargetElement): TargetClass? {
        return targetClass(element.className)
    }

    fun typeSpec(className: ClassName): TypeSpec? {
        return targetElement(className)?.typeSpec
    }

    private fun getOrCreateTargetElement(className: ClassName): TargetElement? {
        return otherTargetElements.getOrPut(className) {
            val element = className.canonicalName.let(elements::getTypeElement)
            val meta = element?.getAnnotation(Metadata::class.java)
            val typeSpec = meta?.toImmutableKmClass()?.toTypeSpec(classInspector, className)
            typeSpec?.let { TargetElement(element, className, it) }
        }
    }

    companion object {
        @KotlinPoetMetadataPreview
        fun create(elements: Iterable<Element>, classInspector: ClassInspector) =
            elements.map { element ->
                val meta = element.getAnnotation(Metadata::class.java)
                val kmClass = meta.toImmutableKmClass()
                val className = ClassInspectorUtil.createClassName(kmClass.name)
                val typeSpec = kmClass.toTypeSpec(classInspector)
                TargetElement(element, className, typeSpec)
            }
    }
}