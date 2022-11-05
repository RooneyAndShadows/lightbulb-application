package com.github.rooneyandshadows.lightbulb.annotation_processors;

import com.github.rooneyandshadows.lightbulb.annotation_processors.annotations.FragmentConfiguration;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class FragmentProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private Elements elements;
    private List<ClassName> classList;
    private List<ClassInfo> informationList;

    private final ClassName classContext = ClassName.get("android.content", "Context");
    private final ClassName classIntent = ClassName.get("android.content", "Intent");
    private static final String generatedPackage = "me.aflak.annotations";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
        this.elements = processingEnvironment.getElementUtils();
        this.classList = new ArrayList<>();
        this.informationList = new ArrayList<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        /**
         *      1) Getting annotated classes
         */
        for (Element element : roundEnvironment.getElementsAnnotatedWith(FragmentConfiguration.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@FragmentConfiguration should be on top of fragment classes");
                return false;
            }
            ClassInfo classInfo = new ClassInfo();
            classInfo.className = ClassName.get(elements.getPackageOf(element).getQualifiedName().toString(),
                    element.getSimpleName().toString());
            classInfo.configAnnotation = element.getAnnotation(FragmentConfiguration.class);
            informationList.add(classInfo);

            //classList.add(ClassName.get(elements.getPackageOf(element).getQualifiedName().toString(),
            //        element.getSimpleName().toString()));
        }

        /**
         *      2) For each annotated class, generate new static method
         */

        List<MethodSpec> methods = new ArrayList<>();

        informationList.forEach(classInfo -> {
            boolean hasFragmentConfigAnnotation = classInfo.configAnnotation != null;
            String simpleName = classInfo.className.simpleName();
            ClassName fragConfigClassName = ClassName.get(FragmentConfig.class);
            if (hasFragmentConfigAnnotation) {
                String layoutName = classInfo.configAnnotation.layoutName();
                String isMainScreenFragment = String.valueOf(classInfo.configAnnotation.isMainScreenFragment());
                String hasLeftDrawer = String.valueOf(classInfo.configAnnotation.hasLeftDrawer());
                String hasOptionsMenu = String.valueOf(classInfo.configAnnotation.hasOptionsMenu());
                MethodSpec generateFragmentConfigMethod = MethodSpec
                        .methodBuilder("generateFragmentConfiguration" + simpleName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(FragmentConfig.class)
                        .addStatement("return new $T($S,$L,$L,$L)", fragConfigClassName, layoutName, isMainScreenFragment, hasLeftDrawer, hasOptionsMenu)
                        .build();
                methods.add(generateFragmentConfigMethod);
            }
        });


        for (ClassName className : classList) {
            MethodSpec generateFragmentConfigMethod = MethodSpec
                    .methodBuilder("generateFragmentConfiguration" + className.simpleName())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(FragmentConfig.class)
                    .addStatement("$T fragConfiguration = new $T()")
                    .addParameter(classContext, "context")
                    .addStatement("$T intent = new $T(context, $T.class)", classIntent, classIntent, className)
                    .addStatement("context.startActivity(intent)")
                    .build();

            methods.add(generateFragmentConfigMethod);
        }

        /**
         *      3) Generate a class called Navigator that contains the static methods
         */

        TypeSpec.Builder generatedClass = TypeSpec
                .classBuilder("Navigator")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(methods);

        /**
         *      4) Write Navigator class into file
         */

        try {
            JavaFile.builder(generatedPackage, generatedClass.build()).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(FragmentConfiguration.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private static class ClassInfo {
        public ClassName className;
        public FragmentConfiguration configAnnotation;
    }
}