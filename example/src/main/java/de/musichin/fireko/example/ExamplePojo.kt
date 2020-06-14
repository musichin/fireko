package de.musichin.fireko.example

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import de.musichin.fireko.annotations.Fireko
import de.musichin.fireko.example.more.A
import java.time.Instant
import java.util.*

@Fireko
data class ExamplePojo(
    @DocumentId val id: String,
    val str: String,
    @PropertyName("p_str") val pStr: String,
    val byte: Byte,
    val short: Short,
    val int: Int = 3,
    val longOpt: Long? = null,
    val date: Date,
    val blob: ByteArray,
    val instant: Instant,
    @Exclude val ignore: String? = null,
    val a: A,
    val abc: B,
    val point: GeoPoint
)

data class B(val l: Long)
