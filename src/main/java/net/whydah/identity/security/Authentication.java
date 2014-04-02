package net.whydah.identity.security;

import net.whydah.identity.user.authentication.UserTokenID;

/**
 * Holds current authenticated user in a threadlocal.
 */
public final class Authentication {
    private static final ThreadLocal<UserTokenID> authenticatedUserID = new ThreadLocal<UserTokenID>();

    public static void setAuthenticatedUserID(UserTokenID user) {
        authenticatedUserID.set(user);
    }

    public static UserTokenID getAuthenticatedUserID() {
        return authenticatedUserID.get();
    }

    public static void clearAuthentication() {
        authenticatedUserID.remove();
    }

    private Authentication(){
    }
}
