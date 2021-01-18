package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.CommonURLService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author guozhenlei
 * @Description 类描述：
 * @Date 2019-12-09 15:03
 * @modify 修改记录：
 */
public class URLCommonAction extends CIVAction {
    @Autowired
    private CommonURLService commonURLService;

    /**
     * @Description 血透url
     */
    public void getDialysisUrl() {
        Map<String, String> rs = new HashMap<String, String>();
        String sysCode = getParameter("sysCode");
        rs = commonURLService.getCommonUrl(patientId, visitId, "HD", new HashMap<String, String>());
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * @Description 获取通用url
     */
    public void getCommonUrl() {
        Map<String, String> rs = new HashMap<String, String>();
        String sysCode = getParameter("sysCode");
        rs = commonURLService.getCommonUrl(patientId, visitId, sysCode, new HashMap<String, String>());
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * @Description 获取web电子病历url
     */
    public void getWebEmrUrl() {
        Map<String, String> rs = new HashMap<String, String>();
        String sysCode = getParameter("sysCode");//WEBEMR
//        String visitNo = getParameter("visit_no");//就诊号
//        String visitTypeCode = getParameter("visit_type_code");
        Map<String,String> map = new HashMap<String, String>();
//        map.put("hisInpatientID",visitNo);
//        if("01".equals(visitTypeCode)){//门诊
//            map.put("Out","MQ==");
//        }else{
//            map.put("Out","MA==");
//        }
        rs = commonURLService.getCommonUrl(patientId, visitId, sysCode, map);
        renderJson(JsonUtil.getJSONString(rs));
    }


    /**
     * @Description 获取新版护理url
     */
    public void getOperUrl() {
        Map<String, String> rs = new HashMap<String, String>();
        String field = Config.getCIV_NURSE_URL_FIELD();
        String viewType = getParameter(field);//就诊号
        Map<String,String> map = new HashMap<String, String>();
        if(StringUtils.isNotBlank(viewType)) {
            map.put(field, viewType);
        }
        String visitType = getParameter("visitType");
        rs = commonURLService.getCommonNurseUrl(patientId, visitId, visitType, map);
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * 第三方调用通过brid查询pid,vid
     */
    public void getPatientIdByBrid(){
        String brid = getParameter("brid");
        String useType = getParameter("useType");
        Map<String, String> result = new HashMap<String, String>();
        if("brid".equalsIgnoreCase(useType)) {
            result = commonURLService.getPatientIdByBrid(brid);
        }
        renderJson(JsonUtil.getJSONString(result));
    }
}
