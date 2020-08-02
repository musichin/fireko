package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*
import de.musichin.fireko.annotations.Embedded

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
internal val ANDROID_URI =
    ClassName("android.net", "Uri")
internal val URI =
    ClassName("java.net", "URI")
internal val URL =
    ClassName("java.net", "URL")
internal val CURRENCY =
    ClassName("java.util", "Currency")

internal val EMBEDDED =
    ClassName(Embedded::class.java.`package`.name, Embedded::class.java.simpleName)
