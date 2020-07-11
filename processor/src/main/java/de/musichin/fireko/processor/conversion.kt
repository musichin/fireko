package de.musichin.fireko.processor

import com.squareup.kotlinpoet.*

private fun sources(target: TypeName): List<TypeName> = when (target.notNullable) {
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
    else -> listOf(target)
}

internal fun select(sources: Collection<TypeName>, target: TypeName): TypeName {
    val sourcesNotNullable = sources.map { it.notNullable }
    val typeNotNullable = target.notNullable

    if (target in sources) {
        return target
    }
    val directSelect = sources.find { it.notNullable == typeNotNullable }
    if (directSelect != null) {
        return directSelect
    }
    val select = sources(target).find { source -> source in sourcesNotNullable }
        ?: throw IllegalArgumentException("Cannot select $target of $sources")

    if (select in sources) {
        return select
    }

    return select.nullable
}

internal fun select(source: TypeName, targets: Collection<TypeName>): TypeName =
    select(targets, source)

internal fun CodeBlock.Builder.convert(
    sources: List<TypeName>,
    target: TypeName
): CodeBlock.Builder {
    val source = select(sources, target)
    return convert(source, target)
}

internal fun CodeBlock.Builder.convert(
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
        convert(source.notNullable, target)
    }

    source.isFirebaseTimestamp() && target.isUtilDate() -> {
        call(source)
        add("toDate()")
    }

    source.isFirebaseTimestamp() && target.isInstant() -> {
        call(source)
        beginControlFlow("let")
        addStatement(
            "%T.ofEpochSecond(it.seconds, it.nanoseconds.toLong())",
            target.notNullable
        )
        endControlFlow()
    }

    source.isNumber() && target.isNumber() ->
        convertNumber(source, target)

    source.isCharSequence() && target.isString() -> {
        call(source)
        add("toString()")
    }

    source.isString() && target.isCharSequence() ->
        this

    source.isString() && target.isNumber() ->
        convertNumber(source, target)

    source.isUtilDate() && target.isFirebaseTimestamp() ->
        call(source).add("let(::%T)", FIREBASE_TIMESTAMP)

    source.isInstant() && target.isFirebaseTimestamp() -> {
        call(source)
        beginControlFlow("let")
        addStatement("%T(it.epochSecond, it.nano)", FIREBASE_TIMESTAMP)
        endControlFlow()
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

private fun TypeName.isOneOf(vararg typeNames: TypeName): Boolean {
    val thisNotNullable = this.notNullable
    return typeNames.any { typeName -> thisNotNullable == typeName.notNullable }
}

private fun TypeName.isNumber(): Boolean =
    isOneOf(NUMBER, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)

private fun TypeName.isInstant(): Boolean =
    isOneOf(TIME_INSTANT, BP_INSTANT)

private fun TypeName.isFirebaseTimestamp(): Boolean =
    isOneOf(FIREBASE_TIMESTAMP)

private fun TypeName.isUtilDate(): Boolean =
    isOneOf(UTIL_DATE)

private fun TypeName.isString(): Boolean =
    isOneOf(STRING)

private fun TypeName.isCharSequence(): Boolean =
    isOneOf(CHAR_SEQUENCE)