package dao;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import util.RegExpUtil;

import java.util.List;
import java.util.concurrent.Future;

public class PagedListI extends PagedListAbstractListI{   
	   
		public boolean isComplex = false;
		public PagedListI(boolean isComplex, Page page, String sortBy, String sql, Object... params){
			super(page,sortBy,sql,params);		
			this.isComplex =isComplex;
		}
		public PagedListI(boolean isComplex,Page page,String sortBy,String sql,Object[] countParams,Object... params){
			super(page,sortBy,sql,countParams,params);	
			this.isComplex =isComplex;
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
		
		@Override
		public int getTotalRowCount() {
			// TODO Auto-generated method stub
			if(RegExpUtil.groupBy(sql)>0||isComplex){//存在group by子句
				try{
					StringBuilder sb = new StringBuilder("select count(1) from ("+sql+") COUNT_TABLE");
					List<Object[]> result = baseDao.queryNative(sb.toString(), params);;
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
			}else{
				return super.getTotalRowCount();
			}
		}

		@Override
		public int getTotalRowCount(int sharding,int year) {
			List<Object[]> result = null;
			try{
				if(RegExpUtil.groupBy(sql)>0||isComplex){//存在group by子句
					StringBuilder sb = new StringBuilder("select count(1) from ("+sql+") COUNT_TABLE");
					result = baseDao.queryNativeSharding(sharding,year,sb.toString(),params);
				}else {
					result = getAllRecordSharding(sharding, year);
				}
				long rowCount = 0;
				if(result!=null && result.size()==1){
					rowCount = (long) result.get(0)[0];
				}
				count = (int) rowCount;
			}catch(Exception e){
				count = 0;
				e.printStackTrace();
			}
		return count;
	}
}
