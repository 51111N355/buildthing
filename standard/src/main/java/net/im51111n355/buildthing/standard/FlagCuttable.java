package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// isPresent: true - флаг=да удаляет. false - флаг=нет удаляет.
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlagCuttable {
    String value();

    @ReadMePlease({
        "This option will be removed in a future update! Replace with a @FlagCuttable to an opposite flag",
        "Эта опция будет удалена в будущем обновлении! Замените на @FlagCuttable с противоположным флагом"
    })
    @Deprecated boolean isPresent() default false;
}