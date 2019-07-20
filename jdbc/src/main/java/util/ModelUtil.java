package util;

import cn.hy.otms.rpcproxy.comm.cstruct.BoolMessage;
import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import dao.BaseDAO;
import global.QCParam;
import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 查询工具类
 * @author ywkj
 */
public class ModelUtil {
	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	/**
	 * 操作类别 EQ 等于操作 LIKE 字符串模糊匹配 HASCODE 资源码特殊操作 是否有某个码值 BITOPERA 位操作
	 * BIT_AND_ZERO name&?=0 BIT_OR_ZERO name|?=0 LT < GT > LE <= GE >=
	 * 
	 * @author ywkj
	 *
	 */
	public static enum OPERATOR {
		EQ, LIKE,NOTLIKE, HASCODE, BITOPERA, BIT_AND_ZERO, BIT_OR_ZERO, LT, GT, LE, GE, SYSCODE, ORLIKE
	}


	/**
	 * 替换前后的属性 rField 位空或"" 表示删除对应属性
	 * 
	 * @author ywkj
	 *
	 */
	public static class ReplaceField {
		public String[] field;
		public String[] rField;

		public ReplaceField(String[] field, String[] rField) {
			if (field.length == rField.length) {
				this.field = field;
				this.rField = rField;
			}
		}
	}

	/**
	 * 动态生成sql
	 * 
	 * @param list
	 * @param sql
	 * @param name
	 * @param model
	 * @param type
	 */
	@SuppressWarnings("incomplete-switch")
	public static void dynamicSql(List<Object> list, StringBuilder sql,
			String name, Object model, OPERATOR type) {
		if (!isNull(model)) {
			// SValue sValue = new SValue(type, model);
			list.add(model);
			switch (type) {
			case EQ: {
				sql.append(" and " + name + " = ? ");
				break;
			}
			case LIKE: {
				sql.append(" and " + name + " like ? ");
				list.set(list.size() - 1, "%" + model + "%");
				break;
			}
			case NOTLIKE: {
				sql.append(" and " + name + " not like ? ");
				list.set(list.size() - 1, "%" + model + "%");
				break;
			}
			case HASCODE: {
				sql.append(" and " + name + "&?>0 ");
				break;
			}
			case BITOPERA: {
				list.add(model);
				sql.append(" and ?&" + name + "=? ");
				break;
			}
			case BIT_AND_ZERO: {
				sql.append(" and " + name + "&?=0 ");
				break;
			}
			case BIT_OR_ZERO: {
				sql.append(" and " + name + "|?=0 ");
				break;
			}
			case LT: {
				sql.append(" and " + name + "<? ");
				break;
			}
			case GT: {
				sql.append(" and " + name + ">? ");
				break;
			}
			case LE: {
				sql.append(" and " + name + "<=? ");
				break;
			}
			case GE: {
				sql.append(" and " + name + ">=? ");
				break;
			}
			case ORLIKE: {
				sql.append(" or " + name + " like ? ");
				list.set(list.size() - 1, "%" + model + "%");
				break;
			}
			}
		}
	}

	/**
	 * 自动生成SQL语句且可自由搭配条件运算符和括号的控制，并将结果传入自SQL。
	 * 
	 * @param $list
	 *            存放对应条件的值的list。
	 * @param $sql
	 *            需要追加的SQL语句。
	 * @param $name
	 *            条件。
	 * @param $model
	 *            该条件所对应的值。
	 * @param $type
	 *            条件与值之间的类型。
	 * @param $link
	 *            该SQL条件的条件运算符。可能的值有：and和or。
	 * @param $begin
	 *            可选参数，不填无意义。<strong>设置为true表示开启一个括号。</strong> <br>
	 *            (即表示增加一个 '(' 但是需要注意：<strong>搭配对应多个的false</strong>)
	 */

	public static void dynamicSql(List<Object> $list, StringBuilder $sql,
			final String $name, final Object $model, final OPERATOR $type,
			final String $link, final boolean... $begin) {
		if (!isNull($model)) {

			$list.add($model);

			// 根据条件获取头括号和尾括号的值。
            String front  = $begin.length  > 0 && $begin[0] ? "(" : " ";
            String behind = $begin.length == 0 || $begin[0] ? " " : ")";


			// 组装条件运算符、头括号和条件名。
			$sql.append(" " + $link + " " + front
					+ ($type.equals(OPERATOR.BITOPERA) ? "?&" : "") + $name);

			// 组装类型
			switch ($type) {
			case EQ: {
				$sql.append(" = ? ");
				break;
			}
			case LIKE: {
				$sql.append(" like ? ");
				$list.set($list.size() - 1, "%" + $model + "%");
				break;
			}
			case HASCODE: {
				$sql.append("&?>0 ");
				break;
			}
			case BITOPERA: {
				$list.add($model);
				$sql.append("=? ");
				break;
			}
			case BIT_AND_ZERO: {
				$sql.append("&?=0 ");
				break;
			}
			case BIT_OR_ZERO: {
				$sql.append("|?=0 ");
				break;
			}
			case LT: {
				$sql.append("<? ");
				break;
			}
			case GT: {
				$sql.append(">? ");
				break;
			}
			case LE: {
				$sql.append("<=? ");
				break;
			}
			case GE: {
				$sql.append(">=? ");
				break;
			}
			default:
				break;
			}

			// 组装尾括号。
			$sql.append(behind);

		}
	}

	/**
	 * 判断属性是否是查询条件
	 * 
	 * @param model
	 * @return
	 */
	private static boolean isNull(Object model) {
		if (model == null)
			return true;
		
		if (model instanceof Long) {
			Long l = (Long) model;
			if (0L == l)
				return true;
		}
		if (model instanceof Integer) {
			Integer l = (Integer) model;
			if (l == 0)
				return true;
		}
		if (model instanceof Short) {
			Short l = (Short) model;
			if (l == 0)
				return true;
		}
		if (model instanceof Float) {
			Float l = (Float) model;
			if (l == 0.0F)
				return true;
		}
		if (model instanceof Double) {
			Double l = (Double) model;
			if (l == 0.0d)
				return true;
		}
		if (model instanceof Byte) {
			Byte l = (Byte) model;
			if (l == 0)
				return true;
		}
		if (model instanceof String) {
			String l = (String) model;
			if (l.isEmpty())
				return true;
		}
		return false;
	}

	/**
	 * 配置查询参数
	 * 
	 * @param list
	 * @return
	 */
	public static Object[] getParmars(List<Object> list) {
		Object[] params = new Object[list.size()];
		for (int i = 0; i < list.size(); i++) {
			params[i] = list.get(i);
		}
		return params;
	}

	/**
	 * 替换原始的查询属性
	 * 
	 * @param list
	 *            返回的查询参数
	 * @param clazz
	 *            类
	 * @param sql
	 *            返回的sql语句
	 * @param rfs
	 *            需要进行替换的属性
	 */
	@SuppressWarnings("rawtypes")
	public static void replaceFields(List<String> list, Class clazz,
			StringBuilder sql, ReplaceField rfs) {
		getAllFields(list, clazz, sql, rfs.field);
		for (int i = 0; i < rfs.rField.length; i++) {
			String r = rfs.rField[i];
			String f = rfs.field[i];
			if (r != null && !"".equals(r)) {
				sql.append(",").append(r);
				list.add(f);
			}
		}
	}

	/**
	 * 获取指定类的所有属性放入list中,生成对应sql, 可选择移除不需要查询的列
	 * 
	 * @param list
	 *            属性结果集
	 * @param clazz
	 *            类
	 * @param sql
	 *            生成的sql
	 * @param nFields
	 *            需要移除的属性
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static void getAllFields(List<String> list, Class clazz,
			StringBuilder sql, String... nFields) {
		Field[] fields = clazz.getDeclaredFields();
		if (fields == null || fields.length <= 3 || list == null || sql == null)
			return;
		if (nFields == null)
			nFields = new String[0];
		StringBuilder nFieldStrB = new StringBuilder();
		for (String field : nFields) {
			nFieldStrB.append(field).append(",");
		}
		String nFieldsStr = nFieldStrB.toString();
		sql.append("select ");
		for (int index = 0; index < fields.length - 3; index++) {
			String field = fields[index].getName();
			if (!nFieldsStr.contains(field)) {
				list.add(field);
				sql.append(field).append(",");
			}
		}
		if (sql.length() >= 1)
			sql = sql.deleteCharAt(sql.length() - 1);
	}

	public static void getFieldNames(List<String> names,
			Class<? extends Object> c, String... nFields) {
		Field[] fields = getFields(c);
		if (fields == null || fields.length <= 3)
			return;
		if (nFields == null)
			nFields = new String[0];
		for (int index = 0; index < fields.length - 3; index++) {
			String field = fields[index].getName();
			if (!isContainStr(field, nFields)) {
				names.add(field);
			}
		}
	}

	private static boolean isContainStr(String str, String... strs) {
		for (String s : strs) {
			if (s.equals(str))
				return true;
		}
		return false;
	}

	private static Map<Class<? extends Object>, Field[]> fieldsMap = new HashMap<Class<? extends Object>, Field[]>();

	public static Field[] getFields(Class<? extends Object> c) {
		Field[] fields = fieldsMap.get(c);
		if (fields == null) {
			fields = c.getDeclaredFields();
			fieldsMap.put(c, fields);
		}
		return fields;
	}

	/**
	 * 获取对象所有属性和对应的值
	 * 
	 * @param names
	 * @param values
	 * @param o
	 * @param nFields
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static void getFieldNameAndValues(List<String> names,
			List<Object> values, Object o, String... nFields) {
		Field[] fields = getFields(o.getClass());
		if (fields == null || fields.length <= 3)
			return;
		if (nFields == null)
			nFields = new String[0];
		for (int index = 0; index < fields.length - 3; index++) {
			String field = fields[index].getName();
			if (!isContainStr(field, nFields)) {
				names.add(field);
				try {
					values.add(fields[index].get(o));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public static <T> Object[] getValuesByNames(String[] names, T t) {
		List<Object> values = new LinkedList<Object>();
		Field[] fields = getFields(t.getClass());
		for (String name : names) {
			for (int index = 0; index < fields.length - 3; index++) {
				String field = fields[index].getName();
				if (name.equals(field)) {
					try {
						values.add(fields[index].get(t));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return values.toArray();
	}

	/**
	 * 得到新增的sql和要插入的值
	 * 
	 * @param tableInt
	 * @param t
	 * @param sql
	 * @param nFields
	 * @return
	 * @throws Exception
	 */
	public static <T> Object[] getInsertSQL(int tableInt, T t,
			StringBuilder sql, String... nFields) {
		sql.append("insert into {{?" + tableInt + "}} (");
		List<String> names = new LinkedList<String>();
		List<Object> values = new LinkedList<Object>();
		// 获取字段名称和值
		getFieldNameAndValues(names, values, t, nFields);
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (String name : names) {
			sql.append("`"+name+"`");
			sql.append(",");
			sb.append("?,");
		}
		sb.setCharAt(sb.length() - 1, ')');
		sql.setCharAt(sql.length() - 1, ')');
		sql.append("values" + sb);
		return values.toArray();
	}

//	public static <T> BoolMessage insert(T t,String idName, int tableInt, String... nFields) {
//		
//	}
	
	/**
	 * 将对象insert到数据库中,字段名需和数据库的保持一致
	 * 
	 * @param t
	 * @param tableInt
	 * @param nFields
	 *            忽略的字段(不传默认忽略oid)
	 * @return
	 */
	public static <T> BoolMessage insert(T t, int tableInt, String... nFields) {
		BoolMessage r = new BoolMessage();
		StringBuilder sql = new StringBuilder();
		Object[] params = null;
		// 默认过滤id
		if (nFields == null || !(nFields.length > 0)) {
			nFields = new String[] { "oid" };
		}
		try {
			params = getInsertSQL(tableInt, t, sql, nFields);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int result = BaseDAO.getBaseDAO().updateNative(sql.toString(), params);
		r.flag = result > 0;
		return r;
	}

	/**
	 * 新增记录时校验是否重名
	 * @param fieldName
	 * @param value
	 * @param tableInt
	 * @return
	 */
	public static BoolMessage checkName(String fieldName, String value, int tableInt) {
		BoolMessage bool = new BoolMessage();
		String sql = "select " + fieldName + " from {{?" + tableInt
				+ "}} where cstatus&1=0 and " + fieldName + "=?";
		List<Object[]> results = BaseDAO.getBaseDAO().queryNative(sql, value);
		if (results != null && !results.isEmpty() && results.get(0).length > 0) {
			// 存在记录
			bool.flag = false;
			bool.fmessage = "名称已存在!";
			return bool;
		}
		bool.flag = true;
		return bool;
	}
	/**
	 * 新增记录时校验是否重名，并返回重名的id
	 * @param fieldName
	 * @param value
	 * @param tableInt
	 * @param reFieldName 重名数据的某属性的值
	 * @return
	 */
	public static BoolMessage checkName(String fieldName, String value, int tableInt,String reFieldName) {
		BoolMessage bool = new BoolMessage();
		String sql = "select " + reFieldName + " from {{?" + tableInt
				+ "}} where cstatus&1=0 and " + fieldName + "=?";
		List<Object[]> results = BaseDAO.getBaseDAO().queryNative(sql, value);
		if (results != null && !results.isEmpty() && results.get(0).length > 0) {
			// 存在记录
			bool.flag = false;
			bool.fmessage = "名称已存在!";
			bool.smessage=String.valueOf(results.get(0)[0]);//重名记录的id
			return bool;
		}
		bool.flag = true;
		return bool;
	}
	
	/**
	 * 修改记录时校验是否重名
	 * @param idName
	 * @param idValue
	 * @param fieldName
	 * @param value
	 * @param tableInt
	 * @return
	 */
	public static BoolMessage checkName(String idName,String idValue,String fieldName, String value, int tableInt) {
		BoolMessage bool = new BoolMessage();
		String sql = "select " + fieldName + " from {{?" + tableInt
				+ "}} where cstatus&1=0 and " + fieldName + "=?"+" and "+idName+ "<>?";
		List<Object[]> results = BaseDAO.getBaseDAO().queryNative(sql, value,idValue);
		if (results != null && !results.isEmpty() && results.get(0).length > 0) {
			// 存在记录
			bool.flag = false;
			bool.fmessage = "名称已存在!";
			return bool;
		}
		bool.flag = true;
		return bool;
	}
	
	public static BoolMessage checkName(String idValue,String fieldName, String value, int tableInt) {
		return checkName("oid",idValue, fieldName, value, tableInt);
	}

	/**
	 * 将对象update到数据库中,字段名需和数据库的保持一致
	 * 
	 * @param t
	 * @param tableInt
	 * @param filter
	 *            条件
	 * @param nFields
	 *            忽略的字段
	 * @return
	 */
	public static <T> BoolMessage update(T t, int tableInt, String[] filter,
			String... nFields) {
		BoolMessage r = new BoolMessage();
		StringBuilder sql = new StringBuilder();
		Object[] params = null;
		try {
			params = getUpdateSQL(tableInt, t, sql, filter, nFields);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int result = BaseDAO.getBaseDAO().updateNative(sql.toString(), params);
		r.flag = result > 0;
		return r;
	}

	/**
	 * 将对象update到数据库中,字段名需和数据库的保持一致(默认条件为oid)
	 * 
	 * @param t
	 * @param tableInt
	 * @param nFields
	 *            忽略的字段
	 * @return
	 */
	public static <T> BoolMessage update(T t, int tableInt, String... nFields) {
		return update(t, tableInt, new String[] { "oid" }, nFields);
	}
	public static <T> Object[] getUpdateSQL(int tableInt, T t,
			StringBuilder sql, String... nFields) {
		return getUpdateSQL(tableInt, t, sql, new String[] { "oid" }, nFields);
	}
	public static <T> Object[] getUpdateSQL(int tableInt, T t,
			StringBuilder sql, String[] filter, String... nFields) {
		sql.append("update {{?" + tableInt + "}} set ");
		List<String> names = new LinkedList<String>();
		List<Object> values = new LinkedList<Object>();
		// 获取字段名称和值
		getFieldNameAndValues(names, values, t, nFields);
		for (String name : names) {
			sql.append("`"+name+"`" + "=?,");
		}
		sql.setCharAt(sql.length() - 1, ' ');
		if (filter != null && filter.length > 0) {
			sql.append("where ");
			for (String filteName : filter) {
				sql.append(filteName + "=?,");
			}
		}
		sql.setCharAt(sql.length() - 1, ' ');
		for (Object o : getValuesByNames(filter, t)) {
			values.add(o);
		}
		return values.toArray();
	}

	/**
	 * 生成删除语句
	 * 
	 * @param t
	 * @param field
	 *            删除的条件(数据库的主键)
	 * @param values
	 *            要删除的主键值数组
	 */
	public static void getDelSql(int tableInt, StringBuilder sql, String field,
			long[] ids) {
		sql.append("update {{?" + tableInt
				+ "}} set cstatus=cstatus|1 where cstatus&1=0 and ");
		sql.append(field + " in( ");
		for (long id : ids) {
			sql.append(id + ",");
		}
		sql.setCharAt(sql.length() - 1, ')');
	}

	
	/**
     * 生成删除语句
     * 
     * @param t
     * @param field
     *            删除的条件(数据库的主键)
     * @param values
     *            要删除的主键值数组
     */
    public static void getDelSql(int tableInt, StringBuilder sql, String field,
            int[] ids) {
        sql.append("update {{?" + tableInt
                + "}} set cstatus=cstatus|1 where cstatus&1=0 and ");
        sql.append(field + " in( ");
        for (long id : ids) {
            sql.append(id + ",");
        }
        sql.setCharAt(sql.length() - 1, ')');
    }
	
	/**
	 * 生成删除语句
	 * 
	 * @param
	 * @param field
	 *            删除的条件(数据库的主键)
	 * @param
	 *            要删除的主键值数组
	 */
	public static void getDelSql(int tableInt, StringBuilder sql, String field,
			String[] ids) {
		sql.append("update {{?" + tableInt
				+ "}} set cstatus=cstatus+1 where cstatus&1=0 and binary ");
		sql.append(field + " in( ");
		for (String id : ids) {
			sql.append("'" + id + "'" + ",");
		}
		sql.setCharAt(sql.length() - 1, ')');
	}

	
	/**
     * 生成删除语句(使用delete)
     * 
     * @param t
     * @param field
     *            删除的条件(数据库的主键)
     * @param values
     *            要删除的主键值数组
     */
    public static void getAbsDelSql(int tableInt, StringBuilder sql, String field,
            long[] ids) {
        sql.append("DELETE FROM {{?" + tableInt
                + "}} WHERE cstatus&1=0 and ");
        sql.append(field + " in( ");
        for (long id : ids) {
            sql.append(id + ",");
        }
        sql.setCharAt(sql.length() - 1, ')');
    }

    
    /**
     * 生成删除语句(使用delete)
     * 
     * @param t
     * @param field
     *            删除的条件(数据库的主键)
     * @param values
     *            要删除的主键值数组
     */
    public static void getAbsDelSql(int tableInt, StringBuilder sql, String field,
            int[] ids) {
        sql.append("DELETE FROM {{?" + tableInt
                + "}} WHERE cstatus&1=0 and ");
        sql.append(field + " in( ");
        for (long id : ids) {
            sql.append(id + ",");
        }
        sql.setCharAt(sql.length() - 1, ')');
    }
    
    /**
     * 生成删除语句(使用delete)
     * 
     * @param t
     * @param field
     *            删除的条件(数据库的主键)
     * @param values
     *            要删除的主键值数组
     */
    public static void getAbsDelSql(int tableInt, StringBuilder sql, String field,
            String[] ids) {
        sql.append("DELETE FROM {{?" + tableInt
                + "}} WHERE cstatus&1=0 and binary ");
        sql.append(field + " in( ");
        for (String id : ids) {
            sql.append("'" + id + "'" + ",");
        }
        sql.setCharAt(sql.length() - 1, ')');
    }
	
	
	/**
	 * 删除
	 * 
	 * @param tableInt
	 * @param field
	 * @param ids
	 * @return
	 */
	public static BoolMessage del(int tableInt, String field, long[] ids) {
		BoolMessage r = new BoolMessage();
		if (ParameterUtil.isNull(field)) {
			r.flag = false;
			r.fmessage = "主键字段名不能为空!";
		}
		StringBuilder sql = new StringBuilder();
		getDelSql(tableInt, sql, field, ids);
		int result = BaseDAO.getBaseDAO().updateNative(sql.toString());
		r.flag = result > 0;
		return r;
	}

	/**
	 * 删除
	 * 
	 * @param tableInt
	 * @param field
	 * @param ids
	 * @return
	 */
	public static BoolMessage del(int tableInt, String field, String[] ids) {
		BoolMessage r = new BoolMessage();
		if (field != null && !"".equals(field)) {
			r.flag = false;
			r.fmessage = "主键字段名不能为空!";
		}
		StringBuilder sql = new StringBuilder();
		getDelSql(tableInt, sql, field, ids);
		int result = BaseDAO.getBaseDAO().updateNative(sql.toString());
		r.flag = result > 0;
		return r;
	}

	/**
	 * 删除(默认主键名为oid)
	 * 
	 * @param tableInt
	 * @param ids
	 * @return
	 */

	public static BoolMessage del(int tableInt, long[] oids) {
		return del(tableInt, "oid", oids);
	}

	
	
	/**
     * 删除(使用delete)
     * 
     * @param tableInt
     * @param field
     * @param ids
     * @return
     */
    public static BoolMessage delAbs(int tableInt, String field, long[] ids) {
        BoolMessage r = new BoolMessage();
        if (ParameterUtil.isNull(field)) {
            r.flag = false;
            r.fmessage = "主键字段名不能为空!";
        }
        StringBuilder sql = new StringBuilder();
        getAbsDelSql(tableInt, sql, field, ids);
        int result = BaseDAO.getBaseDAO().updateNative(sql.toString());
        r.flag = result > 0;
        return r;
    }

    /**
     * 删除(使用delete)
     * 
     * @param tableInt
     * @param field
     * @param ids
     * @return
     */
    public static BoolMessage delAbs(int tableInt, String field, String[] ids) {
        BoolMessage r = new BoolMessage();
        if (field != null && !"".equals(field)) {
            r.flag = false;
            r.fmessage = "主键字段名不能为空!";
        }
        StringBuilder sql = new StringBuilder();
        getAbsDelSql(tableInt, sql, field, ids);
        int result = BaseDAO.getBaseDAO().updateNative(sql.toString());
        r.flag = result > 0;
        return r;
    }

    /**
     * 删除(默认主键名为oid)(使用delete)
     * 
     * @param tableInt
     * @param ids
     * @return
     */

    public static BoolMessage delAbs(int tableInt, long[] oids) {
        return delAbs(tableInt, "oid", oids);
    }
	
	
	
	/**
	 * 生成查询sql语句,必须模型与数据库字段顺序和拼写完全一致
	 * 
	 * @param tableInt
	 * @param t
	 * @param sql
	 * @return
	 */
	public static <T> void getFindSQLWithoutWhere(int tableInt,
			Class<? extends Object> c, StringBuilder sql, String... nFields) {
		sql.append("select ");
		List<String> names = new LinkedList<String>();
		// 获取字段名称和值
		try {
			getFieldNames(names, c, nFields);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		for (String name : names) {
			sql.append("`"+name+"`" + ",");
		}
		sql.setCharAt(sql.length() - 1, ' ');
		sql.append("from {{?" + tableInt + "}} ");
	}

	
	public static <T> void getFindSQL(int tableInt, Class<? extends Object> c, String otherName,
			String[] selectExParams, StringBuilder sql, String... nFields) {
		sql.append("select ");
		List<String> names = new LinkedList<String>();
		// 获取字段名称和值
		try {
			getFieldNames(names, c, nFields);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		for (String name : names) {
			sql.append("`"+name+"`" + ",");
		}
		
		if (selectExParams != null) {
			for (String exParams : selectExParams) {
				sql.append(exParams + ",");
			}
		}
		
		sql.setCharAt(sql.length() - 1, ' ');
		sql.append("from {{?" + tableInt + "}} " 
		+ ((ParameterUtil.isNull(otherName)) ? "" : otherName) + " where cstatus&1=0");
	}
	
	/**
	 * 生成查询sql语句,必须模型与数据库字段顺序和拼写完全一致 注意：如果数据库字段存在DateTime类型 则不推荐使用该工具。
	 * 
	 * @param tableInt
	 * @param t
	 * @param sql
	 * @return
	 */
	public static void getFindSQL(int tableInt, Class<? extends Object> c,
			StringBuilder sql, String... nFields) {
		getFindSQL(tableInt, c, null, null, sql, nFields);
	}

	/**
	 * 从数据库获取模型数据数组
	 * 
	 * @param t
	 * @param results
	 * @param nFiled
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] getModels(Class<T> c,
			List<Object[]> results, String... nFiled) {
		T[] tArray = (T[]) Array.newInstance(c, results.size());
		if (results == null || !(results.size() > 0)) {
			return tArray;
		}
		List<String> names = new LinkedList<String>();
		// 获取对象所有属性
		getFieldNames(names, c, nFiled);
		String[] proName = names.toArray(new String[names.size()]);
		// 获取数组
		BaseDAO.getBaseDAO().convToEntity(results, tArray, (Class<T>) c,
				proName);
		
		return tArray;
	}

	/**
	 * With C, it would use a complex page to query a SQL.
	 * 
	 * @return
	 */
	public static Object[] getModelArrayC(PageHolder pageOut, Page page,
			String sortBy, String sql, Object[] params,
			Class<? extends Object> c, String... nFiled) {
		List<Object[]> results = BaseDAO.getBaseDAO().queryNativeC(pageOut,
				page, sortBy, sql, params);
		return getModels(c, results, nFiled);
	}

	/**
	 * 
	 * @param pageOut
	 * @param page
	 * @param sortBy
	 * @param sql
	 * @param params
	 * @param c
	 * @param nFiled
	 * @return
	 */
	public static Object[] getModelArray(PageHolder pageOut, Page page,
			String sortBy, String sql, Object[] params,
			Class<? extends Object> c, String... nFiled) {
		List<Object[]> results = BaseDAO.getBaseDAO().queryNative(pageOut,
				page, sortBy, sql, params);
		return getModels(c, results, nFiled);
	}

	public static Object[] getModelArray(PageHolder pageOut, Page page,
			String sortBy, StringBuilder sql, Object[] params,
			Class<? extends Object> c, String... nFiled) {
		return getModelArray(pageOut, page, sortBy, sql.toString(), params, c,
				nFiled);
	}

	/**
	 * 
	 * @param sql
	 * @param params
	 * @param c
	 * @param nFiled
	 * @return
	 */
	public static Object[] getModelArray(String sql, Object[] params,
			Class<? extends Object> c, String... nFiled) {
		List<Object[]> results = BaseDAO.getBaseDAO().queryNative(sql, params);
		return getModels(c, results, nFiled);
	}

	public static Object[] getModelArray(StringBuilder sql, Object[] params,
			Class<? extends Object> c, String... nFiled) {
		return getModelArray(sql.toString(), params, c, nFiled);
	}
	
	

	/**
	 * 拆分 指定格式的日期时间
	 * 
	 * @param date
	 *            日期
	 * @param pattern
	 *            日期格式
	 * @return String[] String[0]为日期yyyy-MM-dd,String[1]为时间HH:mm:ss
	 */
	public static String[] splitDate(String date, String pattern) {
		DateFormat fmt = new SimpleDateFormat(pattern);
		String[] dateTimes = new String[2];
		try {
			Date date2 = fmt.parse(date);
			dateTimes = splitDate(date2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateTimes;
	}

	public static String splitDateP(String date, String pattern) {
		DateFormat fmt = new SimpleDateFormat(pattern);
		String dateTimes = new String();
		try {
			Date date2 = fmt.parse(date);
			dateTimes = fmt.format(date2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateTimes;
	}

	/**
	 * 拆分 Date
	 * 
	 * @param date
	 *            日期
	 * @return String[] String[0]为日期yyyy-MM-dd,String[1]为时间HH:mm:ss
	 */
	public static String[] splitDate(Date date) {
		DateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat fmt2 = new SimpleDateFormat("HH:mm:ss");
		String[] dateTimes = new String[2];
		String regDate = fmt1.format(date);
		String regTime = fmt2.format(date);
		dateTimes[0] = regDate;
		dateTimes[1] = regTime;
		return dateTimes;
	}
	
	/**
	 * 获取日期和时间
	 * @param date
	 * @return
	 */
	public static String getDateAndTime(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		return sdFormat.format(date);
	}
	
	/**
	 * 获取日期
	 * @param date
	 * @return
	 */
	public static String getTime(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		String dateString = sdFormat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return (ldt.getHour() + ":" + ldt.getMinute() + ":" + ldt.getSecond());
	}
	

	/**
	 * 	获取时间
	 * @param date
	 * @return
	 */
	public static String getDate(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		String dateString = sdFormat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return (ldt.getYear() + "-" + ldt.getMonthValue() + "-" + ldt.getDayOfMonth());
	}

	/**
	 * 年
	 * @param date
	 * @return
	 */
	public static int getYear(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		String dateString = sdFormat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return ldt.getYear();
	}
	
	/**
	 * 月
	 * @param date
	 * @return
	 */
	public static int getMouth(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		String dateString = sdFormat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return ldt.getMonthValue();
	}
	
	/**
	 * 日
	 * @param date
	 * @return
	 */
	public static int getDay(Date date) {
		SimpleDateFormat sdFormat  = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		String dateString = sdFormat.format(date);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return ldt.getDayOfMonth();
	}
	
	/**
	 * 获取日期
	 * @param dateString
	 * @return
	 */
	public static String getDate(String dateString) {
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return (ldt.getHour() + ":" + ldt.getMinute() + ":" + ldt.getSecond());
	}
	

	/**
	 * 	获取时间
	 * @param dateString
	 * @return
	 */
	public static String getTime(String dateString) {
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);
		LocalDateTime ldt = LocalDateTime.parse(dateString, df);
		return (ldt.getYear() + "-" + ldt.getMonthValue() + "-" + ldt.getDayOfMonth());
	}
	
	
	private static long getCount(int tableName, String name, String columName,
			String pkName, long oid) {
		BaseDAO baseDao = BaseDAO.getBaseDAO();
		Object[] params;
		StringBuilder sql = new StringBuilder("select count(*) from {{?")
				.append(tableName + "}} where ");
		sql.append(columName + "=? ");
		if (oid > 0) {
			sql.append("and " + pkName + " <> ?");
			params = new Object[] { name, oid };
		} else {
			params = new Object[] { name };
		}

		List<Object[]> result = baseDao.queryNative(sql.toString(), params);
		if (result == null || result.size() != 1) {
			return -1;
		}
		long count = (long) result.get(0)[0];
		return count;
	}

	/**
	 * 查询根据某一条件是否有重复。多个条件之间的关联为and。<br>
	 * 注意该方法必要条件为cstatus&1=0！
	 * 
	 * @param tableName
	 * @param columNames
	 * @param values
	 * @return 是否可以添加。如果存在重复 则返回false。
	 */

	public static boolean isAddable(int tableName, String[] columNames,
			Object[] values) {
		return isAddable(tableName, columNames, values, null, null);
	}

	
	/**
	 * 查询根据某一条件是否有重复,并增加剔除限制。<br>
	 * <strong>注意该方法必要条件为cstatus&1=0！</strong>
	 * @param tableName
	 * @param columNames
	 * @param values
	 * @param neglectNames
	 * @param neglectValues
	 * @return
	 */
	
	public static boolean isAddable(int tableName , String[] columNames,
			Object[] values, String[] neglectNames, Object[] neglectValues) {
		StringBuilder sql = new StringBuilder();

		sql.append("select count(*) from {{?").append(tableName).append("}} ")
				.append("where ");

		if (columNames != null && values != null && columNames.length == values.length) {
			for (int i = 0; i < columNames.length; i++) {
				sql.append(" " + columNames[i] + " = ? ").append(" and ");
			}
		} else {
			values = null;
		}

		if (neglectNames != null && neglectValues != null && neglectNames.length == neglectValues.length) {
			for (int i = 0; i < neglectNames.length; i++) {
				sql.append(" " + neglectNames[i] + " <> ? ").append(" and ");
			}
		} else {
			neglectValues = null;
		}
		
		sql.append(" cstatus&1 = 0");

		List<Object[]> result = BaseDAO.getBaseDAO().queryNative(
				sql.toString(), ArrayUtils.addAll(values, neglectValues));
		
		if (result != null && result.size() > 0) {
			return (long) result.get(0)[0] == 0;
		}

		return true;
	}
	
	/**
	 * 获取某列数据数目 用于判断重名
	 * 
	 * @param tableName
	 * @param name
	 * @param columName
	 * @param oid
	 *            如果新增则为0 更新为oid
	 * @return 0表示可以新增 其余表示不能新增
	 */
	public static long getCount(int tableName, String name, String columName,
			long oid) {
		return getCount(tableName, name, columName, "oid", oid);
	}

	public static String getState(Map<Integer, String> states, int state) {
		if (states == null)
			return "没有定义审核状态规则";
		if ((256 & state) > 0) {
			return states.get(256);
		}
		if ((512 & state) > 0) {
			return states.get(512);
		}
		if ((128 & state) > 0) {
			return states.get(128);
		}
		return null;
	}

	public static int getAge(String birthdate, String dateFormat) {
		if (ParameterUtil.isNull(birthdate)) {
			return 0;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		SimpleDateFormat sdfy = new SimpleDateFormat("yyyy");
		SimpleDateFormat sdfm = new SimpleDateFormat("MMdd");
		// SimpleDateFormat sdfd = new SimpleDateFormat("dd");
		int age = 0;

		if (!ParameterUtil.isNull(birthdate)) {
			try {
				Date bdate = sdf.parse(birthdate);
				int byear = Integer.parseInt(sdfy.format(bdate));
				int bmon = Integer.parseInt(sdfm.format(bdate));

				Date ndate = new Date();
				int nyear = Integer.parseInt(sdfy.format(ndate));
				int nmon = Integer.parseInt(sdfm.format(ndate));

				age = nyear - byear - ((nmon < bmon) ? 1 : 0);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return age > 0 ? age : 0;
	}


	/**
	 * 
	 * @param tableNames
	 *            所有被关联的表
	 * @param columName
	 *            关联列名
	 * @param value
	 *            查询值
	 * @return 可能的值为： <br>
	 *         -1 -> 数据库错误。 <br>
	 *         0 -> 不存在关联。 <br>
	 *         1 -> 存在关联。
	 */

	public static int isLinked(int tableName, String columName, Object value) {
		BaseDAO baseDao = BaseDAO.getBaseDAO();
		Object[] params = new Object[] { value };
		StringBuilder sql = new StringBuilder("select count(*) from {{?")
				.append(tableName + "}} where cstatus&1 = 0 and ");
		sql.append(columName + "=? ");
		List<Object[]> result = baseDao.queryNative(sql.toString(), params);
		if (result == null || result.size() <= 0)
			return -1;
		long count = (long) result.get(0)[0];
		return count > 0 ? 1 : 0;
	}

	public static String[] getParamsBySql(String $sql) {
	    $sql = $sql.trim().toLowerCase();
		String temp = $sql.substring(6, $sql.indexOf("from {{"))
				.replace(" ", "").replace("`", "");
		String[] arr = temp.split(",");

		for (int i = 0; i < arr.length; i++) {
			if (arr[i].indexOf(".") != -1) {
				arr[i] = arr[i].split("\\.")[1];
			}
		}

		return arr;

	}

	/**
	 * 判断是否查询出结果。
	 * @param query 通过nativeQuery得到的对象。
	 * @return 如果未查询到则返回true。
	 */
	
	public static boolean queryEmpty(final List<Object[]> query) {
	    return query == null || query.isEmpty() || query.get(0) == null || query.get(0)[0] == null; 
	}

	/**
	 * 判断是否成功执行更新事务。
	 * @param uptTran 通过updateTransNative得到的对象。
	 * @return 如果失败则返回true
	 */
	public static boolean updateTransEmpty(final int[] uptTran) {
	    boolean result = uptTran == null || uptTran.length == 0;
	    
	    for (int i = 0; !result && i < uptTran.length; i++) {
	        result = uptTran[i] < 0;
	    }
	    return result;
	}
	/**
	 * @Title: getQCPS
	 * @author shanben-CN
	 * @Description: TODO 根据前端传进来的查询条件值，组装真实的查询条件值并组装动态的SQL查询条件语句。
	 * @param params 前端传进来的查询条件的参数值数组
	 * @param dyQCSQL 调用此方法的动态查询主SQL；
	 * @param dySQLVarNum 调用此方法的动态查询主SQL里动态变量个数；
	 * @return: Map<Integer,Object[]> 处理（包含类型转换、参数个数自动处理等）后的真实查询条件参数的值(索引值为一的对象)与动态查询条件SQL语句（索引值为0对象）
	 */
	public static Map<Integer,Object[]> getQCPS(QCParam[] params,String[] dyQCSQL,int dySQLVarNum){
		/*返回集合对象，索引0为主SQL里变量数组；索引1为查询参数的值数组*/
		Map<Integer,Object[]> returnResult = new HashMap<Integer,Object[]>();
		/*查询参数的值集合*/
		List<Object> trueParamsList= new ArrayList<Object>();
		/*查询条件动态SQL集合，用来取代主SQL的变量*/
		List<StringBuilder> queryConsqlist = new ArrayList< StringBuilder>();
		/*单个动态查询条件SQL：根据传进来的动态变量个数创建对应的个数*/
		StringBuilder queryConsql;
		for(int i = 0; i < dySQLVarNum; i++){
		    queryConsql= new StringBuilder();
		    queryConsqlist.add(queryConsql);    
		}
		/*日期格式化*/
		SimpleDateFormat dformat;
		String[] dformatStr = {"yyyy-MM-dd","yyyy-MM-dd HH:mm:ss"};
		/*记录循环次数变量：代表第几个参数*/
		int index = 0;
		for (QCParam param : params) {
			if (param.getPdv() != null && param.getPdv().length() > 0) {//判断是否需要产生查询条件，当值为空时代表前端没有用这个查询条件。
				switch (param.getPdt()) {
				case 0:// 日期到天
				case 1:// 日期到秒
					dformat = new SimpleDateFormat(dformatStr[param.getPdt()]);
					try {
						for(int i = 0; i < param.getMatchPN(); i++){
						    trueParamsList.add(dformat.parse(param.getPdv()));// 增加查询参数值。
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
					break;
				case 2:// int
					for(int i = 0; i < param.getMatchPN(); i++){
						trueParamsList.add(Integer.parseInt(param.getPdv()));// 增加查询参数值。
					}
					break;
				case 3:// long
					for(int i = 0; i < param.getMatchPN(); i++){
						trueParamsList.add(Long.parseLong(param.getPdv()));// 增加查询参数值。
					}
					break;
				default:// 字符串类型
					for(int i = 0; i < param.getMatchPN(); i++){
						trueParamsList.add(param.getPdv());// 增加查询参数值。
					}
					break;
				}
				if(dySQLVarNum > 0) queryConsqlist.get(param.getDyQCSQLIndex()).append(dyQCSQL[index]);// 增加查询条件SQL语句
			}
			index++;
		}
		Object[] trueParams = new Object[trueParamsList.size()];
		for(int i = 0; i < trueParamsList.size(); i++){
			trueParams[i] = trueParamsList.get(i);
		}
		String[] qcsa = new String[dySQLVarNum];
		for(int i = 0; i < dySQLVarNum; i++){
			qcsa[i] =  queryConsqlist.get(i).toString();
		}
		returnResult.put(0, qcsa);
		returnResult.put(1, trueParams);
		return returnResult;
	}
	
	/**
	 * 根据条件获取一个合适的ID。只适用于需要根据上一个最大的ID + 步长而生成的数。如：承运商机构码等。<br>
	 * 注意：忽略cstatus。
	 * @param table 需要依赖的表名
	 * @param row 需要自增的字段名
	 * @param step 步长
	 * @param init 初始值
	 * @return
	 */
	
	public static long getSuitableNo(int table, String row, int step, int init) {
	    long result = init;
	    
	    StringBuilder nativeSQL = new StringBuilder();
	    
	    nativeSQL.append("select IFNULL(MAX(").append(row).append("), " + (init - step) + ") ")
	             .append(" from {{?").append(table).append("}} ")
	             .append(" limit 1 ");
	    
	    List<Object[]> results = BaseDAO.getBaseDAO().queryNative(nativeSQL.toString(), new Object[] {});
	    
	    if (results != null && !results.isEmpty() && results.get(0) != null && results.get(0).length > 0) {
	        result = (long) results.get(0)[0] + step;
	    }
	    
	    return result;
	}
	
	/**
     * 根据条件获取一个合适的ID。只适用于需要根据上一个最大的ID + 步长而生成的数。如：承运商机构码等。<br>
     * 注意：忽略cstatus。步长默认为1，初始值默认为1
     * @param table 需要依赖的表名
     * @param row 需要自增的字段名
     * @return
     */
	public static long getSuitableNo(int table, String row) {
	    return getSuitableNo(table, row, 1, 1);
	}
	
	/**
	 * 查询某字段是否存在外部连接。
	 * @return true:存在外部连接 不可删除。
	 */
	public static boolean checkForeignLink(ForeignItem foreignObj, LinkedItem[] linkedObj) {
	    if (linkedObj.length == 0) {
	        throw new RuntimeException("Without any linkedObj!");
	    }
	    
	    StringBuilder querySQL = new StringBuilder();
	    
	    querySQL.append("SELECT IFNULL(COUNT(1), 0) ");
	    querySQL.append(" FROM {{?" + foreignObj.foreignTable + "}} BASE ");
	    querySQL.append(" WHERE BASE.cstatus&1 = 0 ");
	    querySQL.append(" AND ( 1 = 1 " + foreignObj.getForignLimitString() + " )");
	    querySQL.append(" AND ( 0 = 1 ");
	    
	    int index = 0;
	    for (LinkedItem linked : linkedObj) {
	        char nickname = (char) (65 + index++);
	        querySQL.append(" OR EXISTS(SELECT * FROM {{?" + linked.linkedTable + "}} " + nickname);
	        querySQL.append(" WHERE " + nickname + ".cstatus&1 = 0 ");
	        querySQL.append(linked.getExLimitString());
	        querySQL.append(" AND ( 0 = 1 ");
	        
	        for (String key : linked.linkedKeys) {
	            querySQL.append(" OR ");
	            querySQL.append(nickname + "." + key + " = BASE." + foreignObj.foreignKey);
	        }
	        //关 AND(
	        querySQL.append(" ) ");
	        
	        //关 EXISTS(
	        querySQL.append(" ) ");
        }
	    
	    //关 AND(
	    querySQL.append(" ) ");
	    
	    List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(querySQL.toString());
	    
	    try {
	        return Integer.parseInt(queryResult.get(0)[0].toString()) > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
	public static LinkedItem getLinkedItem(int linkedTable, String[] linkedKeys) {
	    return new LinkedItem(linkedTable, linkedKeys, null);
	}
	
	public static LinkedItem getLinkedItem(int linkedTable, String[] linkedKeys, String[] exLimit) {
	    return new LinkedItem(linkedTable, linkedKeys, exLimit);
	}
	
	public static LinkedItem[] getLinkedItems(LinkedItem... linkedItems) {
        return linkedItems;
    }
	
	public static ForeignItem getForeignItem(int foreignTable, String foreignKey, String[] limits) {
	    return new ForeignItem(foreignTable, foreignKey, limits);
	}
	
	public static ForeignItem getForeignItem(int foreignTable, String foreignKey) {
	    return new ForeignItem(foreignTable, foreignKey, null);
	}
	
	public static class LinkedItem {
	    private int linkedTable;
        private String[] linkedKeys;
        private String[] exLimit;
        
        private LinkedItem(int linkedTable, String[] linkedKeys, String[] exLimit) {
            this.linkedTable = linkedTable;
            this.linkedKeys = linkedKeys;
            this.exLimit = exLimit;
        }
        
        private String getExLimitString() {
            StringBuilder result = new StringBuilder();
            
            if (exLimit != null) {
                for (String exlimit : exLimit) {
                    result.append(" AND " + exlimit);
                }
            }
            
            return result.toString();
        }
        
	}
	
	public static class ForeignItem {
	    private int foreignTable;
	    private String foreignKey;
	    private String[] limits;
	    
        private ForeignItem(int foreignTable, String foreignKey, String[] limits) {
            this.foreignTable = foreignTable;
            this.foreignKey = foreignKey;
            this.limits = limits;
        }
        
        private String getForignLimitString() {
            StringBuilder result = new StringBuilder();
            
            if (limits != null && limits.length > 0) {
                for (String limit : this.limits) {
                    result.append(" AND " + limit);
                }
            } 
            
            return result.toString();
        }

	}
	
	public static boolean isContains(int tableInt, String colname, Object value) {
	    List<Object[]> queryResult = 
	            BaseDAO.getBaseDAO().queryNative("select IFNULL(count(*), 0) from {{?" + tableInt + "}} where " + colname + " = ? ", value);
	    
	    if (queryResult == null || queryResult.isEmpty() || queryResult.get(0)[0] == null) {
	        return false;
	    }
	    
	    return Integer.valueOf(queryResult.get(0)[0].toString()) > 0;
	}
	
}
