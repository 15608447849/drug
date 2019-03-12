package com.onek;

import java.io.Serializable;

public class UserSession implements Serializable {
    public long oid;
    public String uname;
    public long roleid;
    public int cstatus;
    public long storeId; //门店id
}
