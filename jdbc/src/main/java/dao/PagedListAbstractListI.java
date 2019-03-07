package dao;

import java.util.List;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import db.util.MathUtil;
import db.util.RegExpUtil;
import util.ParameterUtil;

public abstract class PagedListAbstractListI implements PagedList {
   public Page page ;
    public String sql;
    public String sortBy;
    public Object[] params;
    public Object[] countParams;
    public BaseDAO baseDao;
    public int count = -1;
    public PagedListAbstractListI(Page page,String sortBy,String sql,Object[] params){    	
		this.page = page;
		this.sql = sql;
		this.params = params;
		this.sortBy = sortBy;
		countParams= null;
		baseDao = BaseDAO.getBaseDAO();	;
	}
    
    public PagedListAbstractListI(Page page,String sortBy,String sql,Object[] countParams,Object[] params){   
    	this.page = page;
		this.sql = sql;
		this.params = params;
		this.sortBy = sortBy;
		this.countParams = countParams;
		baseDao = BaseDAO.getBaseDAO();			
	}
    
	protected List<Object[]> getAllRecord(){
		int offset = RegExpUtil.sf(sql);
        if(offset<0) return null;
        StringBuilder sb = new StringBuilder(sql);
        sb.replace(0, offset,"select count(1) from ");
        if(countParams==null)
        	return baseDao.queryNative(sb.toString(), params);	
        else
        	return baseDao.queryNative(sb.toString(), countParams);
	}
	

	@Override
	public int getTotalRowCount() {
		// TODO Auto-generated method stub
		try{
		List<Object[]> result = getAllRecord();
		long rowCount = 0;
		if(result!=null&&result.size()==1){
			rowCount = (long) result.get(0)[0];
		}
		count = (int) rowCount;
		}catch(Exception e){
			count = 0;
			e.printStackTrace();
		}
		return count;
	}

	@Override
	public int getTotalPageCount() {
	    if(page==null) return 1;
	    
	    if(count==-1){
	        count = getTotalRowCount();
	    }
		
		if(page.pageSize<=0) return -1; 
		
		return MathUtil.ceilDiv(count, page.pageSize);
	}

	@Override
	public int getPageIndex() {
		// TODO Auto-generated method stub
		if(page==null) return -1;
		return page.pageIndex;
	}

	@Override
	public int getPageSize() {
		// TODO Auto-generated method stu
		if(page==null) return -1;
		return page.pageSize;
	}
	
	@Override
	public List<Object[]> getList() {
		 if(!ParameterUtil.isNull(sortBy)){
			 sql = sql+" order by "+sortBy;
		}
		if(page==null || page.pageIndex<=0||page.pageSize<=0){
			System.out.println("===========bufenye====");
			return baseDao.queryNative(sql, params);		
		}else{			
			System.out.println("===========fenye====");
			int start = (page.pageIndex-1)*page.pageSize;
			int end = page.pageSize;			
			String nativeSQL = sql+" limit ?,?";
			Object[] paramsPage = new Object[params.length+2];
			System.arraycopy(params, 0, paramsPage, 0, params.length);				
			paramsPage[paramsPage.length-2] = start;
			paramsPage[paramsPage.length-1] = end;
			return baseDao.queryNative(nativeSQL, paramsPage);							
		}					
	}

	

}
