package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*

private fun sources(target: TypeName): List<TypeName> = when (target.notNullable()) {
    FIREBASE_TIMESTAMP,
    UTIL_DATE,
    TIME_INSTANT,
    BP_INSTANT ->
        listOf(FIREBASE_TIMESTAMP, UTIL_DATE, TIME_INSTANT, BP_INSTANT)
    DOUBLE, FLOAT ->
        listOf(DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR, NUMBER, STRING, CHAR_SEQUENCE)
    LONG, INT, SHORT, BYTE, CHAR ->
        listOf(LONG, INT, SHORT, BYTE, CHAR, NUMBER, DOUBLE, FLOAT, STRING, CHAR_SEQUENCE)
    NUMBER ->
        listOf(NUMBER, DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR, STRING, CHAR_SEQUENCE)
    STRING, CHAR_SEQUENCE ->
        listOf(STRING, CHAR_SEQUENCE)
    ANDROID_URI ->
        listOf(ANDROID_URI, STRING)
    URI ->
        listOf(URI, STRING)
    URL ->
        listOf(URL, STRING)
    CURRENCY ->
        listOf(CURRENCY, STRING)
    else -> listOf(target)
}

private fun select(sources: Collection<TypeName>, target: TypeName): TypeName {
    val sourcesNotNullable = sources.map { it.notNullable() }
    val typeNotNullable = target.notNullable()

    if (target in sources) {
        return target
    }
    val directSelect = sources.find { it.notNullable() == typeNotNullable }
    if (directSelect != null) {
        return directSelect
    }
    val select = sources(target).find { source -> source in sourcesNotNullable }
        ?: throw IllegalArgumentException("Cannot select $target of $sources")

    if (select in sources) {
        return select
    }

    return select.nullable()
}

private fun select(source: TypeName, targets: Collection<TypeName>): TypeName =
    select(targets, source)

private fun CodeBlock.Builder.convert(
    sources: List<TypeName>,
    target: TypeName
): CodeBlock.Builder {
    val source = select(sources, target)
    return convert(source, target)
}

private fun CodeBlock.Builder.convert(
    source: TypeName,
    targets: List<TypeName>
): CodeBlock.Builder {
    val target = select(source, targets)
    return convert(source, target)
}

internal fun CodeBlock.Builder.convert(
    source: TypeName,
    target: TypeName
): CodeBlock.Builder = when {
    target.isAssignable(source) -> this

    source.isNullable && !target.isNullable -> {
        add(".let(::requireNotNull)")
        convert(source.notNullable(), target)
    }

    target.isAny() -> this

    source.isFirebaseTimestamp() && target.isUtilDate() -> {
        call(source)
        add("toDate()")
    }

    source.isFirebaseTimestamp() && target.isInstant() -> {
        call(source)
        add("let { %T.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }", target.notNullable())
    }

    source.isNumber() && target.isNumber() ->
        convertNumber(source, target)

    source.isString() && target.isCharSequence() ->
        this

    source.isCharSequence() && target.isString() -> {
        call(source)
        add("toString()")
    }

    source.isString() && target.isNumber() ->
        convertNumber(source, target)

    source.isUtilDate() && target.isFirebaseTimestamp() ->
        call(source).add("let(::%T)", FIREBASE_TIMESTAMP)

    source.isInstant() && target.isFirebaseTimestamp() -> {
        call(source)
        add("let { %T(it.epochSecond, it.nano) }", FIREBASE_TIMESTAMP)
    }

    source.isOneOf(STRING) && target.isOneOf(ANDROID_URI) -> {
        call(source)
        add("let(%T::parse)", ANDROID_URI)
    }

    source.isOneOf(ANDROID_URI) && target.isOneOf(STRING) -> {
        call(source)
        add("toString()")
    }

    source.isOneOf(STRING) && target.isOneOf(URI, URL) -> {
        call(source)
        add("let(::%T)", target.notNullable())
    }

    source.isOneOf(URI, URL) && target.isOneOf(STRING) -> {
        call(source)
        add("toString()")
    }

    source.isOneOf(STRING) && target.isOneOf(CURRENCY) -> {
        call(source)
        add("let(%T::getInstance)", target.notNullable())
    }

    source.isOneOf(CURRENCY) && target.isOneOf(STRING) -> {
        call(source)
        add("toString()")
    }

    else -> throwConversionError(source, target)
}

private fun throwConversionError(source: TypeName, target: TypeName): Nothing =
    throw IllegalArgumentException("Cannot convert from $source to $target")

private fun CodeBlock.Builder.convertNumber(source: TypeName, target: TypeName): CodeBlock.Builder {
    if (source.copy(nullable = false) == target.copy(nullable = false)) {
        return this
    }

    if (source.isNumber() && target.copy(nullable = false) == NUMBER) {
        return this
    }

    if (source.isNumber()) {
        return invokeToType(source.isNullable, (target as ClassName).simpleName)
    }

    if (source.copy(nullable = false) == STRING) {
        return invokeToType(source.isNullable, (target as ClassName).simpleName)
    }

    // TODO Rational
    // TODO BigDecimal

    throwConversionError(source, target)
}
