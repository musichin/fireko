package de.musichin.fireko.annotations

/**
 * Defines a behavior for NULL values.
 *
 * @property omit if true will omit the property when value is `null`.
 * @property preset when null value is read, it will fallback to default value when provided.
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.SOURCE)
annotation class NullValue(val omit: Boolean = false, val preset: Boolean = false)
