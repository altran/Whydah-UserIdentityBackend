package net.whydah.identity.user.identity;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class CommandLdapSearch extends HystrixCommand<SearchResult> {

    private static final Logger log = LoggerFactory.getLogger(CommandLdapSearch.class);

    private final InitialDirContext context;
    private final String baseDN;
    private final String filter;
    private final SearchControls constraints;

    public CommandLdapSearch(InitialDirContext context, String baseDN, String filter, SearchControls constraints) {
        super(HystrixCommandGroupKey.Factory.asKey("LDAP-calls"));
        this.context = context;
        this.baseDN = baseDN;
        this.filter = filter;
        this.constraints = constraints;
    }

    @Override
    protected SearchResult run() throws Exception {
        NamingEnumeration results = context.search(baseDN, filter, constraints);
        if (!results.hasMore()) {
            return null;
        }
        SearchResult searchResult = (SearchResult) results.next();
        return searchResult;
    }

    @Override
    protected SearchResult getFallback() {
        log.debug("Ldap search command failed due to timeout. Returning null.");
        return null;
    }
}
