package net.whydah.identity.user.search;

import java.util.ArrayList;
import java.util.List;

import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.identity.UIBUserIdentityRepresentation;
import net.whydah.identity.user.resource.UIBUserAggregateRepresentation;

public class PaginatedUIBUserAggregateDataList {
	
	public int pageNumber=0;
	public int pageSize=Paginator.pageSize;
	public int totalCount=0;
	public List<UIBUserAggregateRepresentation> data = new ArrayList<UIBUserAggregateRepresentation>();
	
	public PaginatedUIBUserAggregateDataList() {

    }

    public PaginatedUIBUserAggregateDataList(int pageNumber,  int totalCount,  List<UIBUserAggregateRepresentation> data) {
        this.pageNumber = pageNumber;
        this.totalCount = totalCount;
        this.data = data;
    }
    



}
