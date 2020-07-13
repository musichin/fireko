package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import de.musichin.fireko.annotations.Fireko

@KotlinPoetMetadataPreview
internal fun generateFile(context: Context, element: TargetElement): FileSpec {
    val target = context.targetClass(element)
        ?: throw IllegalArgumentException("Could not process meta information of ${element.className}")
    val targetTypes = context.targetElements.map { it.className }
    val imports = target.params
        .map { it.type.copy(nullable = false) }
        .filter { targetTypes.contains(it) }
        .minus(target.type)
        .filterIsInstance<ClassName>()

    return FileSpec
        .builder(target.packageName, "${target.simpleName}${Fireko::class.java.simpleName}")
        .apply {
            imports.forEach {
                addImport(it.packageName, listOf(toType(it)))
                addImport(it.packageName, "toMap")
            }
        }
        .addFunction(generateFunReceiverDocumentSnapshot(target))
        .addFunction(generateFunReceiverMap(target))
        .addFunction(generateFunTargetMap(target))
        .build()
}
