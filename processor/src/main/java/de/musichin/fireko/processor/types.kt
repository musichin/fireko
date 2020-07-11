package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.Fireko

internal val FIREBASE_PROPERTY_NAME =
    ClassName("com.google.firebase.firestore", "PropertyName")
internal val FIREBASE_EXCLUDE =
    ClassName("com.google.firebase.firestore", "Exclude")
internal val FIREBASE_DOCUMENT_SNAPSHOT =
    ClassName("com.google.firebase.firestore", "DocumentSnapshot")
internal val FIREBASE_BLOB =
    ClassName("com.google.firebase.firestore", "Blob")
internal val FIREBASE_TIMESTAMP =
    ClassName("com.google.firebase", "Timestamp")
internal val FIREBASE_GEO_POINT =
    ClassName("com.google.firebase.firestore", "GeoPoint")
internal val FIREBASE_DOCUMENT_REFERENCE =
    ClassName("com.google.firebase.firestore", "DocumentReference")
internal val FIREBASE_DOCUMENT_ID =
    ClassName("com.google.firebase.firestore", "DocumentId")
internal val FIREBASE_SERVER_TIMESTAMP =
    ClassName("com.google.firebase.firestore", "ServerTimestamp")
internal val FIREBASE_FIELD_VALUE =
    ClassName("com.google.firebase.firestore", "FieldValue")
internal val UTIL_DATE =
    ClassName("java.util", "Date")
internal val TIME_INSTANT =
    ClassName("java.time", "Instant")
internal val BP_INSTANT =
    ClassName("org.threeten.bp", "Instant")

internal val EMBEDDED =
    ClassName(Embedded::class.java.`package`.name, Embedded::class.java.simpleName)


internal val FIREBASE_SUPPORTED_TYPES = listOf(
    FIREBASE_DOCUMENT_REFERENCE.copy(nullable = true),
    FIREBASE_TIMESTAMP.copy(nullable = true),
    FIREBASE_GEO_POINT.copy(nullable = true),
    FIREBASE_BLOB.copy(nullable = true),
    BOOLEAN.copy(nullable = true),
    DOUBLE.copy(nullable = true),
    LONG.copy(nullable = true),
    STRING.copy(nullable = true),
    MAP.parameterizedBy(STRING, ANY).copy(nullable = true),
    MAP.parameterizedBy(STRING, ANY.copy(nullable = true)).copy(nullable = true),
    MAP.parameterizedBy(ANY, ANY).copy(nullable = true),
    MAP.parameterizedBy(ANY, ANY.copy(nullable = true)).copy(nullable = true),
    MAP.copy(nullable = true)
)
