package de.musichin.fireko.processor

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

internal fun generateFun(
    target: TargetClass,
    receiver: TypeName,
    propertyGen: (TargetParameter) -> PropertySpec
) = FunSpec
    .builder(toType(target.type))
    .receiver(receiver)
    .returns(target.type)
    .apply {
        val params = target.includeParams
        params.forEach { parameter ->
            addCode("%L", propertyGen(parameter))
        }

        val paramNames = params.map { it.name }.joinToString(", ") { "$it = $it" }

        addCode("return ${target.simpleName}($paramNames)")
    }
    .build()
