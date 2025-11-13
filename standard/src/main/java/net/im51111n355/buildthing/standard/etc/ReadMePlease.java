package net.im51111n355.buildthing.standard.etc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Чтобы писать текст который будет видно при открытии класса, без надобности мне разбираться с sources/javadoc jar'ом
@ReadMePlease({
    "[!READ ME!] This annotation is used to leave messages in classes for decomiler views, as comments",

    "[!ПРОЧИТАЙ МЕНЯ!] Эта аннотация используется чтобы оставлять сообщения в классах для просмотра в декомпиляторах, как коментарии"
})
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadMePlease {
    String[] value();
}
