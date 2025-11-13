package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Используйте `Inject.flag` метод.
 */
@ReadMePlease({
    "[!READ ME!] Replace with Inject.flag",

    "[!ПРОЧИТАЙ МЕНЯ!] Замените на Inject.flag"
})
@Deprecated
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectFlag {
    String value();
}
