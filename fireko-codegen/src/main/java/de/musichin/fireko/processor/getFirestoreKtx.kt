package de.musichin.fireko.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.util.Locale
import java.util.concurrent.Executor

@KotlinPoetMetadataPreview
internal fun getFirestoreExtensions(target: TargetClass): List<FunSpec> {
    return listOfNotNull(
        genDataGet(target),
        genDataSet(target),
        genDataSetWithOptions(target),
        genDataAddListener(target),
        genDataAddListenerWithExecutor(target),
        // FIXME Android only
//        genDataAddListenerWithMetadataChanges(target),
//        genDataAddListenerWithExecutorAndMetadataChanges(target),
//        genDataAddListenerWithActivity(target),
//        genDataAddListenerWithActivityAndMetadataChanges(target),

        genCollectionGet(target),
        genCollectionAdd(target),
        //genCollectionSet(target),
        genCollectionAddListener(target),
        genCollectionAddListenerWithExecutor(target),
    )
}

@KotlinPoetMetadataPreview
private fun genDataGet(target: TargetClass) = FunSpec
    .builder("get${target.type.simpleName.capitalize(Locale.US)}")
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .returns(FIREBASE_TASK.parameterizedBy(target.type))
    .addCode(
        CodeBlock.builder()
            .beginControlFlow("return get().onSuccessTask")
            .addStatement("%T.forResult(it?.%L())", FIREBASE_TASKS, toType(target.type))
            .endControlFlow()
            .build()
    )
    .build()

@KotlinPoetMetadataPreview
private fun genDataSet(target: TargetClass) = FunSpec
    .builder("set${target.type.simpleName.capitalize(Locale.US)}")
    .addParameter("data", target.type.asNotNullable())
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.of("return set(data.toMap())"))
    .build()

@KotlinPoetMetadataPreview
private fun genDataSetWithOptions(target: TargetClass) = FunSpec
    .builder("set${target.type.simpleName.capitalize(Locale.US)}")
    .addParameter("data", target.type.asNotNullable())
    .addParameter("options", FIREBASE_SET_OPTIONS)
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.of("return set(data.toMap(), options)"))
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListener(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListenerWithExecutor(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("executor", Executor::class.asClassName())
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(executor) { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListenerWithActivity(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("activity", ClassName("android.app", "Activity"))
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(activity) { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListenerWithMetadataChanges(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("metadataChanges", FIREBASE_METADATA_CHANGES)
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(metadataChanges) { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListenerWithExecutorAndMetadataChanges(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("executor", Executor::class.asClassName())
    .addParameter("metadataChanges", FIREBASE_METADATA_CHANGES)
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(executor, metadataChanges) { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genDataAddListenerWithActivityAndMetadataChanges(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("activity", ClassName("android.app", "Activity"))
    .addParameter("metadataChanges", FIREBASE_METADATA_CHANGES)
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(target.type))
    .receiver(FIREBASE_DOCUMENT_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(activity, metadataChanges) { value, error ->")
        .add("listener.onEvent(value?.%L(), error)", toType(target.type))
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genCollectionGet(target: TargetClass) = FunSpec
    .builder("get${target.type.simpleName.capitalize(Locale.US)}Collection")
    .receiver(FIREBASE_COLLECTION_REFERENCE)
    .returns(FIREBASE_TASK.parameterizedBy(LIST.parameterizedBy(target.type)))
    .addCode(
        CodeBlock.builder()
            .beginControlFlow("return get().onSuccessTask")
            .beginControlFlow("%T.forResult(it?.mapNotNull", FIREBASE_TASKS)
            .add("it?.%L()", toType(target.type))
            .endControlFlow()
            .add(")")
            .endControlFlow()
            .build()
    )
    .build()

@KotlinPoetMetadataPreview
private fun genCollectionAdd(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}")
    .addParameter("data", target.type.asNotNullable())
    .receiver(FIREBASE_COLLECTION_REFERENCE)
    .addCode(CodeBlock.of("return add(data.toMap())"))
    .build()

//@KotlinPoetMetadataPreview
//private fun genCollectionSet(target: TargetClass): FunSpec? {
//    if (!target.needsDocumentId) return null
//
//    return FunSpec
//        .builder("set${target.type.simpleName.capitalize(Locale.US)}")
//        .addParameter("data", target.type.asNotNullable())
//        .receiver(FIREBASE_COLLECTION_REFERENCE)
//        //.addCode(CodeBlock.of("return add(data.toMap())"))
//        .addCode(CodeBlock.builder()
//            .add()
//            .apply {
//                target.documentIdParams.forEach {
//
//                }
//            }
//            .add("return document().set(data)")
//            .build())
//        .build()
//}

@KotlinPoetMetadataPreview
private fun genCollectionAddListener(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(LIST.parameterizedBy(target.type)))
    .receiver(FIREBASE_COLLECTION_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener { value, error ->")
        .add("listener.onEvent(")
        .beginControlFlow("value?.documents?.mapNotNull {")
        .add("it?.%L()", toType(target.type))
        .endControlFlow()
        .add(", error)")
        .endControlFlow()
        .build())
    .build()

@KotlinPoetMetadataPreview
private fun genCollectionAddListenerWithExecutor(target: TargetClass) = FunSpec
    .builder("add${target.type.simpleName.capitalize(Locale.US)}Listener")
    .addParameter("executor", Executor::class.asClassName())
    .addParameter("listener", FIREBASE_EVENT_LISTENER.parameterizedBy(LIST.parameterizedBy(target.type)))
    .receiver(FIREBASE_COLLECTION_REFERENCE)
    .addCode(CodeBlock.builder()
        .beginControlFlow("return addSnapshotListener(executor) { value, error ->")
        .add("listener.onEvent(")
        .beginControlFlow("value?.documents?.mapNotNull {")
        .add("it?.%L()", toType(target.type))
        .endControlFlow()
        .add(", error)")
        .endControlFlow()
        .build())
    .build()
