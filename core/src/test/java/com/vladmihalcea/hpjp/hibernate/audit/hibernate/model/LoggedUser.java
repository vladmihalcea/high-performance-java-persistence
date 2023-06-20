package com.vladmihalcea.hpjp.hibernate.audit.hibernate.model;

/**
 * @author Vlad Mihalcea
 */
public class LoggedUser {

    private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

    public static void logIn(String user) {
        userHolder.set(user);
    }

    public static void logOut() {
        userHolder.remove();
    }

    public static String get() {
        return userHolder.get();
    }
}
