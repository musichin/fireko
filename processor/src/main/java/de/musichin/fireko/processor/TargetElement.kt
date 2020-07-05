package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.element.Element

class TargetElement(
    val element: Element,
    val className: ClassName,
    val typeSpec: TypeSpec
)
