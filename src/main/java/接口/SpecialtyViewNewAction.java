package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.SpecialtyViewNewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author guozhenlei
 * @Description 类描述：新版专科视图action
 * @modify 修改记录：
 */
public class SpecialtyViewNewAction extends CIVAction {

    private static final long serialVersionUID = 1L;

    @Autowired
    private SpecialtyViewNewService specialtyViewNewService;

    /**
     * 获取疾病列表
     * String userCode = SecurityUtils.getCurrentUserName(); //在线用户
     */
    public void getSicknessList() {
        String main_diag = getParameter("main_code");
//        String main_diag_name = getParameter("main_diag");
        if (null == main_diag || "-".equals(main_diag) || "".equals(main_diag)) {
            //todo  此处主诊断为空
        }
        specialtyViewNewService.getSicknessList(main_diag, pageNo, pageSize);

    }


    /**
     * 获取各疾病指标
     */
    public void getSicknessIndicatorList() {
        String main_diag = getParameter("main_code");
//        String main_diag_name = getParameter("main_diag");
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (StringUtils.isNotBlank(main_diag) || !"-".equals(main_diag)) {
            list = specialtyViewNewService.getSicknessIndicatorList(patientId, visitId, main_diag);
        }
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 折线图--重点生命体征
     */
    public void getChartData_FoldLine_Health() {
        String subItemCode = getParameter("code");
        String numIndex = getParameter("dateType");
//        String classCode = getParameter("classCode");
        String beginTime = getParameter("startDate");
        String endTime = getParameter("endDate");
        Map<String, Object> map = specialtyViewNewService.getFoldLineData(patientId, visitId, subItemCode, Integer.parseInt(numIndex), beginTime, endTime);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 折线图--重点检验结果
     */
    public void getChartData_FoldLine_LabDetail() {
        String labSubItemCode = getParameter("code");
//        String labSubItemName = getParameter("subItemName");
//        String classCode = getParameter("classCode");
        String numIndex = getParameter("dateType");
        String beginTime = getParameter("beginTime");
        String endTime = getParameter("endTime");
        Map<String, Object> map = specialtyViewNewService.getFoldLineLabData(patientId, visitId, labSubItemCode, Integer.parseInt(numIndex), beginTime, endTime);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 护理及饮食列表数据
     */
    public void getChartData_Table_NurseAndEat() {
        String itemCodes = getParameter("code");
        Page<Map<String, String>> listPage = specialtyViewNewService.getHlysListData(patientId, visitId, itemCodes, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(listPage));
    }

    /**
     * 重点用药-列表数据
     */
    public void getChartData_Table_Drag() {
        String itemCodes = getParameter("code");
        Page<Map<String, String>> list = specialtyViewNewService.getInorderListData(patientId, visitId, itemCodes, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 重点检验左侧id列表
     */
    public void getChartData_List_Lab() {
        String itemCode = getParameter("code");
        String pName = getParameter("pname");
        List<Map<String, String>> list = specialtyViewNewService.getZdjyCardIdList(patientId, visitId, itemCode, pName);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 重点检查报告左侧id列表
     */
    public void getChartData_List_Exam() {
        String itemCode = getParameter("code");
//        String pCode = getParameter("pcode");
        List<Map<String, String>> list = specialtyViewNewService.getZdjcCardIdList(patientId, visitId, itemCode);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 重点检验列表详细数据
     */
    public void getDetailData_Lab() {
        String itemCode = getParameter("code");
        String rowKey = getParameter("id");
        Map<String, Object> map = specialtyViewNewService.getZdjyCardListDetailData(patientId, visitId, rowKey, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 重点检查列表详细数据
     */
    public void getDetailData_Exam() {
        String subItemCode = getParameter("code");
        String rowKey = getParameter("id");
        Map<String, Object> map = specialtyViewNewService.getZdjcCardListDetailData(patientId, visitId, rowKey, subItemCode);
        renderJson(JsonUtil.getJSONString(map));

    }

    /**
     * 生命体征末次数据
     */
    public void getLastData_Health() {
        String itemCode = getParameter("subItemCode");
        String classCode = getParameter("classCode");
        Map<String, Object> map = specialtyViewNewService.getHeathLastData(patientId, visitId, classCode, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 重点检验结果末次数据
     */
    public void getLastData_LabDetail() {
        String itemCode = getParameter("subItemCode");
        String classCode = getParameter("classCode");
        Map<String, Object> map = specialtyViewNewService.getImportmentLabResLastData(patientId, visitId, classCode, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 护理饮食末次数据
     */
    public void getLastData_NurseAndEat() {
        String classCode = getParameter("classCode");
        String itemCode = getParameter("subItemCode");
        Map<String, Object> map = specialtyViewNewService.getNurseAndEatLastData(patientId, visitId, classCode, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 重点检查报告末次数据
     */
    public void getLastData_Exam() {
        String classCode = getParameter("classCode");
        String itemCode = getParameter("subItemCode");
        Map<String, Object> map = specialtyViewNewService.getExamLastData(patientId, visitId, classCode, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 重点用药末次数据
     */
    public void getLastData_Drag() {
        String classCode = getParameter("classCode");
        String itemCode = getParameter("subItemCode");
        Map<String, Object> map = specialtyViewNewService.getDragLastData(patientId, visitId, classCode, itemCode);
        renderJson(JsonUtil.getJSONString(map));
    }


    /**
     * 重点检验末次数据
     */
    public void getLastData_Lab() {
        String classCode = getParameter("classCode");
        String className = getParameter("pname");
        Map<String, Object> map = specialtyViewNewService.getLabLastData(patientId, visitId, classCode, className);
        renderJson(JsonUtil.getJSONString(map));
    }
}
