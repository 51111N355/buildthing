package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Используйте `Inject.xxxValue` методы.
 */
@ReadMePlease({
    "[!READ ME!] Replace with Inject.xxxValue calls",

    "[!ПРОЧИТАЙ МЕНЯ!] Замените на Inject.xxxValue вызовы"
})
@Deprecated
public final class InjectValue {

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Integer {
        java.lang.String value();
    }

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Float {
        java.lang.String value();
    }

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Long {
        java.lang.String value();
    }

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Double {
        java.lang.String value();
    }

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface String {
        java.lang.String value();
    }

    @Deprecated
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Boolean {
        java.lang.String value();
    }
}
