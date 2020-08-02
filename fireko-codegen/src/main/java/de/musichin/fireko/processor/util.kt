package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*

operator fun CodeBlock.plus(other: CodeBlock): CodeBlock {
    return CodeBlock.Builder().add(this).add(other).build()
}

fun List<AnnotationSpec>.get(typeName: TypeName): AnnotationSpec? =
    find { it.typeName == typeName }

fun TypeName.isAssignable(initializer: TypeName): Boolean {
    if (this == initializer) return true
    if (this.notNullable() != initializer.notNullable()) return false
    return this.isNullable
}

fun <T : TypeName> T.notNullable(): T = copy(nullable = false) as T
fun <T : TypeName> T.nullable(): T = copy(nullable = true) as T

fun TypeName.isOneOf(vararg typeNames: TypeName): Boolean {
    val thisNotNullable = (this as? ParameterizedTypeName)?.rawType ?: this.notNullable()
    return typeNames.any { typeName -> thisNotNullable == typeName.notNullable() }
}

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
