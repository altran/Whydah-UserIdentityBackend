package net.whydah.identity.security;

import net.whydah.identity.application.ApplicationDao;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 22.11.15.
 */
@Service
public class AuthenticationService {
    private static final Logger log = getLogger(AuthenticationService.class);

    private final ApplicationDao applicationDao;

    @Autowired
    public AuthenticationService(ApplicationDao applicationDao) {
        this.applicationDao = applicationDao;
    }

    public boolean isAuthenticated(ApplicationCredential applicationCredential) {
        //TODO https://github.com/Cantara/Whydah-UserAdminService/issues/38 and
        //https://github.com/Cantara/Whydah-UserIdentityBackend/issues/42
        return true;
    }
}
