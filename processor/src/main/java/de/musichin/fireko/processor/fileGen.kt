package de.musichin.fireko.processor

import com.squareup.kotlinpoet.FileSpec
import de.musichin.fireko.annotations.Fireko

internal fun generateFile(target: TargetType, targets: List<TargetType>) = FileSpec
    .builder(target.packageName, "${target.simpleName}${Fireko::class.java.simpleName}")
    .addFunction(generateFunReceiverDocumentSnapshot(target, targets))
    .addFunction(generateFunReceiverMap(target, targets))
//    .addFunction(generateFunTargetMap(target))
    .build()
