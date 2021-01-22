package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.NursingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NursingAction extends CIVAction {

    private static final long serialVersionUID = -5781201065808395022L;

    @Autowired
    private NursingService nursingService;

    /**
     * 已弃用
     *
     * @Description url
     */
    public void getNursingUrl() {
        Map<String, String> rs = new HashMap<String, String>();
        rs = nursingService.getNursingUrl(patientId, visitId);
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * @Description 住院的就诊列表
     */
    public void getAllINVisits() {
        String year = getParameter("year");
        List<Map<String, String>> rs = new ArrayList<Map<String, String>>();
        rs = nursingService.getAllINVisits(patientId, visitType, year);
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * 护理记录调用第三方的url 护理类型配置
     */
    public void getNuringUrlTypes() {
        Map<String, Object> rs = Config.getCIV_NURSE_URL_TYPES();
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * 新增护理单-查看护理单类型下拉框
     */
    public void getNurseType() {
        String outPatientId = getParameter("outPatientId");
        List<Map<String, String>> rs = nursingService.getNurseTypes(patientId, visitId, visitType, outPatientId);
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * 新增护理单-护理单列表
     * @param patientId 患者id
     * @param visitId 就诊次数
     * @param visitType 就诊类型
     * @param outPatientId 门诊患者id
     * @param nurseType 护理类型
     * @param pageNo 当前页码
     * @param pageSize 每页大小
     */
    public void getNurseList(String patientId, String visitId,String visitType,String outPatientId, String nurseType, int pageNo, int pageSize) {
    /*调用逻辑包中 getNurseList 方法，将浏览器参数传给逻辑层处理，得到数据分页返回*/
         nursingService.getNurseList(patientId, visitId, visitType, outPatientId, nurseType, pageNo, pageSize);

    }

    /**
     * 获取护理单表头
     */
    public void getNurseTableHead() {
        List<Map<String, String>> rs = nursingService.getNurseTableHead();
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * 查询护理单详情
     */
    public void getNurseDetail() {
        String date = getParameter("date");
        String outPatientId = getParameter("outPatientId");
        Page<Map<String, String>> rs = nursingService.getNurseDetail(patientId, outPatientId, date, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(rs));
    }
}
