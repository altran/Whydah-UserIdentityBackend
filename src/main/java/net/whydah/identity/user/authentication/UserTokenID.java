package net.whydah.identity.user.authentication;


@Deprecated
public class UserTokenID {

    public String getId() {
        return id;
    }

    private final String id;

    public UserTokenID(String id) {
        this.id = id;
    }

}
