package de.musichin.fireko.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import de.musichin.fireko.annotations.Fireko
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

@KotlinPoetMetadataPreview
@AutoService(Processor::class)
class FirekoAnnotationProcessor : AbstractProcessor() {
    private lateinit var classInspector: ClassInspector
    private lateinit var elements: Elements

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(Fireko::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment?
    ): Boolean {
        val targets = roundEnv?.getElementsAnnotatedWith(Fireko::class.java)
            ?.mapNotNull { element ->
                TargetClass.create(element, elements, classInspector)
            }

        targets?.forEach { target ->
            process(target, targets)
        }

        return false
    }

    override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elements = env.elementUtils
        classInspector = ElementsClassInspector.create(elements, env.typeUtils)
    }

    private fun process(targetClass: TargetClass, targets: List<TargetClass>) {
        val fileSpec = generateFile(targetClass, targets)
        writeToFile(fileSpec)
    }

    private fun writeToFile(fileSpec: FileSpec) {
        val dist = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]?.let(::File) ?: return
        fileSpec.writeTo(dist)
    }

    companion object {
        private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}
