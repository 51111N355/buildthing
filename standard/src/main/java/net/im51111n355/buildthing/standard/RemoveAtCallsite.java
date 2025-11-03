package net.im51111n355.buildthing.standard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Дополнение к @FlagCuttable которое будет удалять вызовы этого метода, в не вырезанных местах, когда этот метод вырезан.
// Также отменяет проблемы с валидацией у таких методов. Нужно для создания альтернатив JustAGod'овских Invoke.server(...).
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoveAtCallsite {
}
