package com.vladmihalcea.hpjp.spring.partition.util;

import com.vladmihalcea.hpjp.spring.partition.domain.User;

/**
 * @author Vlad Mihalcea
 */
public class UserContext {

    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void logIn(User user) {
        userHolder.set(user);
    }

    public static void logOut() {
        userHolder.remove();
    }

    public static User getCurrent() {
        return userHolder.get();
    }
}
