package de.musichin.fireko.example

import com.google.firebase.firestore.*
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.Fireko
import java.time.Instant
import java.util.*

@Fireko
data class ExamplePojo(
    val firebaseGeoPoint: GeoPoint,
//    @DocumentId val id: String,
    @DocumentId val idAsLong: Long,
    val str: String,
    @PropertyName("p_str") val pStr: String,
    val byte: Byte,
    val short: Short,
    val int: Int = 3,
    val intOpt: Int?,
    val longOpt: Long? = null,
    val number: Number? = null,
    val float: Float,
    val date: Date,
    val instant: Instant,
    val charSequence: CharSequence,
    @Exclude val ignore: String? = null,
    val simpleEnum: SimpleEnum,
    val complexEnum: ComplexEnum,
    @Embedded val embeddedPojo: EmbeddedPojo,
    @Embedded val embeddedPojoWithDocId: EmbeddedPojoWithDocId,
    val point: GeoPoint
)

@Fireko
data class EmbeddedPojo(
    val a: String
)

@Fireko
data class EmbeddedPojoWithDocId(
    @DocumentId
    val idEmbedded: String
)

enum class SimpleEnum {
    A,
    B
}

enum class ComplexEnum {
    @PropertyName("sdf")
    X,
    Y
}