package net.whydah.identity.security;

import net.whydah.identity.usertoken.UserToken;

/**
 * Holds current authenticated user in a threadlocal.
 */
public final class Authentication {
    private static final ThreadLocal<UserToken> authenticatedUser = new ThreadLocal<UserToken>();

    public static void setAuthenticatedUser(UserToken user) {
        authenticatedUser.set(user);
    }

    public static UserToken getAuthenticatedUser() {
        return authenticatedUser.get();
    }

    public static void clearAuthentication() {
        authenticatedUser.remove();
    }

    private Authentication(){
    }
}
