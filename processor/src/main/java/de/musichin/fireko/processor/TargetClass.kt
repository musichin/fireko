package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

data class TargetClass(
    val typeSpec: TypeSpec,
    val type: ClassName,
    val constructor: FunSpec
) {
    val canonicalName: String get() = type.canonicalName
    val simpleName: String get() = type.simpleName
    val packageName: String get() = type.packageName
    val params: List<TargetParameter> = constructor.parameters.map { parameter ->
        TargetParameter(this, parameter)
    }
    val includeParams = params.filter { it.include }
}
