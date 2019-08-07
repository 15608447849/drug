package com.onek.user.operations;

import com.onek.entitys.Result;
import com.onek.user.entity.CompInfoVO;
import com.onek.user.entity.ProxyStoreVO;
import org.apache.poi.hssf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.onek.util.FileServerUtils.getExcelDownPath;

public class ExcelStoreInfoOP {
    private static final String[] HEADER = {"药店名称","门店注册手机","省","市","区","收货地址","企业类型","更新日期","是否已签控销协议","客服专员","药店状态"};
    private static final String FILENAME = "门店信息";
    private ProxyStoreVO[] proxyStoreVOS;

    public ExcelStoreInfoOP(ProxyStoreVO[] proxyStoreVOS){
        this.proxyStoreVOS = proxyStoreVOS;
    }

    public Result excelStoreInfo(){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

       try{
            //工作簿
            HSSFWorkbook workbook = new HSSFWorkbook();
            //列表页
            HSSFSheet sheet = workbook.createSheet("门店信息");
            //单元格样式
            HSSFCellStyle style = workbook.createCellStyle();
            HSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short)14);
            style.setWrapText(true);
            style.setFont(font);
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
            style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);

            HSSFRow row = sheet.createRow(0);

            HSSFCell cell = null;
            for (int i = 0;i<HEADER.length;i++){
                cell = row.createCell(i);
                cell.setCellStyle(headerCellStyle(workbook));
                cell.setCellValue(HEADER[i]);
            }

            for(int i = 0;i<proxyStoreVOS.length;i++){
                if(proxyStoreVOS[i].getCompanyId()<0){
                    continue;
                }
                row = sheet.createRow(i+1);
                createCompInfoDATA(row,cell,style,proxyStoreVOS[i]);
            }



            workbook.write(bos);
            String title = getExcelDownPath(FILENAME.toString(), new ByteArrayInputStream(bos.toByteArray()));
            return new Result().success(title);
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
        return new Result().fail("导出时失败");
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

    /**
     * 为门店excel导出填充数据
     * @param row 行
     * @param cell 列
     * @param style 样式
     * @param compInfo 门店信息
     * {"药店名称","门店注册手机","省","市","区","收货地址","企业类型","更新日期","是否已签控销协议","客服专员","药店状态"};
     */
    public void createCompInfoDATA(HSSFRow row,HSSFCell cell,HSSFCellStyle style,ProxyStoreVO compInfo) {
        //药店名称
        createCell(style,row,0,String.valueOf(compInfo.getCompany()));
        //门店注册手机
        createCell(style,row,1,String.valueOf(compInfo.getPhone()));
        //省
        createCell(style,row,2,String.valueOf(compInfo.getProvince()));
        //市
        createCell(style,row,3,String.valueOf(compInfo.getCity()));
        //区
        createCell(style,row,4,String.valueOf(compInfo.getRegion()));
        //收货地址
        createCell(style,row,5,String.valueOf(compInfo.getAddress()));
        //企业类型
        createCell(style,row,6,getCompType(compInfo.getStoretype()));
        //更新日期
        createCell(style,row,7,compInfo.getCreatedate()+" "+ compInfo.getCreatetime());
        //是否已签控销协议
        createCell(style,row,8,compInfo.getControl()>0 ? "是" : "否");
        //客服专员
        createCell(style,row,9,compInfo.getCursorName());
        //药店状态
        createCell(style,row,10,getCompStatus(compInfo.getStatus()));
    }
    private static void createCell(HSSFCellStyle style, HSSFRow row, int i, String value) {
        HSSFCell cell;
        cell = row.createCell(i);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    /**
     * 获取门店状态
     * @param status
     * @return
     */
    private String getCompStatus(int status){
        String text = "";
        if((status & 128) > 0){
            text = "待审核";
        } else if((status & 256) > 0) {
            text = "审核通过";
        } else if((status & 512) > 0) {
            text = "不通过";
        }
        return text;
    }

    /**
     * 获取门店类型
     * @param type
     * @return
     */
    private String getCompType(int type){
        String text ="";
        if(type==0){
            text = "医疗机构";
        }else{
            text = "药品经营企业";
        }
        return text;
    }
}
