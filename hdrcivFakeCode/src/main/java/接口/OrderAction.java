package 接口;


import org.springframework.beans.factory.annotation.Autowired;
import 逻辑.OrderService;

import java.util.Map;

/**
 * 类描述：医嘱Action
 *
 * @author 余涛
 * @Date 2021/1/14
 */
public class OrderAction {

    private static final long serialVersionUID = 1L;

    @Autowired
    private OrderService orderService;

    /**
     * 当前视图 - 药品医嘱
     * @param patientId   患者ID
     * @param visitId 患者就诊次数
     * @param orderStatus   医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
     * @param orderProperty 医嘱性质   1-临时    2-长期
     * @param orderType     医嘱类型  1-口服   2-静脉
     * @param mainDiag      主诊断
     * @param dept_code     末次科室
     * @param pageNo 当前页码
     * @param pageSize 每页大小
     */
    public void getCVDrugList(String patientId,String visitId,String orderStatus, String orderProperty, String orderType, String mainDiag, String dept_code,int pageNo, int pageSize) {
      /*  如果	patientId	不为空
            调用逻辑层的 getCVDrugList 方法,将浏览器给的参数传入逻辑层处理,查出结果分页返回给浏览器 */

            /*  以下为 getCVDrugList 方法的调用链接
                 @parm  "INPV" 住院标识，对应visitType（就诊类型）字段，
                 @param  "Drag"  药品标识，对应orderCode（）字段  */
                 orderService.getCVDrugList(patientId, visitId, orderType, "INPV", orderStatus, orderProperty,
                        mainDiag, dept_code, "Drag", pageNo, pageSize);

      /*
      如果 patientId 为空值
          返回给浏览器空页面
       */
    }

    /**
     * 口服药品医嘱
     * @param patientId   患者ID
     * @param visitId 患者就诊次数
     * @param visitType 就诊类型
     * @param orderStatus   医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
     * @param orderProperty 医嘱性质   1-临时    2-长期
     */
    public void getDrugListKF(String patientId,String visitId,String visitType,String orderStatus,String orderProperty) {


        Page<Map<String, String>> result = orderService.getDrugListKF(patientId, visitId, visitType, orderStatus,
                orderProperty, "", "", "", pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 静脉药品医嘱
     */
    public void getDrugListJMZS() {
        //医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
        String orderStatus = getParameter("orderStatus");
        //医嘱性质   1-临时    2-长期
        String orderProperty = getParameter("orderProperty");
        Page<Map<String, String>> result = orderService.getDrugListJMZS(patientId, visitId, visitType, orderStatus,
                orderProperty, "", "", "", pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 其他药品医嘱
     */
    public void getDrugListQTYP() {
        //医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
        String orderStatus = getParameter("orderStatus");
        //医嘱性质   1-临时    2-长期
        String orderProperty = getParameter("orderProperty");
        Page<Map<String, String>> result = orderService.getDrugListQTYP(patientId, visitId, visitType, orderStatus,
                orderProperty, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * 获取护理医嘱
     */
    public void getNurseOrder() {
        //医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
        String orderStatus = getParameter("orderStatus");
        //医嘱性质   1-临时    2-长期
        String orderProperty = getParameter("orderProperty");
        Page<Map<String, String>> result = orderService.getNurseOrderList(patientId, visitId, visitType, orderStatus,
                orderProperty, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * 获取其他医嘱
     */
    public void getOthersOrder() {
        //医嘱状态   1-下达  2-审核  3-开始  4-撤销  5-停止
        String orderStatus = getParameter("orderStatus");
        //医嘱性质   1-临时    2-长期
        String orderProperty = getParameter("orderProperty");
        Page<Map<String, String>> result = orderService.getOthersOrderList(patientId, visitId, visitType, orderStatus,
                orderProperty, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 检验医嘱
     */
    public void getLabList() {
        Page<Map<String, String>> result = orderService.getLabList(patientId, visitId, visitType, "", "", "", pageNo,
                pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 检查医嘱
     */
    public void getExamList() {
        Page<Map<String, String>> result = orderService.getExamList(patientId, visitId, visitType, "", "", "", pageNo,
                pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 手术医嘱
     */
    public void getOperList() {
        Page<Map<String, String>> result = orderService.getOperList(patientId, visitId, visitType, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 用血申请
     */
    public void getBloodList() {
        Page<Map<String, String>> result = orderService.getBloodList(patientId, visitId, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 医嘱执行记录
     */
    public void getOrderExeList() {
        String orderNo = getParameter("orderNo");
        Page<Map<String, String>> result = orderService.getOrderExeList(patientId, visitId, orderNo, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 药品医嘱发药审核
     */
    public void getDrugCheckList() {
        String orderNo = getParameter("orderNo");
        Page<Map<String, String>> result = orderService.getDrugCheckList(patientId, visitId, orderNo, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 检验医嘱详情
     */
    public void getLabReportDetails() {
        String field = Config.getOrderCloseJoinField();
        String[] fieldValue = field.split("\\|");
        String orderno = getParameter(fieldValue[1]);
        Page<Map<String, String>> result = orderService.getLabReportDetails(patientId, visitType, fieldValue[0],
                orderno, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 检查医嘱详情
     */
    public void getExamReportDetails() {
        String field = Config.getOrderCloseJoinField();
        String[] fieldValue = field.split("\\|");
        String orderno = getParameter(fieldValue[1]);
        //		String orderNo = getParameter("orderNo");
        Map<String, String> result = orderService.getExamReportDetails(patientId, visitType, fieldValue[0], orderno);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 查询患者医嘱
     */
    public void getOrders() {
        //医嘱状态
        String orderStatus = getParameter("orderStatus");
        //医嘱性质   1-临时    2-长期
        String orderProperty = getParameter("orderProperty");
        //医嘱类型   KF-口服药   JM-静脉药    QT-其他
        String orderType = getParameter("orderType");
        //药品名称
        String orderName = getParameter("keyWord").replace("(", "").replace(")", "");
        //开立时间
        String orderTimeBegin = getParameter("orderTimeBegin");
        String orderTimeEnd = getParameter("orderTimeEnd");
        String outPatientId = getParameter("outPatientId");
        String visitType = getParameter("visitType");
        Page<Map<String, String>> result = orderService.getOrders(patientId, orderStatus, orderProperty, orderType,
                orderTimeBegin, orderTimeEnd, orderName, orderBy, orderDir, pageNo, pageSize, outPatientId, visitType);
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 医嘱闭环显示配置
     */
    public void getOrderCloseShowConfig() {
        Map<String, String> result = Config.getCIV_CATEGARY_ORDER_SHOW_CONFIG();
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 每次就诊全部医嘱
     */
    public void getVisitView() {
        String patientId = getParameter("patientId");
        String visitId = getParameter("visitId");
        String visitType = getParameter("visitType");
        String orderStatus = getParameter("orderStatus");
        String orderProperty = getParameter("orderProperty");
        String orderType = getParameter("orderType");

        renderJson(JsonUtil.getJSONString(orderService.getVisitPageView(patientId, visitId, visitType, orderStatus,
                orderProperty, orderType, orderBy, orderDir, pageNo, pageSize)));
    }

}
