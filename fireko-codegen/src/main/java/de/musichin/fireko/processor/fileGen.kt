package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import de.musichin.fireko.annotations.Fireko

@KotlinPoetMetadataPreview
internal fun generateFile(context: Context, element: ClassElement): FileSpec {
    val target = context.targetClass(element)
        ?: throw IllegalArgumentException("Could not process meta information of ${element.className}")
    val targetTypes = context.classElements.map { it.className }
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
        .addFunction(genDocumentSnapshotDeserializer(context, target))
        .addFunction(genMapDeserializer(context, target))
        .addFunction(genFunObjectToMap(context, target))
        .apply {
            getFirestoreExtensions(target).forEach(this::addFunction)
        }
        .build()
}
