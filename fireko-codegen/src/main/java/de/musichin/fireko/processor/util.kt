package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.AnnotationMirror

operator fun CodeBlock.plus(other: CodeBlock): CodeBlock {
    return CodeBlock.Builder().add(this).add(other).build()
}

fun List<AnnotationSpec>.get(typeName: TypeName): AnnotationSpec? =
    find { it.typeName == typeName }

internal fun AnnotationSpec.value(property: String): Any? =
    tag<AnnotationMirror>()
        ?.elementValues
        ?.entries
        ?.singleOrNull { it.key.simpleName.toString() == property }
        ?.value
        ?.value

fun TypeName.isAssignable(initializer: TypeName): Boolean {
    if (this == initializer) return true
    if (this.asNotNullable() != initializer.asNotNullable()) return false
    return this.isNullable
}

fun <T : TypeName> T.asNotNullable(): T = nullable(false)
fun <T : TypeName> T.asNullable(): T = nullable(true)
fun <T : TypeName> T.nullable(nullable: Boolean): T = copy(nullable = nullable) as T

fun TypeName.isOneOf(vararg typeNames: TypeName): Boolean {
    val thisNotNullable = (this as? ParameterizedTypeName)?.rawType ?: this.asNotNullable()
    return typeNames.any { typeName -> thisNotNullable == typeName.asNotNullable() }
}

val TypeSpec.isData: Boolean get() = modifiers.contains(KModifier.DATA)

fun TypeName.isAny(): Boolean =
    isOneOf(ANY)

fun TypeName.isString(): Boolean =
    isOneOf(STRING)

fun TypeName.isBoolean(): Boolean =
    isOneOf(BOOLEAN)

fun TypeName.isList(): Boolean =
    isOneOf(LIST, COLLECTION, ITERABLE)

fun TypeName.isMap(): Boolean =
    isOneOf(MAP)

fun TypeName.isFirebaseTimestamp(): Boolean =
    isOneOf(FIREBASE_TIMESTAMP)

fun TypeName.isFirebaseGeoPoint(): Boolean =
    isOneOf(FIREBASE_GEO_POINT)

fun TypeName.isFirebaseBlob(): Boolean =
    isOneOf(FIREBASE_BLOB)

fun TypeName.isFirebaseDocumentReference(): Boolean =
    isOneOf(FIREBASE_DOCUMENT_REFERENCE)
