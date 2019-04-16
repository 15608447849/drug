package com.onek.discount.calculate.service.calculate;

import com.onek.discount.calculate.entity.Gift;
import com.onek.discount.calculate.entity.Ladoff;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivityCalculateService extends BaseDiscountCalculateService {
    private static final String GET_LADOFF =
            " SELECT lad.* "
            + " FROM {{?" + DSMConst.TD_PROM_LADOFF + "}} lad"
            + " INNER JOIN {{?" + DSMConst.TD_PROM_RELA + "}} rela "
            + " ON lad.cstatus&1 = 0 AND rela.cstatus&1 = 0 "
            + " AND lad.unqid = rela.ladid AND rela.actcode = ? "
            + " WHERE 1 = 1 ";

    private static final String GET_GIFT =
            " SELECT * "
                    + " FROM {{?" + DSMConst.TD_PROM_GIFT + "}} gift "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_ASSGIFT + "}} ass "
                    + " ON gift.unqid = ass.assgiftno AND ass.offercode = ? "
                    + " AND ass.cstatus&1 = 0 AND gift.cstatus&1 = 0 "
                    + " WHERE 1=1 ";

    private List<Gift> getGifts(long offerCode) {
        List<Gift> result = new ArrayList<>();

        if (offerCode > 0) {
            List<Object[]> queryResult =
                    BaseDAO.getBaseDAO().queryNative(GET_GIFT, offerCode);

            Gift[] gArray = new Gift[queryResult.size()];

            BaseDAO.getBaseDAO().convToEntity(queryResult, gArray, Gift.class);

            result.addAll(Arrays.asList(gArray));
        }

        return result;
    }


    @Override
    public Ladoff[] getLadoffs(long actCode) {
        List<Object[]> ladResult =
                BaseDAO.getBaseDAO().queryNative(GET_LADOFF, actCode);

        Ladoff[] lapArray = new Ladoff[ladResult.size()];

        BaseDAO.getBaseDAO().convToEntity(ladResult, lapArray, Ladoff.class);

        for (Ladoff ladoff : lapArray) {
            ladoff.setLadamt(ladoff.getLadamt() / 100);
            ladoff.setOffer(ladoff.getOffer() / 100);
            ladoff.setGiftList(getGifts(ladoff.getOffercode()));
        }

        return lapArray;
    }
}
