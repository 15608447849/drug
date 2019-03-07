package org.entity;

import org.annation.RedisKey;


@RedisKey(prefix = "dict_", key = "dictc")
public class DictVo {
	private Integer dictc;
	private String type;
	private String text;
	private int cstatus;
	private String remark;

	public Integer getDictc() {
		return dictc;
	}

	public void setDictc(Integer dictc) {
		this.dictc = dictc;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getCstatus() {
		return cstatus;
	}

	public void setCstatus(int cstatus) {
		this.cstatus = cstatus;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

}
