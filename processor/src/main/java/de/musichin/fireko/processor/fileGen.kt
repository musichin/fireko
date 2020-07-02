package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import de.musichin.fireko.annotations.Fireko

internal fun generateFile(target: TargetClass, targets: List<TargetClass>): FileSpec {
    val targetTypes = targets.map { it.type }
    val imports = target.includeParams
        .map { it.type.copy(nullable = false) }
        .filter { targetTypes.contains(it) }
        .minus(target.type)
        .filterIsInstance<ClassName>()

    return FileSpec
        .builder(target.packageName, "${target.simpleName}${Fireko::class.java.simpleName}")
        .apply {
            imports.forEach {
                addImport(it.packageName, listOf(toType(it)))
            }
        }
        .addFunction(generateFunReceiverDocumentSnapshot(target))
        .addFunction(generateFunReceiverMap(target))
//    .addFunction(generateFunTargetMap(target))
        .build()
}
