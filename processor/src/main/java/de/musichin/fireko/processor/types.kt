package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName

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
internal val UTIL_DATE =
    ClassName("java.util", "Date")
internal val TIME_INSTANT =
    ClassName("java.time", "Instant")
internal val BP_INSTANT =
    ClassName("org.threeten.bp", "Instant")
