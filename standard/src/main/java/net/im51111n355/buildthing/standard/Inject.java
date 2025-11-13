package net.im51111n355.buildthing.standard;

import net.im51111n355.buildthing.standard.etc.ReadMePlease;

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
    public static boolean flag(String flag) {
        return System.getProperties().containsKey(flag);
    }

    public static int intValue(String key) {
        try {
            return Integer.parseInt(System.getenv(key));
        } catch (Exception e) {
            return 0;
        }
    }

    public static float floatValue(String key) {
        try {
            return Float.parseFloat(System.getenv(key));
        } catch (Exception e) {
            return 0F;
        }
    }

    public static long longValue(String key) {
        try {
            return Long.parseLong(System.getenv(key));
        } catch (Exception e) {
            return 0L;
        }
    }

    public static double doubleValue(String key) {
        try {
            return Double.parseDouble(System.getenv(key));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static String stringValue(String key) {
        try {
            return System.getenv(key);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean booleanValue(String key) {
        try {
            return Boolean.parseBoolean(System.getenv(key));
        } catch (Exception e) {
            return false;
        }
    }

    public static int randInt(int minInclusive, int maxExclusive) {
        return ThreadLocalRandom.current()
            .nextInt(minInclusive, maxExclusive);
    }

    public static float randFloat(float min, float max) {
        float d = max - min;
        float r = ThreadLocalRandom.current().nextFloat();

        return min + d * r;
    }

    public static long randLong(long minInclusive, long maxExclusive) {
        return ThreadLocalRandom.current()
            .nextLong(minInclusive, maxExclusive);
    }

    public static double randDouble(double min, double max) {
        double d = max - min;
        double r = ThreadLocalRandom.current().nextFloat();

        return min + d * r;
    }
}