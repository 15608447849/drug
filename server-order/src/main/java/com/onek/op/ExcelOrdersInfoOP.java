package com.onek.op;

import com.google.gson.*;
import com.onek.context.AppContext;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderDetail;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.IceRemoteUtil;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import util.TimeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.onek.util.FileServerUtils.getExcelDownPath;

public class ExcelOrdersInfoOP {

    private static final String[] HEADERS = {"订单信息","订单详情","收货信息","发票信息"};
    private static final String[] ORDER_MSG_HEAD = {"订单号","下单时间","订单状态","支付时间","付款方式","应付金额","运费","实付金额","订单备注"};
    private static final String[] ORDER_DETIL_HEAD = {"商品名称","生产厂家","有效日期","商品单价","商品优惠价","购买数量","优惠","余额抵扣","商品实付金额"};
    private static final String[] RECEIV_MSG_HEAD = {"下单药店","收货人","地址","收货电话"};
    private static final String[] INVOICE_MSG_HEAD = {"发票类型","公司名称","公司地址","纳税人称号","开户银行","银行账号"};
    private static Map<String,Integer> map = new HashMap<String,Integer>();

    private static final String FILENAME = "订单信息";
    private TranOrder[] tranOrders;

    public ExcelOrdersInfoOP(TranOrder[] tranOrders){
        this.tranOrders = tranOrders;
    }

    public Result excelOrderInfo(){
       try{
           long stime = System.currentTimeMillis();
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
           HSSFWorkbook workbook = new HSSFWorkbook();

           HSSFSheet sheet = workbook.createSheet("订单信息");
           createExcelOrdersInfoHeader(workbook,sheet);
           createExcelOrderGoodsInfo(workbook,sheet);
           long etime = System.currentTimeMillis();
           System.out.println("=========================查询订单数据以及填充数据使用 = " + (etime-stime)/1000);
           workbook.write(bos);
           String title = getExcelDownPath(FILENAME.toString(), new ByteArrayInputStream(bos.toByteArray()));
           return new Result().success(title);
       }catch (Exception e){
           e.printStackTrace();
       }
       return new Result().fail("导出失败");
    }

    private void createExcelOrdersInfoHeader(HSSFWorkbook workbook,HSSFSheet sheet){
        HSSFRow row = sheet.createRow(0);

        HSSFRow row1 = sheet.createRow(1);
        int startcell = 0;
        int nextcell = 0;
        for(int i =0; i<HEADERS.length;i++){
            HSSFCell cell = row.createCell(startcell);//0  6
            cell.setCellStyle(headerCellStyle(workbook));
            cell.setCellValue(HEADERS[i]);
            String[] heads = getHeads(HEADERS[i]);
            for(int j = 0;j<heads.length; j++){
                HSSFCell cell1 = row1.createCell(nextcell);
                cell1.setCellStyle(headerCellStyle(workbook));
                cell1.setCellValue(heads[j]);
                nextcell++;
            }
            sheet.addMergedRegion(new CellRangeAddress(0,0,startcell>0?startcell:0,startcell+getHeadMerge(HEADERS[i])-1));//5 11
            startcell = startcell+getHeadMerge(HEADERS[i]);//6
        }

    }


    private void createExcelOrderGoodsInfo(HSSFWorkbook workbook,HSSFSheet sheet){
        int startrow = 2;
        int marginnum = 2;
        HSSFCellStyle style = dataCellStyle(workbook);
        for (int i =0 ; i<tranOrders.length; i++){
            String json = IceRemoteUtil.getOrderDetail(tranOrders[i].getCusno(),Long.valueOf(tranOrders[i].getOrderno()));
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
            JsonArray tranOrderDetails = jsonObject.getAsJsonObject("data").getAsJsonArray("goods");

            JsonArray giftDetails = jsonObject.getAsJsonObject("data").getAsJsonArray("gifts");
            if(tranOrderDetails.size()<=0){
                continue;
            }
            List<TranOrderDetail> orList = jsonArrayToOrderList(tranOrderDetails);
            List<TranOrderGoods> giftList = jsonArrayToGiftList(giftDetails);
            if(orList.get(0).getGoods().size()<=0){
                continue;
            }

            HSSFRow row = sheet.createRow(startrow++);


            //订单编号
            createCell(style,row,0,tranOrders[i].getOrderno());
            //下单时间
            createCell(style,row,1,tranOrders[i].getOdate()+" "+tranOrders[i].getOtime());
            //订单状态
            createCell(style,row,2,getOrderStatus(tranOrders[i].getOstatus()));
            //支付时间
            createCell(style,row,3,orList.get(0).getPaydate());
            //付款方式
            createCell(style,row,4,getOrderPayWay(Integer.valueOf(tranOrders[i].getPayway()).intValue()));
            //应付金额
            createCell(style,row,5,String.valueOf(orList.get(0).getPdamt())+" 元");
            //运费
            createCell(style,row,6,String.valueOf(orList.get(0).getFreight())+" 元");
            //实付金额
            createCell(style,row,7,String.valueOf(orList.get(0).getPayamt())+" 元");
            //订单备注
            createCell(style,row,8,tranOrders[i].getRemarks());


            //商品名称
            createCell(style,row,9,orList.get(0).getGoods().get(0).getPname());
            //生产厂家
            createCell(style,row,10,orList.get(0).getGoods().get(0).getManun());
            //有效期
            createCell(style,row,11,orList.get(0).getGoods().get(0).getCreatedate());
            //商品单价
            createCell(style,row,12,String.valueOf(orList.get(0).getGoods().get(0).getRrp()));
            //商品优惠价
            createCell(style,row,13,String.valueOf(orList.get(0).getGoods().get(0).getPdprice()));
            //购买数量
            createCell(style,row,14,String.valueOf(orList.get(0).getGoods().get(0).getPnum()));
            //优惠
            createCell(style,row,15,String.valueOf(orList.get(0).getGoods().get(0).getDistprice()));
            //余额抵扣
            createCell(style,row,16,String.valueOf(orList.get(0).getGoods().get(0).getBalamt()));
            //商品实付金额
            createCell(style,row,17,String.valueOf(orList.get(0).getGoods().get(0).getPayamt()));


            //下单药店
            createCell(style,row,18,orList.get(0).getCusname());
            //收货人
            createCell(style,row,19,orList.get(0).getConsignee());
            //地址
            createCell(style,row,20,orList.get(0).getAddress());
            //收货电话
            createCell(style,row,21,orList.get(0).getContact());
            //发票类型
            createCell(style,row,22,getinvoicetype(orList.get(0).getInvoicetype()));
            //公司名称
            createCell(style,row,23,orList.get(0).getCusname());
            //公司地址
            createCell(style,row,24,orList.get(0).getCusaddress());
            //纳税人识别号
            createCell(style,row,25,orList.get(0).getInvoice()!= null ? orList.get(0).getInvoice().getTaxpayer():"-");
            //开户银行
            createCell(style,row,26,orList.get(0).getInvoice()!= null ? orList.get(0).getInvoice().getBankers():"-");
            //银行账号
            createCell(style,row,27,orList.get(0).getInvoice()!= null ? orList.get(0).getInvoice().getAccount():"-");
            if(orList.get(0).getGoods().size()>1) {
                for (int j = 1; j < orList.get(0).getGoods().size(); j++) {
                    HSSFRow orRow = sheet.createRow(startrow++);
                    //商品名称
                    createCell(style, orRow, 9, orList.get(0).getGoods().get(j).getPname());
                    //生产厂家
                    createCell(style, orRow, 10, orList.get(0).getGoods().get(j).getManun());
                    //有效期
                    createCell(style, orRow, 11, orList.get(0).getGoods().get(j).getCreatedate());
                    //商品单价
                    createCell(style, orRow, 12, String.valueOf(orList.get(0).getGoods().get(j).getRrp()));
                    //商品优惠价
                    createCell(style, orRow, 13, String.valueOf(orList.get(0).getGoods().get(j).getPdprice()));
                    //购买数量
                    createCell(style, orRow, 14, String.valueOf(orList.get(0).getGoods().get(j).getPnum()));
                    //优惠
                    createCell(style, orRow, 15, String.valueOf(orList.get(0).getGoods().get(j).getDistprice()));
                    //余额抵扣
                    createCell(style, orRow, 16, String.valueOf(orList.get(0).getGoods().get(j).getBalamt()));
                    //商品实付金额
                    createCell(style, orRow, 17, String.valueOf(orList.get(0).getGoods().get(j).getPayamt()));
                }
                if(giftList.size()>0){
                    for(int giftsize = 0; giftsize<giftList.size();giftsize++){
                        HSSFRow orRow = sheet.createRow(startrow++);
                        //商品名称
                        createCell(style, orRow, 9, giftList.get(giftsize).getPname());
                        //生产厂家
                        createCell(style, orRow, 10, "赠品");
                        //有效期
                        createCell(style, orRow, 11, "-");
                        //商品单价
                        createCell(style, orRow, 12, "-");
                        //商品优惠价
                        createCell(style, orRow, 13, "-");
                        //购买数量
                        createCell(style, orRow, 14, String.valueOf(giftList.get(giftsize).getPnum()));
                        //优惠
                        createCell(style, orRow, 15, "-");
                        //余额抵扣
                        createCell(style, orRow, 16, "-");
                        //商品实付金额
                        createCell(style, orRow, 17, "-");
                    }
                }
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 0, 0));//第二行到第四行，第一列
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 1, 1));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 2, 2));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 3, 3));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 4, 4));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 5, 5));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 6, 6));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 7, 7));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 8, 8));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 18, 18));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 19, 19));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 20, 20));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 21, 21));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 22, 22));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 23, 23));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 24, 24));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 25, 25));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 26, 26));
                sheet.addMergedRegion(new CellRangeAddress(marginnum, marginnum + orList.get(0).getGoods().size()+giftList.size() - 1, 27, 27));

                marginnum = marginnum + orList.get(0).getGoods().size()+giftList.size();
            }else{
                marginnum++;
            }
        }
    }

    private List<TranOrderDetail> jsonArrayToOrderList(JsonArray array){
        Gson gson = new Gson();
        List<TranOrderDetail> list = new ArrayList<TranOrderDetail>();
        for (JsonElement tranOrderDetail : array){
            TranOrderDetail ldvo = gson.fromJson(tranOrderDetail, TranOrderDetail.class);
            list.add(ldvo);
        }

        return list;
    }

    private  List<TranOrderGoods> jsonArrayToGiftList(JsonArray array){
        Gson gson = new Gson();
        List<TranOrderGoods> list = new ArrayList<TranOrderGoods>();
        for (JsonElement gift : array){
            TranOrderGoods giftvo = gson.fromJson(gift, TranOrderGoods.class);
            list.add(giftvo);
        }

        return list;
    }


    private String getinvoicetype(int invoicetype){
        String text = "";
        switch (invoicetype) {
            case 1:
                text = "电子普通发票";
                break;
            case 2:
                text = "纸质普通发票";
                break;
            case 3:
                text = "增值税专用发票";
                break;
            default:
                text = "电子普通发票";
        }
        return text;
    }

    private String getOrderPayWay(int payway){
        String text = "";
        switch (payway) {
            case 0:
                text = "余额支付";
                break;
            case 1:
                text = "微信支付";
                break;
            case 2:
                text = "支付宝支付";
                break;
            case 3:
                text = "银联支付";
                break;
            case 4:
                text = "线下转账";
                break;
            case 5:
                text = "线下到付";
                break;
        }
        return text;
    }

    private String getOrderStatus(int ostatus){
        String text = "";
        switch(ostatus){
            case 0:
                text = "未付款";
                break;
            case 1:
                text ="待发货";
                break;
            case 2:
                text = "已发货";
                break;
            case 3:
                text ="已签收";
                break;
            case 4:
                text = "已完成";
                break;
            case -1:
                text = "退货申请";
                break;
            case -2:
                text = "退货中";
                break;
            case -3:
                text = "已退货";
                break;
            case -4:
                text = "交易取消";
                break;
        }
        return text;
    }

    /**
     * 单元格赋值
     * @param style
     * @param row
     * @param i
     * @param value
     */
    private static void createCell(HSSFCellStyle style, HSSFRow row, int i, String value) {
        HSSFCell cell;
        cell = row.createCell(i);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private HSSFCellStyle headerCellStyle(HSSFWorkbook workbook){
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short)16);
        style.setFont(font);
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        return style;
    }

    private HSSFCellStyle dataCellStyle(HSSFWorkbook workbook){
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short)14);
        style.setWrapText(true);
        style.setFont(font);
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        return style;
    }

    private int getHeadMerge(String s){
        if("订单信息".equals(s)){
           return ORDER_MSG_HEAD.length;
        }else if("订单详情".equals(s)){
            return ORDER_DETIL_HEAD.length;
        }else if("收货信息".equals(s)){
            return RECEIV_MSG_HEAD.length;
        }else if("发票信息".equals(s)){
            return INVOICE_MSG_HEAD.length;
        }
        return 0;
    }
    private String[] getHeads(String s){
        if("订单信息".equals(s)){
            return ORDER_MSG_HEAD;
        }else if("订单详情".equals(s)){
            return ORDER_DETIL_HEAD;
        }else if("收货信息".equals(s)){
            return RECEIV_MSG_HEAD;
        }else if("发票信息".equals(s)){
            return INVOICE_MSG_HEAD;
        }
        return null;
    }
}
