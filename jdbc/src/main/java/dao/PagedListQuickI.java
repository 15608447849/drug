package dao;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;

import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于大数据量分页查询,不支持跳页查询,只能查询上一页 下一页 ,一次查询10页返回到前台
 * 只支持select from where group by order by ;类型查询,不支持union all 查询,不支持无条件查询,不支持排序
 *
 */
public class PagedListQuickI extends PagedListAbstractListI{
	public PagedListQuickI(Page page, String sortBy, String sql, Object... params){
		super(page,sortBy,sql,params);		
	}
	
	@SuppressWarnings("unused")
    private int setPageSql(String sql){      
        String regex = "(\\s)?((s|S)(E|e)(L|l)(E|e)(C|c)(T|t))(\\s)[\\s\\S]*(\\s)((f|F)(R|r)(o|O)(m|M))(\\s)[\\s\\S]*((w|W)(H|h)(e|E)(R|r)(E|e))";
        Pattern pt=Pattern.compile(regex);  
        Matcher mt=pt.matcher(sql);       
        int end = -1;
        while(mt.find()){
        	end = mt.end();
        }
       return end;
    } 
	@Override
	public void loadRowCount() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Future<Integer> getFutureRowCount() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object[]> getList() {
		// TODO Auto-generated method stub		
//		int offset = setPageSql(sql);
//		if(offset<0) return null;
//		StringBuilder sb = new StringBuilder(sql);		
//		int start = page.maxOid;
//		if(start<0){//上一页
//			start = page.minOid;
//			sb.insert(offset, " oid<"+start+" and ");
//		}else{//下一页
//			sb.insert(offset, " oid>"+start+" and ");
//		}
//		int end = page.pageSize*10;	
//		String nativeSQL = sb.toString()+" limit ?";
//		Object[] paramsPage = new Object[params.length+1];
//		System.arraycopy(params, 0, paramsPage, 0, params.length);				
//		paramsPage[paramsPage.length-1] = end;
//		return baseDao.queryNative(nativeSQL, paramsPage);	
		return null;
	}

	@Override
	public int getTotalRowCount(int sharding, int year) {
		return 0;
	}


	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPrev() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDisplayXtoYofZ(String var1, String var2) {
		// TODO Auto-generated method stub
		return null;
	}
}
