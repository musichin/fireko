package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.NullValue

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

internal val EMBEDDED =
    ClassName(Embedded::class.java.`package`.name, Embedded::class.java.simpleName)
internal val NULL_VALUE =
    ClassName(NullValue::class.java.`package`.name, NullValue::class.java.simpleName)
