package com.vladmihalcea.book.hpjp.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <code>StackTraceUtils</code> - Stack Trace utilities holder.
 *
 * @author Vlad Mihalcea
 */
public final class StackTraceUtils {

    private StackTraceUtils() {
        throw new UnsupportedOperationException("StackTraceUtils is not instantiable!");
    }

    /**
     * Filter the stack trace based on the provide package name prefix
     *
     * @param packageNamePrefix package name to match the {@link StackTraceElement} to be returned
     * @return the {@link StackTraceElement} objects matching the provided package name
     */
    public static List<StackTraceElement> filter(String packageNamePrefix) {
        return Arrays.stream(Thread.currentThread().getStackTrace()).filter(
            stackTraceElement -> {
                String packageName = ReflectionUtils.getClassPackageName(stackTraceElement.getClassName());

                return packageName != null && packageName.startsWith(packageNamePrefix);
            }
        ).collect(Collectors.toList());
    }


}
