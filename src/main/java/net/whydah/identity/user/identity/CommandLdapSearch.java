package net.whydah.identity.user.identity;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class CommandLdapSearch extends HystrixCommand<SearchResult> {

    private static final Logger log = LoggerFactory.getLogger(CommandLdapSearch.class);

    private final DirContext context;
    private final String baseDN;
    private final String filter;
    private final SearchControls constraints;

    public CommandLdapSearch(DirContext context, String baseDN, String filter, SearchControls constraints) {
        super(HystrixCommandGroupKey.Factory.asKey("LDAP-calls"));
        this.context = context;
        this.baseDN = baseDN;
        this.filter = filter;
        this.constraints = constraints;
    }

    @Override
    protected SearchResult run() throws Exception {
        try {
            NamingEnumeration results = context.search(baseDN, filter, constraints);
            if (!results.hasMore()) {
                return null;
            }
            SearchResult searchResult = (SearchResult) results.next();
            return searchResult;
        } catch (NamingException e) {
            throw new HystrixBadRequestException("", e);
        }
    }
}
