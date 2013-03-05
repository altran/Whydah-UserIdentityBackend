package no.freecode.iam.service.domain;

import java.util.ArrayList;
import java.util.List;

public class WhydahUser {
    private WhydahUserIdentity identity = null;
    private List<UserPropertyAndRole> propsandroles = new ArrayList<UserPropertyAndRole>();

    public WhydahUser(WhydahUserIdentity identity, List<UserPropertyAndRole> propsandroles) {
        this.identity = identity;
        this.propsandroles = propsandroles;
    }

    public WhydahUser() {
		// TODO Auto-generated constructor stub
	}

   
	public WhydahUserIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(WhydahUserIdentity identity) {
        this.identity = identity;
    }

    public List<UserPropertyAndRole> getPropsAndRoles() {
        return propsandroles;
    }

    public void addPropsAndRoles(UserPropertyAndRole propsandrole) {
        this.propsandroles.add(propsandrole);
    }

    public void setPropsAndRoles(List<UserPropertyAndRole> propsandroles) {
        this.propsandroles = propsandroles;
    }
}
