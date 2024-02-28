package com.vladmihalcea.hpjp.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Vlad Mihalcea
 */
public class RandomUtils {

    public static final ThreadLocalRandom GENERATOR = ThreadLocalRandom.current();

    private static final String[] STARTING = new String[] {
        "Lorem", "Aenean", "Cum", "Donec", "Nulla", "Nullam", "Vivamus", "Aliquam", "Phasellus", "Quisque", "Aenean", "Etiam", "Curabitur", "Nam", "Maecenas",
    };

    private static final String[] SIMPLE_1 = new String[] {
        "ipsum", "dolor", "sit", "consectetuer", "adipiscing", "commodo", "ligula", "eget", "sociis", "natoque", "penatibus", "et", "magnis", "dis", "parturient", "nascetur", "ridiculus", "quam", "ultricies", "pellentesque", "pretium", "consequat", "massa", "quis", "pede", "fringilla", "aliquet", "vulputate", "enim", "rhoncus", "fringilla", "mauris", "sit", "amet", "sodales", "sagittis", "leo", "eget", "bibendum", "augue", "velit", "cursus", "nunc"
    };

    private static final String[] SIMPLE_2 = new String[] {
        "amet", "felis", "nec", "eu", "quis", "justo", "vel", "nec", "eget", "a", "venenatis", "vitae", "ligula", "eu", "vitae", "ac", "ante", "in", "quis", "a", "tempus", "libero", "vel", "pulvinar", "id", "consequat", "sodales"
    };

    private static final String[] ENDINGS = new String[] {
        "elit.", "dolor.", "massa.", "mus.", "sem.", "enim.", "arcu.", "justo.", "pretium.", "tincidunt.", "dapibus.", "nisi.", "tellus.", "enim.", "tellus.", "laoreet.", "rutrum.", "augue.","nisi.", "dui.", "rhoncus.", "ipsum.", "lorem.", "tempus.", "ante.", "tincidunt.", "leo.", "magna."
    };

    public static String randomTitle() {
        return String.format(
            "%s %s %s %s %s %s %s %s %s %s",
            STARTING[GENERATOR.nextInt(STARTING.length)],
            SIMPLE_1[GENERATOR.nextInt(SIMPLE_1.length)],
            SIMPLE_1[GENERATOR.nextInt(SIMPLE_1.length)],
            SIMPLE_2[GENERATOR.nextInt(SIMPLE_2.length)],
            SIMPLE_1[GENERATOR.nextInt(SIMPLE_1.length)],
            SIMPLE_2[GENERATOR.nextInt(SIMPLE_2.length)],
            SIMPLE_1[GENERATOR.nextInt(SIMPLE_1.length)],
            SIMPLE_1[GENERATOR.nextInt(SIMPLE_1.length)],
            SIMPLE_2[GENERATOR.nextInt(SIMPLE_2.length)],
            ENDINGS[GENERATOR.nextInt(ENDINGS.length)]
        );
    }
}
