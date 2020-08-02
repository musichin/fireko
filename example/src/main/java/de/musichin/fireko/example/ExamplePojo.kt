package de.musichin.fireko.example

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import de.musichin.fireko.annotations.Embedded
import de.musichin.fireko.annotations.Fireko
import java.net.URI
import java.net.URL
import java.time.*
import java.util.*

@Fireko
data class ExamplePojo(
    @DocumentId val id: String,
    @DocumentId val idAsLong: Long,
    @PropertyName("p_str") val pStr: String,
    @Exclude val ignore: String? = null,
    @Embedded val embeddedPojoWithDocId: EmbeddedPojoWithDocId,
    @Embedded val primitivePojo: PrimitivePojo,
    @Embedded val listPojo: ListPojo,
    @Embedded val mapPojo: MapPojo,
    @Embedded val enumPojo: EnumPojo,
    val any: Any,
    val date: Date,
    val instant: Instant,
    val timePojo: TimePojo,
    val geoPoint: GeoPoint,
    val timestamp: Timestamp,
    val androidUri: Uri,
    val uri: URI,
    val url: URL,
    val currency: Currency
)

@Fireko
data class PrimitivePojo(
    val char: Char,
    val byte: Byte,
    val short: Short,
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val number: Number,
    val string: String,
    val charSequence: CharSequence
)

@Fireko
data class ListPojo(
    val listAny: List<Any>,
    val listAnyOpt: List<Any?>,
    val listStringOpt: List<String?>,
    val listInt: List<Int>,
    val listIntOpt: List<Int>?,
    val listListString: List<List<String>>
)

@Fireko
data class MapPojo(
    val mapStringString: Map<String, String>,
    val mapStringInt: Map<String, Int>,
    val mapIntInt: Map<Int, Int>
)

@Fireko
data class EnumPojo(
    val simpleEnum: SimpleEnum,
    val simpleEnumOpt: SimpleEnum?,
    val complexEnum: ComplexEnum
)

@Fireko
data class TimePojo(
    val localDateTime: LocalDateTime,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val offsetDateTime: OffsetDateTime,
    val offsetTime: OffsetTime,
    val zonedDateTime: ZonedDateTime,
    val zoneId: ZoneId,
    val zoneOffset: ZoneOffset,
    val period: Period,
    val duration: Duration
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
