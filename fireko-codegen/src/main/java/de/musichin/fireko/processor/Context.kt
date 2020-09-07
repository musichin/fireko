package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import de.musichin.fireko.annotations.Fireko
import de.musichin.fireko.annotations.FirekoAdapter
import javax.lang.model.element.Element
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.Elements

@KotlinPoetMetadataPreview
internal class Context(
    annotatedElement: Iterable<Element>,
    adapters: Iterable<Element>,
    private val elements: Elements,
    private val classInspector: ClassInspector
) {
    val classElements: List<ClassElement> = create(annotatedElement, classInspector)
    val adapterElements = createAdapter(adapters, classInspector)

    private val otherTargetElements = mutableMapOf<TypeName, ClassElement?>()
    private val targetClasses = mutableMapOf<ClassElement, TargetClass?>()

    fun targetElement(className: TypeName): ClassElement? =
        classElements.find { it.className == className } ?: getOrCreateTargetElement(className)

    fun targetClass(typeName: TypeName): TargetClass? {
        val targetElement = targetElement(typeName) ?: return null
        return targetClasses.getOrPut(targetElement) {
            TargetClass.create(this, targetElement)
        }
    }

    fun adapterElement(type: TypeName): AdapterElement? =
        adapterElements.find { it.className == type }

    fun adapterElement(type: Element): AdapterElement? =
        adapterElements.find { it.element == type }

    fun isEnum(type: TypeName): Boolean {
        if (type !is ClassName) return false
        val targetClass = targetClass(type) ?: return false
        return targetClass.typeSpec.isEnum
    }

    fun isPojo(type: TypeName): Boolean {
        if (type !is ClassName) return false

        val targetElement = targetElement(type) ?: return false
        return targetElement.element.getAnnotation(Fireko::class.java) != null
    }

    fun hasAdapter(type: TypeName): Boolean {
        return getAnnotatedAdapter(type) != null
    }

    fun getAnnotatedAdapter(type: TypeName): AdapterElement? {
        if (type !is ClassName) return null
        val element = targetElement(type) ?: return null
        val annotation = element.typeSpec.annotationSpecs.get(FirekoAdapter::class.asClassName())
        val adapterClass = annotation?.value("using") as? DeclaredType ?: return null
        val adapterClassElement = adapterClass.asElement()
        return adapterElement(adapterClassElement)
    }

    fun targetClass(element: ClassElement): TargetClass? {
        return targetClass(element.className)
    }

    fun typeSpec(className: ClassName): TypeSpec? {
        return targetElement(className)?.typeSpec
    }

    private fun getOrCreateTargetElement(typeName: TypeName): ClassElement? {
        return otherTargetElements.getOrPut(typeName) {
            val className = typeName as? ClassName
            val element = className?.canonicalName.let(elements::getTypeElement)
            val meta = element?.getAnnotation(Metadata::class.java)
            if (className != null && element != null && meta != null) {
                val typeSpec = meta.toImmutableKmClass().toTypeSpec(classInspector, className)
                ClassElement(element, className, typeSpec)
            } else {
                null
            }
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
                ClassElement(element, className, typeSpec)
            }

        @KotlinPoetMetadataPreview
        fun createAdapter(elements: Iterable<Element>, classInspector: ClassInspector) =
            elements.map { element ->
                val meta = element.getAnnotation(Metadata::class.java)
                val kmClass = meta.toImmutableKmClass()
                val className = ClassInspectorUtil.createClassName(kmClass.name)
                val typeSpec = kmClass.toTypeSpec(classInspector)

                val readFunSpec = typeSpec.funSpecs.find {
                    it.annotations.get(FirekoAdapter.Read::class.asClassName()) != null
                }

                val writeFunSpec = typeSpec.funSpecs.find {
                    it.annotations.get(FirekoAdapter.Write::class.asClassName()) != null
                }

                AdapterElement(element, className, typeSpec, readFunSpec, writeFunSpec)
            }
    }
}