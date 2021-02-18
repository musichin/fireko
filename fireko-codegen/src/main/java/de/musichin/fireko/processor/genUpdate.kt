package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.util.Locale

@KotlinPoetMetadataPreview
internal fun genUpdateClass(context: Context, targetClass: TargetClass): TypeSpec {
    val updateVariableName = targetClass.findFreeParamName("update")
    return TypeSpec.classBuilder(updateClassName(targetClass))
        .addProperty(
            PropertySpec.builder(
                updateVariableName,
                MUTABLE_MAP.parameterizedBy(STRING, ANY.asNullable()),
                KModifier.PRIVATE
            ).initializer("mutableMapOf()").build()
        )
        .addFunction(
            FunSpec.builder("toMap")
                .addCode("return %L.toMap()", updateVariableName)
                .build()
        )
        .apply {
            val propertyParams = (targetClass.params - targetClass.documentIdParams)
            propertyParams.forEach { param ->
                val innerClass = createInnerClass(context, targetClass, param, updateVariableName)
                val innerType = updateClassType(targetClass, param)
                addType(innerClass)
                addProperty(
                    PropertySpec.builder(param.name, innerType)
                        .initializer("%T()", innerType)
                        .build()
                )
                addFunction(
                    FunSpec.builder("to")
                        .addModifiers(KModifier.INFIX)
                        .receiver(innerType)
                        .addParameter(param.name, param.type)
                        .addCode("set(%L)", param.name)
                        .build()
                )
            }
        }
        .build()
}

@KotlinPoetMetadataPreview
private fun updateClassName(target: TargetClass) = "${target.type.simpleName}Update"

@KotlinPoetMetadataPreview
private fun updateClassName(target: TargetParameter) = "${target.name.capitalize(Locale.US)}Update"

@KotlinPoetMetadataPreview
private fun updateClassType(targetClass: TargetClass, target: TargetParameter) =
    ClassName(targetClass.packageName, updateClassName(targetClass), updateClassName(target))

@KotlinPoetMetadataPreview
private fun createInnerClass(
    context: Context,
    targetClass: TargetClass,
    param: TargetParameter,
    varName: String
): TypeSpec {
    val adapter = param.usingAdapter?.let { context.adapterElement(it) }
        ?: context.getAnnotatedAdapter(param.type)

    val targetType = if (adapter != null)
        requireNotNull(adapter.writeFunSpec?.returnType)
    else
        param.type

    val targetValueType = ValueType.valueOf(context, targetType)

    return TypeSpec.classBuilder(updateClassName(param))
        .addModifiers(KModifier.INNER)
        .addFunction(
            // TODO only for "nullable" types
            FunSpec.builder("delete")
                .addCode(
                    "%L[%S] = %T.delete()",
                    varName,
                    param.propertyName,
                    FIREBASE_FIELD_VALUE
                )
                .build()
        )
        .addFunction(
            // FIXME type conversion
            FunSpec.builder("set")
                .addParameter(param.name, param.type)
                .addCode(
                    "%L[%S] = %L",
                    varName,
                    param.propertyName,
                    param.name
                )
                .build()
        )
        .apply {
            if (targetValueType == ValueType.TIMESTAMP) {
                addFunction(
                    FunSpec.builder("serverTimestamp")
                        .addCode(
                            "%L[%S] = %T.serverTimestamp()",
                            varName,
                            param.propertyName,
                            FIREBASE_FIELD_VALUE
                        )
                        .build()
                )
            } else if (targetType == ARRAY) {

            }
        }
        .build()
}

@KotlinPoetMetadataPreview
private fun getTimestampFunctions(param: TargetParameter, varName: String): List<FunSpec> = listOf(
    FunSpec.builder("serverTimestamp")
        .addCode(
            "%L[%S] = %T.serverTimestamp()",
            varName,
            param.propertyName,
            FIREBASE_FIELD_VALUE
        )
        .build(),
)

@KotlinPoetMetadataPreview
private fun getTimestampFun(
    context: Context,
    param: TargetParameter,
    varName: String,
    type: TypeName
): FunSpec =
    FunSpec.builder("set")
        .addParameter(param.name, param.type)
        .addCode(
            CodeBlock.builder()
                .serialize(context, type)
                .build()
        )
        .addCode(
            "%L[%S] = %L",
            varName,
            param.propertyName,
        )

        .build()