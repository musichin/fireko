package de.musichin.fireko.example

import com.google.firebase.firestore.*
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.Fireko
import java.time.Instant
import java.util.*

@Fireko
data class ExamplePojo(
    val any: Any,
    val firebaseGeoPoint: GeoPoint,
    @DocumentId val id: String,
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
    val simpleEnumOpt: SimpleEnum?,
    val complexEnum: ComplexEnum,
    val point: GeoPoint,
    @Embedded val embeddedPojo: EmbeddedPojo?,
    @Embedded val embeddedPojoWithDocId: EmbeddedPojoWithDocId,
    val anotherPojo: AnotherPojo,
    val listAny: List<Any>,
    val listAnyOpt: List<Any?>,
    val listStringOpt: List<String?>,
    val listInt: List<Int>,
    val listIntOpt: List<Int>?,
    val listListString: List<List<String>>,
    val mapStringString: Map<String, String>,
    val mapStringInt: Map<String, Int>,
    val mapIntInt: Map<Int, Int>
)

@Fireko
data class AnotherPojo(
    @ServerTimestamp val d: Instant?
)

@Fireko
data class EmbeddedPojo(
    val a: String,
    val b: Int
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
    @PropertyName("x")
    X,
    Y
}
