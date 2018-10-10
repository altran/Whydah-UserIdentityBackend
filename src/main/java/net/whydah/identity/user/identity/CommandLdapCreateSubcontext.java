package net.whydah.identity.user.identity;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;

public class CommandLdapCreateSubcontext extends HystrixCommand<DirContext> {

    private static final Logger log = LoggerFactory.getLogger(CommandLdapCreateSubcontext.class);

    private final DirContext ctx;
    private final String userdn;
    private final Attributes attributes;

    public CommandLdapCreateSubcontext(DirContext ctx, String userdn, Attributes attributes) {
        super(HystrixCommandGroupKey.Factory.asKey("LDAP-calls"), 3000);
        this.ctx = ctx;
        this.userdn = userdn;
        this.attributes = attributes;
    }

    @Override
    protected DirContext run() throws Exception {
        try {
            DirContext subcontext = ctx.createSubcontext(userdn, attributes);
            return subcontext;
        } catch (NoPermissionException | NameAlreadyBoundException | InvalidAttributeValueException e) {
            throw new HystrixBadRequestException("", e);
        } finally {
        	ctx.close();
        }
    }
}
