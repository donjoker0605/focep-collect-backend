package org.example.collectfocep.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    String action();
    String entityType() default "UNKNOWN";
    String description() default "";
    boolean includeRequestDetails() default true;
    boolean includeResponseDetails() default false;
}