package com.onek.op;

import com.onek.entity.TranOrder;
import com.onek.entitys.Result;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.onek.util.FileServerUtils.getExcelDownPath;

public class ExcelOrdersInfoOP {

    private static final String[] HEADERS = {"订单信息","订单详情","收货信息","发票信息"};
    private static final String[] ORDER_MSG_HEAD = {"订单号","下单时间","订单状态","支付时间","付款方式","订单备注"};
    private static final String[] ORDER_DETIL_HEAD = {"商品名称","生产厂家","有效日期","商品单价","购买数量","商品实付金额"};
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
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
           HSSFWorkbook workbook = new HSSFWorkbook();

           createExcelOrdersInfoHeader(workbook);

           workbook.write(bos);
           String title = getExcelDownPath(FILENAME.toString(), new ByteArrayInputStream(bos.toByteArray()));
           return new Result().success(title);
       }catch (Exception e){
           e.printStackTrace();
       }
       return new Result().fail("导出失败");
    }

    private void createExcelOrdersInfoHeader(HSSFWorkbook workbook){
        HSSFSheet sheet = workbook.createSheet("订单信息");
        HSSFRow row = sheet.createRow(0);

        HSSFRow row1 = sheet.createRow(1);
        int startcell = 0;
        int nextcell = 0;
        for(int i =0; i<HEADERS.length;i++){
            HSSFCell cell = row.createCell(startcell);//0  6
            cell.setCellStyle(headerCellStyle(workbook));
            cell.setCellValue(HEADERS[i]);
            String[] heads = getHeads(HEADERS[i]);
            System.out.println(HEADERS[i]);
            for(int j = 0;j<heads.length; j++){
                HSSFCell cell1 = row1.createCell(nextcell);
                cell1.setCellStyle(headerCellStyle(workbook));
                cell1.setCellValue(heads[j]);
                nextcell++;
            }
            System.out.println("开始=======" + (startcell>0?startcell:0));
            System.out.println("结束=======" + (startcell+getHeadMerge(HEADERS[i])-1));
            sheet.addMergedRegion(new CellRangeAddress(0,0,startcell>0?startcell:0,startcell+getHeadMerge(HEADERS[i])-1));//5 11
            startcell = startcell+getHeadMerge(HEADERS[i]);//6
        }

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
