package net.whydah.identity.user.identity;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class CommandLdapDestroySubcontext extends HystrixCommand<Void> {

    private static final Logger log = LoggerFactory.getLogger(CommandLdapDestroySubcontext.class);

    private final DirContext ctx;
    private final String userDN;

    public CommandLdapDestroySubcontext(DirContext ctx, String userDN) {
        super(HystrixCommandGroupKey.Factory.asKey("LDAP-calls"));
        this.ctx = ctx;
        this.userDN = userDN;
    }

    @Override
    protected Void run() throws Exception {
        try {
            ctx.destroySubcontext(userDN);
        } catch (NamingException e) {
            throw new HystrixBadRequestException("", e);
        }
        return null;
    }
}
