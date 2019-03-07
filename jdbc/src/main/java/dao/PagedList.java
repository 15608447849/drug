package dao;

import java.util.List;
import java.util.concurrent.Future;

public interface PagedList {
	 void loadRowCount();

	    Future<Integer> getFutureRowCount();

	    List<Object[]> getList();

	    int getTotalRowCount();

	    int getTotalPageCount();

	    int getPageIndex();

	    int getPageSize();

	    boolean hasNext();

	    boolean hasPrev();

	    String getDisplayXtoYofZ(String var1, String var2);
	
}
