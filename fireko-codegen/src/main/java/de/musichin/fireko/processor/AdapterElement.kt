package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.element.Element

data class AdapterElement(
    val element: Element,
    val className: ClassName,
    val typeSpec: TypeSpec,
    val readFunSpec: FunSpec?,
    val writeFunSpec: FunSpec?,
) {
    init {
        check(typeSpec.kind == TypeSpec.Kind.OBJECT) {
            "$className has to be an object."
        }
    }
}
