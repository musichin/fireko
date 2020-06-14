package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*

internal fun generateFun(
    target: TargetType,
    targets: List<TargetType>,
    receiver: TypeName,
    propertyGen: (TargetType, ParameterSpec, List<TargetType>) -> PropertySpec
) = FunSpec
    .builder("to${target.simpleName.capitalize()}")
    .receiver(receiver)
    .returns(target.type)
    .apply {
        val params = excludeParameters(target, target.constructor.parameters)
        params.forEach { parameter ->
            addCode("%L", propertyGen(target, parameter, targets))
        }

        val paramNames = params.map { it.name }.joinToString(", ") { "$it = $it" }

        addCode("return ${target.simpleName}($paramNames)")
    }
    .build()



private fun excludeParameters(
    targetType: TargetType,
    parameters: List<ParameterSpec>
): List<ParameterSpec> = parameters.filter { param ->
    param.propertyAnnotations(targetType.typeSpec)
        .none { annotation -> annotation.typeName == FIREBASE_EXCLUDE }
}
