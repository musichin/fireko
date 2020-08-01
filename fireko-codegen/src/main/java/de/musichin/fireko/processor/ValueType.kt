package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.BOOLEAN as LANG_BOOLEAN
import com.squareup.kotlinpoet.DOUBLE as LANG_DOUBLE
import com.squareup.kotlinpoet.LIST as UTIL_LIST
import com.squareup.kotlinpoet.LONG as LANG_LONG
import com.squareup.kotlinpoet.MAP as UTIL_MAP
import com.squareup.kotlinpoet.NUMBER as LANG_NUMBER
import com.squareup.kotlinpoet.STRING as LANG_STRING

internal enum class ValueType {
    //    NULL,
    BOOLEAN,
    INTEGER,
    DOUBLE,
    TIMESTAMP,
    STRING,
    BYTES,
    REFERENCE,
    GEO_POINT,
    ARRAY,
    MAP,
    ;

    companion object {
        @KotlinPoetMetadataPreview
        fun valueOf(context: Context, type: TypeName): ValueType = when {
            type.isCharSequence() -> STRING
            type.isOneOf(LANG_NUMBER) -> DOUBLE
            type.isFloating() -> DOUBLE
            type.isNumber() -> INTEGER
            type.isBoolean() -> BOOLEAN
            type.isMap() -> MAP
            type.isList() -> ARRAY
            type.isFirebaseTimestamp() -> TIMESTAMP
            type.isInstant() -> TIMESTAMP
            type.isUtilDate() -> TIMESTAMP
            type.isFirebaseGeoPoint() -> GEO_POINT
            type.isFirebaseBlob() -> BYTES
            type.isFirebaseDocumentReference() -> REFERENCE

            context.isEnum(type) -> STRING
            context.isPojo(type) -> MAP

            else -> throw IllegalArgumentException("Unsupported type $type")
        }

        @KotlinPoetMetadataPreview
        fun typeOf(context: Context, type: TypeName): TypeName {
            if (type.isOneOf(ANY)) return type

            if (context.isPojo(type)) return UTIL_MAP.parameterizedBy(LANG_STRING, ANY.nullable())

            val result = when (valueOf(context, type)) {
                BOOLEAN -> LANG_BOOLEAN
                INTEGER -> LANG_LONG
                DOUBLE -> LANG_DOUBLE
                TIMESTAMP -> FIREBASE_TIMESTAMP
                STRING -> LANG_STRING
                BYTES -> FIREBASE_BLOB
                REFERENCE -> FIREBASE_DOCUMENT_REFERENCE
                GEO_POINT -> FIREBASE_GEO_POINT
                ARRAY -> {
                    type as ParameterizedTypeName
                    val argType = type.typeArguments[0]

                    UTIL_LIST.parameterizedBy(typeOf(context, argType))
                }
                MAP -> {
                    type as ParameterizedTypeName
                    val valueType = type.typeArguments[1]

                    UTIL_MAP.parameterizedBy(
                        LANG_STRING,
                        typeOf(context, valueType)
                    )
                }
            }

            return result.copy(nullable = type.isNullable)
        }
    }
}
