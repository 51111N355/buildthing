package net.im51111n355.buildthing.standard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// isPresent: true - флаг=да удаляет. false - флаг=нет удаляет.
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlagCuttable {
    String value();
    boolean isPresent() default false;
}