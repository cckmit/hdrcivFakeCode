package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.SpecialtyViewNewService;
import com.goodwill.hdr.civ.web.service.SpecialtyViewTimeAxisService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author guozhenlei
 * @Description 类描述：专科视图时间轴action
 * @modify 修改记录：
 */
public class SpecialtyViewTimeAxisAction extends CIVAction {
    @Autowired
    private SpecialtyViewTimeAxisService specialtyViewTimeAxisService;
    @Autowired
    private SpecialtyViewNewService specialtyViewNewService;

    /**
     * 获取诊断信息
     */
    public void getMainDaigList() {
        String visitType = getParameter("visitType");
        List<Map<String, String>> daigList = specialtyViewTimeAxisService.getMainDaig(patientId, visitType);
        renderJson(JsonUtil.getJSONString(daigList));
    }

    /**
     * \
     * 获取时间跨度集合
     */
    public void getTimeList() {
        Set<String> dateSet = specialtyViewTimeAxisService.getTimeList(patientId);
        List<Object> list = new ArrayList<Object>();
        list.add("1");
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 获取主诊断时间分布
     */
    public void getMainDaigTimeList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getMainDaigTimeList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 获取重点药品数据
     */
    public void getMainDrugList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getMainDrugTimeList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));

    }

    /**
     * 获取重点检查
     */
    public void getMainExamList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getMainExamList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 专科视图时间轴手术
     */
    public void getOperationList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getOperationList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));

    }


    /**
     * 获取重点检验报告数据
     */
    public void getMainLabList(){
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getMainLabTimeList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));

    }

    /**
     * 获取重点检验数据
     */
    public void getMainLabDetailList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getMainLabDetailTimeList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));

    }
    /**
     * 获取手术数据
     */
    public void getOperationDetailList() {
        String mainDaigName = getParameter("mainDaigName");
        String mainDaigCode = getParameter("mainDaigCode");
        String ids = getParameter("pvid");
        Map<String, Object> list = specialtyViewTimeAxisService.getOperationList(patientId, mainDaigName, mainDaigCode, visitType,ids);
        renderJson(JsonUtil.getJSONString(list));

    }

}
