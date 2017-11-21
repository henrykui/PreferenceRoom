/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.processor;

import android.content.Context;
import android.support.annotation.NonNull;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class PreferenceComponentGenerator {

    private final PreferenceComponentAnnotatedClass annotatedClazz;
    private final Map<String, PreferenceEntityAnnotatedClass> annotatedEntityMap;

    private static final String CLAZZ_PREFIX = "PreferenceComponent_";
    private static final String ENTITY_PREFIX = "Preference_";
    private static final String FIELD_INSTANCE = "instance";
    private static final String CONSTRUCTOR_CONTEXT = "context";

    public PreferenceComponentGenerator(@NonNull PreferenceComponentAnnotatedClass annotatedClass, @NonNull Map<String, PreferenceEntityAnnotatedClass> annotatedEntityMap) {
        this.annotatedClazz = annotatedClass;
        this.annotatedEntityMap = annotatedEntityMap;
    }

    public TypeSpec generate() {
        return TypeSpec.classBuilder(getClazzName())
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(PUBLIC)
                .addField(getInstanceFieldSpec())
                .addFields(getEntityInstanceFieldSpecs())
                .addMethod(getConstructorSpec())
                .addMethod(getInstanceSpec())
                .addMethods(getEntityInstanceSpecs())
                .build();
    }

    private FieldSpec getInstanceFieldSpec() {
        return FieldSpec.builder(getClassType(), FIELD_INSTANCE, PRIVATE, STATIC).build();
    }

    private List<FieldSpec> getEntityInstanceFieldSpecs() {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        this.annotatedClazz.keyNames.forEach(keyName -> {
            FieldSpec instance = FieldSpec.builder(getEntityClassType(annotatedEntityMap.get(keyName)),
                    getEntityInstanceFieldName(keyName), PRIVATE, STATIC).build();
            fieldSpecs.add(instance);
        });
        return fieldSpecs;
    }

    private MethodSpec getConstructorSpec() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addParameter(ParameterSpec.builder(Context.class, CONSTRUCTOR_CONTEXT).addAnnotation(NonNull.class).build());

        this.annotatedClazz.keyNames.forEach(keyName ->
            builder.addStatement("$N = $N.getInstance($N.getApplicationContext())", getEntityInstanceFieldName(keyName), getEntityClazzName(annotatedEntityMap.get(keyName)), CONSTRUCTOR_CONTEXT));

        return builder.build();
    }

    private MethodSpec getInstanceSpec() {
        return MethodSpec.methodBuilder("inject")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(Context.class, CONSTRUCTOR_CONTEXT).addAnnotation(NonNull.class).build())
                .addStatement("if($N != null) return $N", FIELD_INSTANCE, FIELD_INSTANCE)
                .addStatement("$N = new $N($N)", FIELD_INSTANCE, getClazzName(), CONSTRUCTOR_CONTEXT)
                .addStatement("return $N", FIELD_INSTANCE)
                .returns(getClassType())
                .build();
    }

    private List<MethodSpec> getEntityInstanceSpecs() {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        this.annotatedClazz.keyNames.forEach(keyName -> {
            String fieldName = getEntityInstanceFieldName(keyName);
            MethodSpec instance = MethodSpec.methodBuilder(StringUtils.toUpperCamel(keyName))
                    .addModifiers(PUBLIC, STATIC)
                    .addStatement("if($N != null) return $N", fieldName, fieldName)
                    .addStatement("throw new NullPointerException($S)", "can not access entity before injecting context on the component.")
                    .returns(getEntityClassType(annotatedEntityMap.get(keyName)))
                    .build();
            methodSpecs.add(instance);
        });
        return methodSpecs;
    }

    private ClassName getClassType() {
        return ClassName.get(annotatedClazz.packageName, getClazzName());
    }

    private String getClazzName() {
        return CLAZZ_PREFIX + annotatedClazz.clazzName;
    }

    private ClassName getEntityClassType(PreferenceEntityAnnotatedClass annotatedClass) {
        return ClassName.get(annotatedClazz.packageName, getEntityClazzName(annotatedClass));
    }

    private String getEntityClazzName(PreferenceEntityAnnotatedClass annotatedClass) {
        return ENTITY_PREFIX + annotatedClass.preferenceName;
    }

    private String getEntityInstanceFieldName(String keyName) {
        return FIELD_INSTANCE + StringUtils.toUpperCamel(keyName);
    }
}
