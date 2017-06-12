package net.whydah.identity.user.search;

import java.util.ArrayList;
import java.util.List;

import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.identity.UIBUserIdentity;
import net.whydah.identity.user.identity.UIBUserIdentityRepresentation;
import net.whydah.identity.user.resource.UIBUserAggregateRepresentation;

public class PaginatedUIBUserIdentityDataList {
	
	public int pageNumber=0;
	public int pageSize=Paginator.pageSize;
	public int totalCount=0;
	public List<UIBUserIdentity> data = new ArrayList<UIBUserIdentity>();
	
	public PaginatedUIBUserIdentityDataList() {

    }

    public PaginatedUIBUserIdentityDataList(int pageNumber,  int totalCount,  List<UIBUserIdentity> data) {
        this.pageNumber = pageNumber;
        this.totalCount = totalCount;
        this.data = data;
    }


}
