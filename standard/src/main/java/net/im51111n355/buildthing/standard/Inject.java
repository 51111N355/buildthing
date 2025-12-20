package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Всё что имеет поддержку Inject + runtime версии этих методов
// При сборке - всё кроме randXxx методов будут просчитываться НЕ так же как и тут, будут просчитываться в зависимости от конфига сборки.
// В случае randXxx методов - во время сборки будут вызывать точно такой же код как и тут, и заменять вызов на получившееся значение.

@ReadMePlease({
    "[!READ ME!] Calls to this class are replaced during BuildThing processing with values evaluated at build-time!",
    "See documentation at https://github.com/51111N355/buildthing for more info",

    "[!ПРОЧИТАЙ МЕНЯ!] Вызовы в этот класс заменяются во время BuildThing обработки со значениям определёнными во время сборки!",
    "Посмотрите документацию на https://github.com/51111N355/buildthing для получения дополнительной информации!!!"
})
public class Inject {
    public static List<Class<?>> classList(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static boolean flag(String flag) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static int intValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static float floatValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static long longValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static double doubleValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static String stringValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static boolean booleanValue(String key) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static int randInt(int minInclusive, int maxExclusive) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static float randFloat(float min, float max) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static long randLong(long minInclusive, long maxExclusive) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }

    public static double randDouble(double min, double max) {
        throw new AssertionError("This is not supposed to happen. Please see the documentation for \"processBeforeTask\".");
    }
}