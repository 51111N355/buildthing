package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlagCuttable {
    String value();
}