package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.CommonURLService;
import com.goodwill.hdr.civ.web.service.MedicalRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhaowenkai
 * @Description 类描述：病历文书Action
 * @Date 2018年6月19日
 * @modify 修改记录：
 */
public class MedicalRecordAction extends CIVAction {

    private static final long serialVersionUID = 1L;
    @Autowired
    private MedicalRecordService medicalRecordService;
    @Autowired
    private CommonURLService commonURLService;

    /**
     * @Description 某次就诊的病历文书
     */
    public void getPatientVisitMrs() {
        String mrCode = getParameter("recordType");
        String orderDir = getParameter("orderBy");
        int pno = StringUtils.isBlank(getParameter("pno")) ? 0 : Integer.parseInt(getParameter("pno"));
        //参数  患者编号 就诊次 就诊类型
        Page<Map<String, String>> result = medicalRecordService.getMedicalRecordList(patientId, visitId, visitType,
                "FIRST_MR_SIGN_DATE_TIME", orderDir, pno, pageSize,mrCode);
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 统计某次就诊的病历类型
     */
    public void getCVMrTypes() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(patientId))
            result = medicalRecordService.getCVMrTypes(patientId, visitId);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 根据条件筛选某次就诊病历文书
     */
    public void getCVMrList() {
        Page<Map<String, String>> result = new Page<Map<String, String>>();
        String mrCode = getParameter("type");
        if (StringUtils.isNotBlank(patientId))
            result = medicalRecordService.getCVMrList(patientId, visitId, mrCode, pageNo,
                    pageSize);
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 某份病历文书详情
     */
    public void getMrDetails() {
        //文书主键
        String fileNo = getParameter("fileNo");
        String mrClassCode = getParameter("mrClassCode");
        Map<String, String> result = medicalRecordService.getMedicalRecordDetails(fileNo, patientId, visitId, mrClassCode,"RM");
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 获得所有的病例文书
     */
    public void getMedicalRecords() {
        String outPatientId = getParameter("outPatientId");
        String visitType = getParameter("visitType");
        String year = getParameter("year");
        String key = getParameter("key");
        String type = getParameter("type");
        String click_Type = getParameter("click_type");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        result = medicalRecordService.getMedicalRecords(patientId, visitType, outPatientId, year, key, type, click_Type);
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 获得所有的病例文书类型
     */
    public void getMedicalTypes() {
        String outPatientId = getParameter("outPatientId");
        String visitType = getParameter("visitType");
        List<Map<String, String>> result = medicalRecordService.getMedicalTypes(patientId, outPatientId, visitType);
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }

    /**
     * @Description 病例章节显示
     */
    public void getEmrDgHtmlText() {
        String fileNo = getParameter("fileNo");
        String fileUniqueId = getParameter("fileUniqueId");
        String mrClassCode = getParameter("mrClassCode");
        Map<String, String> result = medicalRecordService.getDgHtmlText(patientId, visitId,mrClassCode, fileNo);
        //响应
        renderJson(JsonUtil.getJSONString(result));
    }


    /**
     * @Description
     * 获取病历文书类型配置列表
     */
    public void  getEmrTypes(){
        List<Map<String, String>> emrTypes = medicalRecordService.getEmrTypes();
        renderJson(JsonUtil.getJSONString(emrTypes));
    }
}
