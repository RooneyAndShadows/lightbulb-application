package com.github.rooneyandshadows.lightbulb.annotation_processors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FragmentConfiguration {
    String layoutName() default "";

    boolean isMainScreenFragment() default true;

    boolean hasLeftDrawer() default false;

    boolean hasOptionsMenu() default false;
}