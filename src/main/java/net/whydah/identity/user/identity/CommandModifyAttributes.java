package net.whydah.identity.user.identity;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

public class CommandModifyAttributes extends HystrixCommand<Void> {

    private static final Logger log = LoggerFactory.getLogger(CommandModifyAttributes.class);

    private final DirContext ctx;
    private final String userDN;
    private final ModificationItem[] mis;

    public CommandModifyAttributes(DirContext ctx, String userDN, ModificationItem[] mis) {
        super(HystrixCommandGroupKey.Factory.asKey("LDAP-calls"));
        this.ctx = ctx;
        this.userDN = userDN;
        this.mis = mis;
    }

    @Override
    protected Void run() throws Exception {
        try {
            ctx.modifyAttributes(userDN, mis);
        } catch (NamingException e) {
            throw new HystrixBadRequestException("", e);
        }
        return null;
    }
}
