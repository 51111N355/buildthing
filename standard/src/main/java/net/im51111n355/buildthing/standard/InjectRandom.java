package net.im51111n355.buildthing.standard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class InjectRandom {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Integer {
        int minInclusive() default java.lang.Integer.MIN_VALUE;
        int maxExclusive() default java.lang.Integer.MAX_VALUE;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Float {
        float min() default java.lang.Float.MIN_VALUE;
        float max() default java.lang.Float.MAX_VALUE;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Long {
        long minInclusive() default java.lang.Long.MIN_VALUE;
        long maxExclusive() default java.lang.Long.MAX_VALUE;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Double {
        double min() default java.lang.Double.MIN_VALUE;
        double max() default java.lang.Double.MAX_VALUE;
    }
}
