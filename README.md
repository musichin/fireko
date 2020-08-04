# Firestore Kotlin Data Convertor [ ![Download](https://api.bintray.com/packages/musichin/maven/fireko/images/download.svg)](https://bintray.com/musichin/maven/fireko/_latestVersion) [![Kotlin](https://img.shields.io/badge/Kotlin-1.3.72-blue.svg)](http://kotlinlang.org) ![CI](https://github.com/musichin/fireko/workflows/CI/badge.svg)

Fireko does generate functions to convert `DocumentSnapshot` to your (data) classes. What you have to do is to annotate it with `Fireko` and the rest is done automatically.

Advantages over using [DocumentSnapshot::toObject](https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/DocumentSnapshot#toObject(java.lang.Class%3CT%3E)):
* Empty constructor is not required
* No reflections
* More types

## Usage
```kotlin
@Fireko
data class User(
    @DocumentId val id: String,
    val firstName: String, 
    val lastName: String,
    @ServerTimestamp val createdAt: Instant? = null,
    @Embedded val address: Address,
)

@Fireko
data class Address(
    val street: String,
    val city: String,
    @PropertyName("zip_code") val zipCode: String,
    val country: String,
)

// query user
FirebaseFirestore.getInstance()
    .collection("users")
    .document("user1")
    .addSnapshotListener { snapshot, _ ->
        val user = snapshot?.toUser() // toUser is generated
        // ...
    }

// store user
FirebaseFirestore.getInstance()
    .collection("users")
    .document("user1")
    .set(user.toMap()) // toMap is generated
    .addOnCompleteListener { 
        // ...
    }
```

## Additionally supported types:
* `java.time.*`
* `org.threeten.bp.*`
* `java.net.URI`
* `java.net.URL`
* `android.net.Uri`
* `java.util.Currency`
* `android.icu.util.Currency`

## Binaries
```groovy
repositories {
    maven { url 'https://dl.bintray.com/musichin/maven' }
}

dependencies {
    def fireko_version = 'x.y.z'
    implementation "de.musichin.fireko:fireko:$fireko_version"
    kapt "de.musichin.fireko:fireko-codegen:$fireko_version"
}
```

## Contributing
Contributors are more than welcome. Just upload a PR with a description of your changes.

## Issues or feature requests?
File github issues for anything that is unexpectedly broken.

## License

    MIT License

    Copyright (c) 2020 Anton Musichin

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
