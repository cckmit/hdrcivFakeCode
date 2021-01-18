package 逻辑;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderService {
    /**
     *
     * 方法描述: 当前视图 - 药品医嘱
     * @param patientId 患者编号
     * @param visitId 就诊次数
     * @param orderType 医嘱类型
     * @param visitType 就诊类型
     * @param orderStatus 医嘱状态
     * @param orderProperty 医嘱性质
     * @param mainDiag 主诊断
     * @param dept_code  末次科室
     * @param orderCode
     * @param pageNo 页码
     * @param pageSize 每页大小
     */
    public void getCVDrugList(String patientId, String visitId, String orderType, String visitType,
                              String orderStatus, String orderProperty, String mainDiag,
                              String dept_code, String orderCode, int pageNo, int pageSize) {

        Page<Map<String, String>> page = new Page<Map<String, String>>();

        //类型判断 口服 药品
        如果orderType不为空

        if (StringUtils.isBlank(orderType)) {

            List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            //口服 + 静脉
               调用 getDrugListKF 方法获取口服
            Page<Map<String, String>> kp = getDrugListKF(patientId, visitId, visitType, orderStatus, orderProperty,
                    mainDiag, orderCode, deptCode, 0, 0);
               调用 getDrugListJMZS 方法获取静脉注射
            Page<Map<String, String>> jp = getDrugListJMZS(patientId, visitId, visitType, orderStatus, orderProperty,
                    mainDiag, orderCode, deptCode, 0, 0);
            //合并
            list.addAll(kp.getResult());
            list.addAll(jp.getResult());
            //排序
            Utils.sortListByDate(list, "orderTime", "desc");
            //按时间排序完后把重要指标显示在前列
            //查询专科视图配置  用药医嘱
            List<Map<String, String>> listDept = specialtyViewPowerService.getSpecialtyConfig(mainDiag, orderCode,
                    deptCode);
            List<Map<String, String>> data = new ArrayList<Map<String, String>>();
            List<Map<String, String>> dataTemp = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : list) {
                String orderCodeTemp = map.get("orderItemCode");
                map.remove("orderItemCode");
                boolean isBreak = false;
                for (Map<String, String> mapSpecialty : listDept) {
                    String code = mapSpecialty.get("subItemCode");
                    if (StringUtils.isNotBlank(orderCodeTemp) && orderCodeTemp.equals(code)) {
                        data.add(map);
                        isBreak = true;
                        break;
                    }
                }
                if (isBreak) {
                    continue;
                }
                dataTemp.add(map);
            }
            //合并数据
            for (Map<String, String> map : dataTemp) {
                data.add(map);
            }

            //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(data, pageNo, pageSize);
            page.setPageNo(listPage.getNowPage());
            page.setPageSize(listPage.getPageSize());
            page.setResult(listPage.getPagedList());
            page.setTotalCount(listPage.getTotalCount());
        } else if ("kf".equals(orderType)) {
            page = getDrugListKF(patientId, visitId, visitType, orderStatus, orderProperty, mainDiag, orderCode,
                    deptCode, pageNo, pageSize);
        } else if ("jm".equals(orderType)) {
            page = getDrugListJMZS(patientId, visitId, visitType, orderStatus, orderProperty, mainDiag, orderCode,
                    deptCode, pageNo, pageSize);
        }

        if (page.getTotalCount() <= 0) {
            return page;
        }
        //		//===========药品不良反应处理==========
        //		if(Config.getCiv_Cv_Drugadr()){
        //			String vtype = "02";
        //			if ("OUTPV".equals(visitType)) {
        //				vtype = "01";
        //			}
        //			for (Map<String, String> map : page) {
        //				//医嘱号
        //				String orderNo = map.get("orderNo");
        //				List<Map<String, String>> mnList = adrMnDao.getDrugMonitorByPv(patientId, visitId, vtype, orderNo);
        //				if (mnList.size() > 0) {
        //					Utils.checkAndPutToMap(map, "reaction", mnList.get(0).get("event_name"), "不良反应情况不明", false);
        //				}
        //			}
        //		}
        return page;
    }



}
