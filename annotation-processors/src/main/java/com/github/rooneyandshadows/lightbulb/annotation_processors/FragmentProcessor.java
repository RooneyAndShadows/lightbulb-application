package com.github.rooneyandshadows.lightbulb.annotation_processors;

import com.github.rooneyandshadows.lightbulb.annotation_processors.annotations.BindView;
import com.github.rooneyandshadows.lightbulb.annotation_processors.annotations.FragmentConfiguration;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class FragmentProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private Elements elements;
    private List<ClassInfo> classInfoList;
    private static final String generatedPackage = "com.github.rooneyandshadows.annotations";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
        this.elements = processingEnvironment.getElementUtils();
        this.classInfoList = new ArrayList<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //Get annotated targets
        boolean processResult;
        processResult = obtainAnnotatedClassesWithFragmentConfiguration(roundEnvironment);
        processResult &= obtainAnnotatedFieldsWithBindView(roundEnvironment);
        if (!processResult) return false;
        //Generate methods
        List<MethodSpec> methods = new ArrayList<>();
        classInfoList.forEach(classInfo -> {
            methods.add(generateFragmentConfigurationMethod(classInfo));
            methods.add(generateFragmentViewBindingsMethod(classInfo));
        });
        //Generate class
        TypeSpec.Builder generatedClass = TypeSpec
                .classBuilder("FragmentBindings")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(methods);
        //Create generated class file
        try {
            JavaFile.builder(generatedPackage, generatedClass.build()).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>() {{
            add(BindView.class.getCanonicalName());
            add(FragmentConfiguration.class.getCanonicalName());
        }};
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private boolean obtainAnnotatedClassesWithFragmentConfiguration(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(FragmentConfiguration.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@FragmentConfiguration should be on top of fragment classes.");
                return false;
            }
            ClassInfo classInfo = new ClassInfo();
            classInfo.type = element.asType();
            classInfo.simpleClassName = element.getSimpleName().toString();
            classInfo.fullClassName = getFullClassName(elements, element);
            classInfo.configAnnotation = element.getAnnotation(FragmentConfiguration.class);
            classInfoList.add(classInfo);
        }
        return true;
    }

    private boolean obtainAnnotatedFieldsWithBindView(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)) {
            if (element.getKind() != ElementKind.FIELD) {
                messager.printMessage(Diagnostic.Kind.ERROR, "@BindView should be on top of fragment field.");
                return false;
            }
            BindView annotation = element.getAnnotation(BindView.class);
            Element classElement = element.getEnclosingElement();
            String fullClassName = getFullClassName(elements, classElement);
            ClassInfo classInfo = classInfoList.stream().filter(info -> info.fullClassName.equals(fullClassName))
                    .findFirst()
                    .orElse(null);
            if (classInfo == null) {
                classInfo = new ClassInfo();
                classInfo.type = classElement.asType();
                classInfo.simpleClassName = classElement.getSimpleName().toString();
                classInfoList.add(classInfo);
            }
            classInfo.viewBindings.put(element.getSimpleName().toString(), annotation.name());
        }
        return true;
    }

    private MethodSpec generateFragmentConfigurationMethod(ClassInfo classInfo) {
        boolean hasFragmentConfigAnnotation = classInfo.configAnnotation != null;
        if (!hasFragmentConfigAnnotation)
            return null;
        String layoutName = classInfo.configAnnotation.layoutName();
        String isMainScreenFragment = String.valueOf(classInfo.configAnnotation.isMainScreenFragment());
        String hasLeftDrawer = String.valueOf(classInfo.configAnnotation.hasLeftDrawer());
        String hasOptionsMenu = String.valueOf(classInfo.configAnnotation.hasOptionsMenu());
        return MethodSpec
                .methodBuilder("generate" + classInfo.simpleClassName + "Configuration")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(FragmentConfig.class)
                .addStatement("return new $T($S,$L,$L,$L)", ClassName.get(FragmentConfig.class), layoutName, isMainScreenFragment, hasLeftDrawer, hasOptionsMenu)
                .build();
    }

    private MethodSpec generateFragmentViewBindingsMethod(ClassInfo classInfo) {
        MethodSpec.Builder fragViewBindingMethod = MethodSpec
                .methodBuilder("generate" + classInfo.simpleClassName + "ViewBindings")
                .addParameter(TypeName.get(classInfo.type), "fragment")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class);
        classInfo.viewBindings.forEach((fieldName, identifierName) -> {
            fragViewBindingMethod.addStatement("fragment.$L = fragment.getView().findViewById(fragment.getResources().getIdentifier($S, \"id\", fragment.getActivity().getPackageName()))", fieldName, identifierName);
        });
        return fragViewBindingMethod.build();
    }

    private String getFullClassName(Elements elements, Element element) {
        String classPackage = elements.getPackageOf(element)
                .getQualifiedName()
                .toString();
        String classSimpleName = element.getSimpleName().toString();
        return classPackage.concat(".").concat(classSimpleName);
    }

    private static class ClassInfo {
        private TypeMirror type;
        private String simpleClassName;
        private String fullClassName;
        private FragmentConfiguration configAnnotation;
        private final Map<String, String> viewBindings = new HashMap<>();
    }
}