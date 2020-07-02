package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.Element
import javax.lang.model.util.Elements

internal class TargetClass private constructor(
    val typeSpec: TypeSpec,
    val type: ClassName,
    val constructor: FunSpec,
    val params: List<TargetParameter>
) {
    val canonicalName: String get() = type.canonicalName
    val simpleName: String get() = type.simpleName
    val packageName: String get() = type.packageName

    val includeParams = params.filter { it.include }

    companion object {
        @KotlinPoetMetadataPreview
        fun create(
            element: Element,
            elements: Elements,
            classInspector: ClassInspector
        ): TargetClass? {
            val meta = element.getAnnotation(Metadata::class.java)
            val kmClass = meta.toImmutableKmClass()
            val className = ClassInspectorUtil.createClassName(kmClass.name)
            val typeSpec = kmClass.toTypeSpec(classInspector)
            val constructor = typeSpec.primaryConstructor ?: return null

            val params = constructor.parameters.map { parameter ->
                TargetParameter.create(typeSpec, parameter, elements, classInspector)
            }

            return TargetClass(typeSpec, className, constructor, params)
        }
    }
}
