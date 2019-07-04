package com.onek.user;

import constant.DSMConst;

public class SyncErrModule {
    private static final String QUERY_BASE =
            " SELECT unqid, synctype, syncid, syncmsg, cstatus, "
                + " syncdate, synctime, syncfrom, syncreason, synctimes, "
                + " syncway "
            + " FROM {{?" + DSMConst.TD_SYNC_ERROR + "}} "
            + " WHERE cstatus&1 = 0 ";

}
