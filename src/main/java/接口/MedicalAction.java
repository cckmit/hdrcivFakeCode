package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.MedicalService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @author guozhenlei
 * @Description 类描述： 体检视图action
 * @modify 修改记录：
 */
public class MedicalAction extends CIVAction {
    @Autowired
    private MedicalService medicalService;

    /**
     * 体检视图获取体检人信息
     */
    public void getPatientInfo() {
        Map<String, String> map = medicalService.getPatientInfo(patientId, visitType);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取体检视图单次体检表头配置
     */
    public void getMedicalTableConfig() {
        List<Map<String, String>> config = medicalService.getMedicalTableHead();
        renderJson(JsonUtil.getJSONString(config));
    }

    /**
     * 获取体检视图列表
     */
    public void getMedicalVisitList() {
        //身份证号
        String inCardNo = getParameter("card_no");
        String beginDate = getParameter("begin_date");
        String endDate = getParameter("end_date");
        String dateType = getParameter("visitDate");
        List<Map<String, String>> list = medicalService.getMedicalListInfo(patientId, inCardNo, beginDate, endDate, dateType);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 获取体检视图首页
     */
    public void getMedicalSummary() {
        String medicalReportNo = getParameter("medicalReportNo");
        String cardNo = getParameter("card_no");
        Map<String, Object> res = medicalService.getMedicalSummaryHomePage(medicalReportNo, patientId, cardNo);
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取体检视图: 一般检查 内科 外科 眼科 耳鼻喉科
     *
     * @return
     */
    public void getCommonCheck() {
        String medicalReportNo = getParameter("medicalReportNo");
        String medicalItemClass = getParameter("medicalItemClass");
        String checkType = getParameter("checkType");
        Map<String, Object> map = medicalService.getMedicalCommonCheck(medicalReportNo, medicalItemClass, checkType);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 体检视图检验报告列表
     */
    public void getMedicalLabList() {
        String medicalReportNo = getParameter("medicalReportNo");
        String medicalItemClass = getParameter("medicalItemClass");
        Page<Map<String, String>> page = medicalService.getMedicalLabList(medicalReportNo, medicalItemClass, "EXECUTE_TIME", "desc", pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 体检视图检验报告明细
     */
    public void getMedicalLabDetail() {
        String medicalReportNo = getParameter("medicalReportNo");
        String medicalItemClass = getParameter("applyNo");
        String showUnNormal = getParameter("showUnNormal");
        String cardNo = getParameter("card_no");
        Map<String, Object> map = medicalService.getMedicalLabDetail(cardNo,medicalReportNo, medicalItemClass, showUnNormal);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 体检视图检查报告列表
     */
    public void getMedicalExamList() {
        String medicalReportNo = getParameter("medicalReportNo");
        String medicalItemClass = getParameter("medicalItemClass");
        Page<Map<String, String>> page = medicalService.getMedicalExamList(medicalReportNo, medicalItemClass, "EXECUTE_TIME", "desc", pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 体检视图检查报告明细
     */
    public void getMedicalExamDetail() {
        String medicalReportNo = getParameter("medicalReportNo");
        String applyNo = getParameter("applyNo");
        Map<String, Object> map = medicalService.getMedicalExamDetail(medicalReportNo, applyNo);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取医院信息
     */
    public void getMedicalHospialInfo() {
        Map<String, String> map = medicalService.getMedicalHospialInfo();
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 统计检查和检验数量
     */
    public void getMedicalReportNum() {
        String medicalReportNo = getParameter("medicalReportNo");
        String labClass = getParameter("lab_class");
        String examClass = getParameter("exam_class");
        Map<String, String> map = medicalService.getReportNum(medicalReportNo, labClass, examClass);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 体检趋势图
     */
    public void getMedicalTrendData() {
        String numIndex = getParameter("dateType");
        String beginTime = getParameter("startDate");
        String endTime = getParameter("endDate");
        String medicalItemCode = getParameter("medicalItemCode");
        medicalService.getReportTrendData(patientId,medicalItemCode,Integer.parseInt(numIndex),beginTime,endTime);
    }
}
