package de.musichin.fireko.example

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import de.musichin.fireko.annotations.Fireko
import java.time.Instant
import java.util.*

@Fireko
data class ExamplePojo(
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
    @Exclude val ignore: String? = null,
    val simpleEnum: SimpleEnum,
    val complexEnum: ComplexEnum
//    val a: A,
//    val abc: B,
//    val point: GeoPoint
)

data class B(val l: Long) {
    fun te() {
        val map = mutableMapOf<String, Any>()
        val a: B by map
    }
}

enum class SimpleEnum {
    A,
    B
}

enum class ComplexEnum {
    @PropertyName("sdf")
    X,
    Y
}